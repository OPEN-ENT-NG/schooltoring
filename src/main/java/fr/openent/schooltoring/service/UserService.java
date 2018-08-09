package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface UserService {

    /**
     * Return user id, username, user class names and user avatar
     *
     * @param users   Users to retrieve
     * @param handler Function handler returning data
     */
    void getUsers(JsonArray users, Handler<Either<String, JsonArray>> handler);
}
