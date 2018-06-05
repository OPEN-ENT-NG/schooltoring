package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.definition.Feature;
import fr.openent.schooltoring.service.ProfileService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultProfileService implements ProfileService {

    Logger LOGGER = LoggerFactory.getLogger(DefaultProfileService.class);

    @Override
    public void get(String userId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT student.id as user_id, monday, tuesday, wednesday, thursday, friday, " +
                "saturday, sunday, feature.id as feature_id, state, subject_id " +
                "FROM " + Schooltoring.dbSchema + ".student " +
                "LEFT OUTER JOIN " + Schooltoring.dbSchema + ".feature ON (student.id = student_id) " +
                "WHERE student.id = ?;";

        Sql.getInstance().prepared(query, new JsonArray().add(userId), SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray body = event.right().getValue();
                if (body.size() == 0) {
                    handler.handle(new Either.Right<>(new JsonObject()));
                    return;
                }
                JsonArray weaknesses = new JsonArray();
                JsonArray strengths = new JsonArray();
                JsonObject result = body.getJsonObject(0);
                JsonObject value;

                if (result.getLong("feature_id") != null) {
                    for (int i = 0; i < body.size(); i++) {
                        value = body.getJsonObject(i);
                        if (Feature.STRENGTH.toString().equals(value.getString("state"))) {
                            strengths.add(formatFeature(value.getLong("feature_id"), value.getString("subject_id")));
                        } else {
                            weaknesses.add(formatFeature(value.getLong("feature_id"), value.getString("subject_id")));
                        }
                    }
                }

                JsonObject avaibilities = new JsonObject()
                        .put("monday", result.getBoolean("monday"))
                        .put("tuesday", result.getBoolean("tuesday"))
                        .put("wednesday", result.getBoolean("wednesday"))
                        .put("thursday", result.getBoolean("thursday"))
                        .put("friday", result.getBoolean("friday"))
                        .put("saturday", result.getBoolean("saturday"))
                        .put("sunday", result.getBoolean("sunday"));
                result.put("availabilities", avaibilities);
                result.put("weaknesses", weaknesses)
                        .put("strengths", strengths);
                result = clearResult(result);
                handler.handle(new Either.Right<>(result));
            } else {
                LOGGER.error("An error occurred when fetching profile for user " + userId);
                handler.handle(new Either.Left<>("An error occurred when fetching profile"));
            }
        }));
    }

    /**
     * Format feature
     * @param featureId Feature id
     * @param subjectId Subject id
     * @return
     */
    private JsonObject formatFeature (Long featureId, String subjectId) {
        return new JsonObject()
                .put("id", featureId)
                .put("subject_id", subjectId);
    }

    private JsonObject clearResult (JsonObject result) {
        result.remove("feature_id");
        result.remove("state");
        result.remove("monday");
        result.remove("tuesday");
        result.remove("wednesday");
        result.remove("thursday");
        result.remove("friday");
        result.remove("saturday");
        result.remove("sunday");

        return result;
    }
}
