package fr.openent.schooltoring.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface MessageService {

    /**
     * Add message to request conversation
     *
     * @param request Http Request. Need by i18n API.
     * @param requestId Request id
     * @param owner     User sending message
     * @param text      Text message
     * @param handler   Function handler returning data
     */
    void addMessage(HttpServerRequest request, Integer requestId, String owner, String text, Handler<Either<String, JsonObject>> handler);
}
