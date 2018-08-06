package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface MatchService {

    /**
     * Return an array containing matched users based on state and page parameters.
     *
     * @param userId      User id
     * @param structureId user structure id
     * @param state       State request. Should be STRENGTH or WEAKNESS
     * @param page        Page number
     * @param handler     Function handler returning data
     */
    void get(String userId, String structureId, String state, Integer page, Handler<Either<String, JsonArray>> handler);
}
