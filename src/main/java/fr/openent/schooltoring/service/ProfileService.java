package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface ProfileService {
    /**
     * Get user profile
     *
     * @param userId  User identifier
     * @param handler Function handler returning data
     */
    void get(String userId, Handler<Either<String, JsonObject>> handler);

    /**
     * Set user profile
     *
     * @param userId  User identifier
     * @param profile Profile object
     * @param handler Function handler returning data
     * @Param structureId User structure
     */
    void set(String userId, String structureId, JsonObject profile, Handler<Either<String, JsonObject>> handler);

    /**
     * Update user profile
     *
     * @param userId  User identifier
     * @param profile Profile object
     * @param handler Function handler returning data
     */
    void update(String userId, JsonObject profile, Handler<Either<String, JsonObject>> handler);
}
