package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.definition.Feature;
import fr.openent.schooltoring.service.MatchService;
import fr.openent.schooltoring.service.ProfileService;
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

public class DefaultMatchService implements MatchService {

    private static Integer PAGE_SIZE = 10;
    private final ProfileService profileService = new DefaultProfileService();
    private final SubjectService subjectService = new DefaultSubjectService();
    private final UserService userService = new DefaultUserService();

    Logger LOGGER = LoggerFactory.getLogger(DefaultProfileService.class);

    @Override
    public void get(String userId, String structureId, String state, Integer page, Handler<Either<String, JsonArray>> handler) {
        profileService.get(userId, event -> {
            if (event.isRight()) {
                JsonObject availabilites = event.right().getValue().getJsonObject("availabilities");
                JsonArray params = new JsonArray()
                        .add(userId)
                        .add(Feature.STRENGTH.toString().equals(state) ? Feature.WEAKNESS : Feature.STRENGTH)
                        .add(state)
                        .add(structureId)
                        .add(userId)
                        .add(state)
                        .add(userId);
                String query = "SELECT st.id, count(*) as matches, row_to_json(st.*) as availabilities, array_to_json(array_agg(ft.subject_id)) as features, concat_ws('$', count(*), st.id) as orderedMatches " +
                        "FROM  " + Schooltoring.dbSchema + ".student st INNER JOIN " +
                        Schooltoring.dbSchema + ".feature ft on (st.id = ft.student_id) " +
                        "WHERE EXISTS (SELECT * " +
                        "FROM  " + Schooltoring.dbSchema + ".student INNER JOIN  " +
                        Schooltoring.dbSchema + ".feature on (student.id = feature.student_id) " +
                        "WHERE student.id = ? " +
                        "AND subject_id = ft.subject_id " +
                        "AND state = ?) " +
                        "AND ft.state = ? " +
                        "AND st.structure_id = ? " +
                        "AND st.id NOT IN (SELECT student_id " +
                        "FROM " + Schooltoring.dbSchema + ".request " +
                        "WHERE owner = ? " +
                        "AND state = ?)" +
                        "AND st.id != ? " +
                        "AND (";
                for (String day : availabilites.fieldNames()) {
                    query += "st." + day + " = ? OR ";
                    params.add(availabilites.getBoolean(day));
                }
                query = query.substring(0, query.length() - 3);
                query += ") " +
                        "GROUP BY st.id " +
                        "ORDER BY orderedMatches DESC " +
                        "LIMIT ? " +
                        "OFFSET ?";

                params.add(PAGE_SIZE)
                        .add(page * PAGE_SIZE);

                Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(matchEvent -> {
                    if (matchEvent.isRight()) {
                        getUsersMatched(matchEvent.right().getValue(), handler);
                    } else {
                        String errorMessage = "[DefaultMatchService@get] An error occurred when fetching matches for user " + userId;
                        LOGGER.error(errorMessage);
                        handler.handle(new Either.Left<>(errorMessage));
                    }
                }));
            } else {
                String errorMessage = "[DefaultMatchService@get] An error occurred when fetching availabilities for user " + userId;
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        });
    }

    /**
     * Get users matched. Fetch user information.
     *
     * @param users   users matched
     * @param handler Function handler returning data
     */
    private void getUsersMatched(JsonArray users, Handler<Either<String, JsonArray>> handler) {
        JsonArray usersId = Utils.extractStringValues(users, "id");
        userService.getUsers(usersId, event -> {
            if (event.isRight()) {
                getSubjectsMatched(users, mapUsers(event.right().getValue()), handler);
            } else {
                String errorMessage = "[DefaultMatchService@getUsersMatched] An error occurred when matching users";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        });
    }

    /**
     * Get subjects matched.
     *
     * @param users       users matched
     * @param mappedUsers user mapped
     * @param handler     Function handler returning data
     */
    private void getSubjectsMatched(JsonArray users, JsonObject mappedUsers, Handler<Either<String, JsonArray>> handler) {
        JsonArray subjectsIds = Utils.extractFeaturesId(users, null);

        subjectService.getSubjectsById(subjectsIds, event -> {
            if (event.isRight()) {
                JsonObject tmpUser, mappedFeatures = mapSubjects(event.right().getValue()), tmpAvailabilites;

                for (int i = 0; i < users.size(); i++) {
                    tmpUser = users.getJsonObject(i);
                    tmpUser.put("userinfo", mappedUsers.getJsonObject(tmpUser.getString("id")));
                    tmpAvailabilites = new JsonObject(tmpUser.getString("availabilities"));
                    tmpAvailabilites.remove("id");
                    tmpAvailabilites.remove("structure_id");
                    tmpUser.put("availabilities", tmpAvailabilites);
                    tmpUser.put("features", getFeatures(new JsonArray(tmpUser.getString("features")), mappedFeatures));
                    tmpUser.remove("orderedmatches");
                }

                handler.handle(new Either.Right<>(users));
            } else {
                String errorMessage = "[DefaultMatchService@getSubjectsMatched] An error occurred when matching subjects";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        });
    }

    /**
     * Get fromatted features
     *
     * @param userFeatures   user features
     * @param mappedSubjects subjects mapped with subject id as key and subject as value
     * @return Returns a JsonArray containing user features
     */
    private JsonArray getFeatures(JsonArray userFeatures, JsonObject mappedSubjects) {
        JsonArray features = new JsonArray();
        for (int i = 0; i < userFeatures.size(); i++) {
            features.add(mappedSubjects.getJsonObject(userFeatures.getString(i)));
        }

        return features;
    }

    /**
     * Map users based on users array
     *
     * @param users Users
     * @return A JsonObject containing user id as key and user as value
     */
    private JsonObject mapUsers(JsonArray users) {
        JsonObject map = new JsonObject(), user;
        for (int i = 0; i < users.size(); i++) {
            user = users.getJsonObject(i);
            map.put(user.getString("id"), user);
        }

        return map;
    }

    /**
     * Map features based on features array
     *
     * @param subjects subject list
     * @return A JsonObject containing feature id as key and feature as value
     */
    private JsonObject mapSubjects(JsonArray subjects) {
        JsonObject map = new JsonObject(), feature;
        for (int i = 0; i < subjects.size(); i++) {
            feature = subjects.getJsonObject(i);
            map.put(feature.getString("subjectId"), feature);
        }

        return map;
    }
}
