package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface RequestService {

    /**
     * Create request
     *
     * @param userId  User launching creation.
     * @param body    Request body. Contains the other student id and the request state.
     *                Request state should be "STRENGTH" or "WEAKNESS"
     * @param handler Function handler returning data
     */
    void create(String userId, JsonObject body, Handler<Either<String, JsonObject>> handler);

    /**
     * Update request status
     *
     * @param requestId Request id to update
     * @param status    New status
     * @param handler   Function handler returning data
     */
    void updateStatus(Integer requestId, String status, Handler<Either<String, JsonObject>> handler);

    /**
     * Get user requests
     *
     * @param userId  Expected user requests
     * @param state   Expected features state
     * @param status  Expected status requests
     * @param handler Function handler returning data
     */
    void getRequests(String userId, String state, List<String> status, Handler<Either<String, JsonArray>> handler);
}
