package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ConversationService {

    /**
     * Create a new conversation between 2 users
     *
     * @param user1   First conversation user
     * @param user2   Second conversation user
     * @param handler Function handler returning data
     */
    void create(String user1, String user2, Handler<Either<String, JsonObject>> handler);

    /**
     * Fetch user conversations
     *
     * @param userId  User id
     * @param handler Function handler returning data
     */
    void getConversations(String userId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get request messages
     *
     * @param conversationId Request identifier
     * @param page      Page number
     * @param handler   Function handler returning data
     */
    void getMessages(Integer conversationId, Integer page, Handler<Either<String, JsonArray>> handler);

    /**
     * Check if conversation already exists
     *
     * @param user1   First user identifier
     * @param user2   Second user identifier
     * @param handler Function handler returning data
     */
    void checkIfExists(String user1, String user2, Handler<Either<String, JsonObject>> handler);
}
