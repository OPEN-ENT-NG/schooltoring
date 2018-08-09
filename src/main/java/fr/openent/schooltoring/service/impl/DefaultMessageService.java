package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.service.MessageService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultMessageService implements MessageService {
    @Override
    public void addMessage(Integer requestId, String owner, String text, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO schooltoring.message(owner, request_id, text) " +
                "VALUES (?, ?, ?);";
        JsonArray params = new JsonArray()
                .add(owner)
                .add(requestId)
                .add(text);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }
}
