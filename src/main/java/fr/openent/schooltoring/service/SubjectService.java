package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface SubjectService {

    /**
     * Get subjects by id
     *
     * @param subjects subject ids list
     * @param handler  Function handler returning data
     */
    void getSubjectsById(JsonArray subjects, Handler<Either<String, JsonArray>> handler);
}
