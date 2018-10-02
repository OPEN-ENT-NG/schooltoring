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

public class DefaultConversationService implements ConversationService {

    private static Integer CONVERSATION_PAGE_SIZE = 20;
    private final UserService userService = new DefaultUserService();
    Logger LOGGER = LoggerFactory.getLogger(DefaultConversationService.class);

    @Override
    public void create(String user1, String user2, Handler<Either<String, JsonObject>> handler) {
        String getConversationIdQuery = "SELECT nextval('" + Schooltoring.dbSchema + ".conversation_id_seq') as id";
        Sql.getInstance().raw(getConversationIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                Integer conversationId = event.right().getValue().getInteger("id");
                JsonArray statements = new JsonArray();
                JsonObject conversationCreationStatement = new JsonObject()
                        .put("statement", "INSERT INTO " + Schooltoring.dbSchema + ".conversation(id) VALUES (?);")
                        .put("values", new JsonArray().add(conversationId))
                        .put("action", "prepared");
                statements.add(conversationCreationStatement)
                        .add(getUserConversationStatement(conversationId, user1))
                        .add(getUserConversationStatement(conversationId, user2));

                Sql.getInstance().transaction(statements, event1 -> {
                    JsonObject result = event1.body();
                    if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
                        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok").put("id", conversationId)));
                    } else {
                        String errorMessage = "[DefaultConversationService@create] Unable to create conversation";
                        LOGGER.error(errorMessage);
                        handler.handle(new Either.Left<>(errorMessage));
                    }
                });
            } else {
                String errorMessage = "[DefaultConversationService@create] Unable to fetch next conversation id";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    /**
     * Get insert user conversation statement
     *
     * @param conversationId Conversation identifier
     * @param userId         User identifier
     * @return Object containing transaction statement
     */
    private JsonObject getUserConversationStatement(Integer conversationId, String userId) {
        String query = "INSERT INTO " + Schooltoring.dbSchema + ".conversation_users(id, conversation_id) VALUES (?, ?);";
        JsonArray params = new JsonArray()
                .add(userId)
                .add(conversationId);
        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void getConversations(String userId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT c.id as id, message.text as message, " +
                "CASE WHEN message.date IS NULL THEN c.created ELSE message.date END as date, " +
                "(SELECT id FROM " + Schooltoring.dbSchema + ".conversation_users WHERE conversation_id = c.id AND id <> ?) as student_id " +
                "FROM " + Schooltoring.dbSchema + ".conversation c " +
                "INNER JOIN " + Schooltoring.dbSchema + ".conversation_users ON (c.id = conversation_users.conversation_id) " +
                "LEFT JOIN " + Schooltoring.dbSchema + ".message ON (c.id = message.conversation_id) " +
                "AND message.id = (SELECT id FROM " + Schooltoring.dbSchema + ".message WHERE c.id = message.conversation_id ORDER BY date DESC LIMIT 1) " +
                "WHERE conversation_users.id = ? " +
                "ORDER BY date DESC";

        params.add(userId)
                .add(userId);

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
    public void getMessages(Integer conversationId, String lastMessage, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT owner, date, text FROM " + Schooltoring.dbSchema + ".message " +
                "WHERE conversation_id = ? AND date < ? ORDER BY date DESC LIMIT " + CONVERSATION_PAGE_SIZE;
        JsonArray params = new JsonArray().add(conversationId).add(lastMessage);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void checkIfExists(String user1, String user2, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT conversation.id " +
                "FROM " + Schooltoring.dbSchema + ".conversation INNER JOIN " + Schooltoring.dbSchema +
                ".conversation_users ON (conversation.id = conversation_users.conversation_id) " +
                "WHERE conversation_users.id = ? " +
                "AND conversation_id IN (SELECT conversation_id " +
                "FROM " + Schooltoring.dbSchema + ".conversation_users " +
                "WHERE id = ?)";
        JsonArray params = new JsonArray()
                .add(user1)
                .add(user2);

        Sql.getInstance().prepared(query, params, message -> {
            Long count = SqlResult.countResult(message);
            JsonObject res = new JsonObject()
                    .put("exists", count != null && count > 0)
                    .put("id", count != null && count > 0 ? message.body().getJsonArray("results").getJsonArray(0).getInteger(0) : 0);
            handler.handle(new Either.Right<>(res));
        });
    }

    @Override
    public void getConversationIds(String userId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT conversation_users.id as user, conversation_users.conversation_id " +
                "FROM " + Schooltoring.dbSchema + ".conversation_users " +
                "WHERE conversation_users.conversation_id IN ( " +
                "SELECT conversation.id " +
                "FROM " + Schooltoring.dbSchema + ".conversation " +
                "INNER JOIN " + Schooltoring.dbSchema + ".conversation_users ON (conversation.id = conversation_users.conversation_id) " +
                "WHERE conversation_users.id = ? " +
                ") " +
                "AND conversation_users.id != ?";

        JsonArray params = new JsonArray()
                .add(userId)
                .add(userId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
