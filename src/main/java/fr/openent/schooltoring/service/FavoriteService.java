package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface FavoriteService {

    /**
     * Get owner favorite list
     *
     * @param owner   Owner id
     * @param handler Function handler returning data
     */
    void get(String owner, Handler<Either<String, JsonArray>> handler);

    /**
     * Add a user to owner favorite list
     *
     * @param userId  User to add to favorite list
     * @param owner   Favorite owner list
     * @param handler Function handler returning data
     */
    void add(String userId, String owner, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete user from owner favorite list
     *
     * @param userId  User to delete
     * @param owner   Favorite owner
     * @param handler Function handler returning data
     */
    void delete(String userId, String owner, Handler<Either<String, JsonObject>> handler);
}
