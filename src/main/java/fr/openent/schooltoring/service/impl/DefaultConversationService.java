package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.service.ConversationService;
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

public class DefaultConversationService implements ConversationService {

    private static Integer CONVERSATION_PAGE_SIZE = 20;
    private final UserService userService = new DefaultUserService();
    Logger LOGGER = LoggerFactory.getLogger(DefaultConversationService.class);

    @Override
    public void getConversations(String userId, String state, List<String> status, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray()
                .add(userId)
                .add(userId)
                .add(userId);

        String statusFilter = "";
        for (int i = 0; i < status.size(); i++) {
            statusFilter += "rq.status = ? OR ";
            params.add(status.get(i));
        }

        String stateFilter = "";
        if (state != null) {
            stateFilter = "AND rq.state = ? ";
            params.add(state);
        }

        statusFilter = statusFilter.substring(0, statusFilter.length() - 3);
        String query = "SELECT rq.id, status, state, msg.text as message, CASE WHEN msg.date IS NULL THEN rq.created ELSE msg.date END as date," +
                "CASE WHEN rq.owner = ? THEN rq.student_id ELSE rq.owner END as student_id " +
                "FROM " + Schooltoring.dbSchema + ".request rq  LEFT JOIN " + Schooltoring.dbSchema + ".message msg ON rq.id = msg.request_id " +
                "AND msg.id = ( SELECT id FROM " + Schooltoring.dbSchema + ".message WHERE request_id = rq.id  ORDER BY date DESC LIMIT 1) " +
                "WHERE (rq.owner = ? OR rq.student_id = ?) " +
                "AND (" + statusFilter + ") " + stateFilter + "ORDER BY date DESC";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray conversations = event.right().getValue();
                JsonArray usersIds = Utils.extractStringValues(conversations, "student_id");
                userService.getUsers(usersIds, usersEvent -> {
                    if (usersEvent.isRight()) {
                        JsonArray users = usersEvent.right().getValue();
                        handler.handle(new Either.Right<>(formatConversations(conversations, users)));
                    } else {
                        String errorMessage = "[DefaultConversationService@getConversations] An error occurred when collecting users";
                        LOGGER.error(errorMessage);
                        handler.handle(new Either.Left<>(errorMessage));
                    }
                });
            } else {
                String errorMessage = "[DefaultConversationService@getConversations] An error occurred when collecting conversations";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    /**
     * Map conversations with users
     *
     * @param conversations Conversations to format
     * @param users         Conversations users
     * @return An array containing formatted conversations
     */
    private JsonArray formatConversations(JsonArray conversations, JsonArray users) {
        JsonObject userMap = Utils.mapObjectsWithStringKeys(users, null);
        JsonObject tmpConversation;

        for (int i = 0; i < conversations.size(); i++) {
            tmpConversation = conversations.getJsonObject(i);
            tmpConversation.put("userinfo", userMap.getJsonObject(tmpConversation.getString("student_id")));
            tmpConversation.remove("student_id");
        }

        return conversations;
    }


    @Override
    public void getMessages(Integer requestId, Integer page, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT owner, date, text FROM " + Schooltoring.dbSchema + ".message " +
                "WHERE request_id = ? ORDER BY date DESC LIMIT " + CONVERSATION_PAGE_SIZE + " OFFSET ?";
        JsonArray params = new JsonArray().add(requestId).add(page * CONVERSATION_PAGE_SIZE);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
