package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.definition.RequestStatus;
import fr.openent.schooltoring.service.ConversationService;
import fr.openent.schooltoring.service.RequestService;
import fr.openent.schooltoring.service.SubjectService;
import fr.openent.schooltoring.service.UserService;
import fr.openent.schooltoring.utils.Utils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultRequestService implements RequestService {

    private final UserService userService = new DefaultUserService();
    private final SubjectService subjectService = new DefaultSubjectService();
    private final ConversationService conversationService = new DefaultConversationService();
    Logger LOGGER = LoggerFactory.getLogger(DefaultRequestService.class);

    @Override
    public void create(String userId, JsonObject body, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Schooltoring.dbSchema + ".request(student_id, owner, state, status) " +
                "VALUES (?, ?, ?, ?) RETURNING id;";
        params.add(body.getString("student_id"))
                .add(userId)
                .add(body.getString("state"))
                .add(RequestStatus.WAITING);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void updateStatus(Integer requestId, String status, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Schooltoring.dbSchema + ".request SET status = ? WHERE id = ?";
        JsonArray params = new JsonArray().add(status).add(requestId);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(event -> {
            if (event.isRight()) {
                if (!RequestStatus.ACCEPTED.toString().equals(status)) {
                    handler.handle(new Either.Right<>(event.right().getValue()));
                    return;
                }

                String getRequestQuery = "SELECT owner, student_id FROM " + Schooltoring.dbSchema + ".request WHERE id = ?;";
                JsonArray getRequestQueryParams = new JsonArray().add(requestId);
                Sql.getInstance().prepared(getRequestQuery, getRequestQueryParams, SqlResult.validResultHandler(getUsersEvent -> {
                    if (getUsersEvent.isRight()) {
                        JsonObject body = getUsersEvent.right().getValue().getJsonObject(0);
                        String user1 = body.getString("owner"), user2 = body.getString("student_id");
                        userService.getUsers(new JsonArray().add(body.getString("owner")), result -> {
                            if (result.isRight()) {
                                conversationService.checkIfExists(user1, user2, response -> {
                                    if (response.isRight() && response.right().getValue().getBoolean("exists")) {
                                        JsonObject returnedValue = new JsonObject()
                                                .put("id", response.right().getValue().getInteger("id"))
                                                .put("student", result.right().getValue().getJsonObject(0));

                                        handler.handle(new Either.Right<>(returnedValue));
                                    } else {
                                        conversationService.create(user1, user2,
                                                event1 -> {
                                                    if (result.isRight()) {
                                                        JsonObject returnedValue = new JsonObject()
                                                                .put("id", event1.right().getValue().getInteger("id"))
                                                                .put("student", result.right().getValue().getJsonObject(0));

                                                        handler.handle(new Either.Right<>(returnedValue));
                                                    } else {
                                                        String errorMessage = "[DefaultRequestService@updateStatus] An error occurred when matching user";
                                                        LOGGER.error(errorMessage);
                                                        handler.handle(new Either.Left<>(errorMessage));
                                                    }
                                                });
                                    }
                                });
                            } else {
                                String errorMessage = "[DefaultRequestService@updateStatus] An error occurred when matching user";
                                LOGGER.error(errorMessage);
                                handler.handle(new Either.Left<>(errorMessage));
                            }
                        });
                    } else {
                        String errorMessage = "[DefaultRequestService@updateStatus] Unable to fetch request users";
                        LOGGER.error(errorMessage);
                        handler.handle(new Either.Left<>(errorMessage));
                    }
                }));
            } else {
                String errorMessage = "[DefaultRequestService@updateStatus] An error occurred updating request status";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    @Override
    public void getRequests(String userId, String state, List<String> status, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray()
                .add(userId);
        if (state != null) {
            params.add(state);
        }

        String statusFilter = "";
        for (int i = 0; i < status.size(); i++) {
            statusFilter += "rq.status = ? OR ";
            params.add(status.get(i));
        }

        statusFilter = statusFilter.substring(0, statusFilter.length() - 3);

        String query = "SELECT rq.id, rq.owner, array_to_json(array_agg(ft.subject_id)) as features, rq.status, rq.state " +
                "FROM " + Schooltoring.dbSchema + ".request rq " +
                "INNER JOIN " + Schooltoring.dbSchema + ".feature ft ON (ft.student_id = rq.owner) " +
                "WHERE rq.student_id = ? ";
        if (state != null) {
            query += "AND rq.state = ? ";
        }
        query += "AND EXISTS (SELECT * " +
                "FROM " + Schooltoring.dbSchema + ".feature " +
                "WHERE subject_id = ft.subject_id " +
                "AND state = rq.state " +
                "AND student_id = rq.student_id) " +
                "AND (" + statusFilter + ") " +
                "GROUP BY rq.owner, rq.id " +
                "ORDER BY rq.created ASC;";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray requests = event.right().getValue();
                JsonArray usersIds = Utils.extractStringValues(requests, "owner");
                userService.getUsers(usersIds, result -> {
                    if (result.isRight()) {
                        JsonArray users = result.right().getValue();
                        JsonArray subjects = Utils.extractFeaturesId(requests, null);
                        subjectService.getSubjectsById(subjects, subjectEvent -> {
                            if (subjectEvent.isRight()) {
                                handler.handle(new Either.Right<>(formatRequests(requests, users, subjectEvent.right().getValue())));
                            } else {
                                String errorMessage = "[DefaultRequestService@getRequests] An error occurred when collecting subjects";
                                LOGGER.error(errorMessage);
                                handler.handle(new Either.Left<>(errorMessage));
                            }
                        });
                    } else {
                        String errorMessage = "[DefaultRequestService@getRequests] An error occurred when collecting users";
                        LOGGER.error(errorMessage);
                        handler.handle(new Either.Left<>(errorMessage));
                    }
                });
            } else {
                String errorMessage = "[DefaultRequestService@getRequests] An error occurred when collecting requests";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    /**
     * Format requests to end Http request.
     *
     * @param requests Requests list
     * @param users    User list
     * @param subjects Subject list
     * @return Return array containing request formatted as getRequest render format
     */
    private JsonArray formatRequests(JsonArray requests, JsonArray users, JsonArray subjects) {
        JsonObject userMap = Utils.mapObjectsWithStringKeys(users, null),
                subjectMap = Utils.mapObjectsWithStringKeys(subjects, "subjectId"),
                o;
        JsonArray tmpFeatures, oFeatures;

        for (int i = 0; i < requests.size(); i++) {
            tmpFeatures = new JsonArray();
            o = requests.getJsonObject(i);
            o.put("userinfo", userMap.getJsonObject(o.getString("owner")));
            o.remove("owner");
            oFeatures = new JsonArray(o.getString("features"));
            for (int j = 0; j < oFeatures.size(); j++) {
                tmpFeatures.add(subjectMap.getJsonObject(oFeatures.getString(j)));
            }
            o.put("features", tmpFeatures);
        }

        return requests;
    }
}
