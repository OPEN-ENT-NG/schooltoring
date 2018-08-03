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
    private String ADDITION = "addition";
    private String DELETION = "deletion";

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
                String errorMessage = "[DefaultProfileService@get] An error occurred when fetching profile for user " + userId;
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    @Override
    public void set(String userId, String structureId, JsonObject profile, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray()
                .add(setStudentStatement(userId, structureId, profile.getJsonObject("availabilities")));

        JsonArray strengths = profile.getJsonArray("strengths");
        JsonArray weaknesses = profile.getJsonArray("weaknesses");

        for (int s = 0; s < strengths.size(); s++) {
            statements.add(setFeatureStatement(userId, strengths.getJsonObject(s), Feature.STRENGTH.toString()));
        }

        for (int w = 0; w < weaknesses.size(); w++) {
            statements.add(setFeatureStatement(userId, weaknesses.getJsonObject(w), Feature.WEAKNESS.toString()));
        }

        Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void update(String userId, JsonObject profile, Handler<Either<String, JsonObject>> handler) {
        this.get(userId, event -> {
            if (event.isRight()) {
                JsonArray arr;
                JsonObject userProfile = event.right().getValue();

                JsonArray statements = new JsonArray()
                        .add(getUpdateAvailabilitiesStatement(userId, profile.getJsonObject("availabilities")));

                JsonObject weaknesses = compareFeatures(userProfile.getJsonArray("weaknesses"), profile.getJsonArray("weaknesses"));
                JsonObject strengths = compareFeatures(userProfile.getJsonArray("strengths"), profile.getJsonArray("strengths"));

                arr = weaknesses.getJsonArray(ADDITION);
                for (int i = 0; i < arr.size(); i++) {
                    statements.add(setFeatureStatement(userId, arr.getJsonObject(i), Feature.WEAKNESS.toString()));
                }

                arr = weaknesses.getJsonArray(DELETION);
                for (int i = 0; i < arr.size(); i++) {
                    statements.add(deleteFeatureStatement(arr.getInteger(i), userId));
                }

                arr = strengths.getJsonArray(ADDITION);
                for (int i = 0; i < arr.size(); i++) {
                    statements.add(setFeatureStatement(userId, arr.getJsonObject(i), Feature.STRENGTH.toString()));
                }

                arr = strengths.getJsonArray(DELETION);
                for (int i = 0; i < arr.size(); i++) {
                    statements.add(deleteFeatureStatement(arr.getInteger(i), userId));
                }

                Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));

            } else {
                String errorMessage = "[DefaultProfileService@update] An error occurred while updating user profile for user " + userId;
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        });
    }

    private JsonObject getUpdateAvailabilitiesStatement(String userId, JsonObject availabilities) {
        String query = "UPDATE " + Schooltoring.dbSchema + ".student SET monday=?, tuesday=?, " +
                "wednesday=?, thursday=?, friday=?, saturday=?, sunday=? WHERE id=?";

        JsonArray params = new JsonArray()
                .add(availabilities.getBoolean("monday"))
                .add(availabilities.getBoolean("tuesday"))
                .add(availabilities.getBoolean("wednesday"))
                .add(availabilities.getBoolean("thursday"))
                .add(availabilities.getBoolean("friday"))
                .add(availabilities.getBoolean("saturday"))
                .add(availabilities.getBoolean("sunday"))
                .add(userId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Compare feature and sort them. It returns an object containing an array with added features and
     * an array with deleted features. For each feature:
     * - If the feature has no SQL identifier, the algorithm checks if the subject id appears in the current
     * subject array. If yes, the feature is skipped. Otherwise, it pushes to the new features array.
     * - If the feature has a SQL identifier, the function skipped it.
     *
     * @param current Current feature array
     * @param updated Updated feature array
     * @return
     */
    private JsonObject compareFeatures(JsonArray current, JsonArray updated) {
        JsonArray currentSqlIds = new JsonArray(),
                currentSubjectsIds = new JsonArray(),
                updatedSqlIds = new JsonArray(),
                updatedSubjectsIds = new JsonArray(),
                newFeatures = new JsonArray(),
                deletedFeatures = new JsonArray();
        JsonObject o;
        Integer id;
        String subjectId;

        for (int i = 0; i < current.size(); i++) {
            o = current.getJsonObject(i);
            currentSqlIds.add(o.getInteger("id"));
            currentSubjectsIds.add(o.getString("subject_id"));
        }

        for (int i = 0; i < updated.size(); i++) {
            o = updated.getJsonObject(i);
            if (o.containsKey("id")) {
                updatedSqlIds.add(o.getInteger("id"));
            }
            updatedSubjectsIds.add(o.getString("subject_id"));
        }

        for (int i = 0; i < updated.size(); i++) {
            o = updated.getJsonObject(i);
            subjectId = o.getString("subject_id");
            if (o.containsKey("id") || currentSubjectsIds.contains(subjectId)) {
                continue;
            }

            newFeatures.add(o);
        }

        for (int i = 0; i < currentSqlIds.size(); i++) {
            id = currentSqlIds.getInteger(i);
            if (!updatedSqlIds.contains(id)) {
                deletedFeatures.add(id);
            }
        }

        return new JsonObject()
                .put(DELETION, deletedFeatures)
                .put(ADDITION, newFeatures);
    }

    private JsonObject setFeatureStatement(String userId, JsonObject feature, String state) {
        JsonArray params = new JsonArray()
                .add(userId);
        String query = "INSERT INTO " + Schooltoring.dbSchema + ".feature(student_id, state, subject_id) " +
                "VALUES (?, ?, ?);";
        params.add(state)
                .add(feature.getString("subject_id"));

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject deleteFeatureStatement(Integer id, String userId) {
        String query = "DELETE FROM " + Schooltoring.dbSchema + ".feature  WHERE id = ? AND student_id = ?;";
        JsonArray params = new JsonArray()
                .add(id)
                .add(userId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject setStudentStatement(String userId, String structureId, JsonObject availabilities) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Schooltoring.dbSchema + ".student (id, structure_id, monday, tuesday, " +
                "wednesday, thursday, friday, saturday, sunday) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        params.add(userId)
                .add(structureId)
                .add(availabilities.getBoolean("monday"))
                .add(availabilities.getBoolean("tuesday"))
                .add(availabilities.getBoolean("wednesday"))
                .add(availabilities.getBoolean("thursday"))
                .add(availabilities.getBoolean("friday"))
                .add(availabilities.getBoolean("saturday"))
                .add(availabilities.getBoolean("sunday"));

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Format feature
     *
     * @param featureId Feature id
     * @param subjectId Subject id
     * @return
     */
    private JsonObject formatFeature(Long featureId, String subjectId) {
        return new JsonObject()
                .put("id", featureId)
                .put("subject_id", subjectId);
    }

    private JsonObject clearResult(JsonObject result) {
        result.remove("feature_id");
        result.remove("subject_id");
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
