package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface ConversationService {

    /**
     * Fetch user conversations
     *
     * @param userId  User id
     * @param state   Requests state to fetch. If null, default states are STRENGTH and WEAKNESS
     * @param status  Requests status to fetch
     * @param handler Function handler returning data
     */
    void getConversations(String userId, String state, List<String> status, Handler<Either<String, JsonArray>> handler);

    /**
     * Get request messages
     *
     * @param requestId Request identifier
     * @param page      Page number
     * @param handler   Function handler returning data
     */
    void getMessages(Integer requestId, Integer page, Handler<Either<String, JsonArray>> handler);
}
