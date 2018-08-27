package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.service.FavoriteService;
import fr.openent.schooltoring.service.UserService;
import fr.openent.schooltoring.utils.Utils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultFavoriteService implements FavoriteService {
    Logger LOGGER = LoggerFactory.getLogger(DefaultFavoriteService.class);
    UserService userService = new DefaultUserService();

    @Override
    public void get(String owner, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Schooltoring.dbSchema + ".favorite WHERE owner = ?;";
        JsonArray params = new JsonArray()
                .add(owner);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray favorites = event.right().getValue();
                JsonArray users = Utils.extractStringValues(favorites, "student_id");

                userService.getUsers(users, userEvent -> {
                    if (userEvent.isRight()) {
                        JsonArray result = new JsonArray();
                        JsonObject usersMapped = Utils.mapObjectsWithStringKeys(userEvent.right().getValue(), "id");
                        for (int i = 0; i < favorites.size(); i++) {
                            JsonObject user = usersMapped.getJsonObject(favorites.getJsonObject(i).getString("student_id"));
                            user.remove("id");
                            JsonObject favorite = new JsonObject()
                                    .put("id", favorites.getJsonObject(i).getString("student_id"))
                                    .put("userinfo", user);

                            result.add(favorite);
                        }

                        handler.handle(new Either.Right<>(result));
                    } else {
                        String errorMessage = "[DefaultFavoriteService@get] An error occurred when fetching users ";
                        LOGGER.error(errorMessage);
                        handler.handle(new Either.Left<>(errorMessage));
                    }
                });
            } else {
                String errorMessage = "[DefaultFavoriteService@get] An error occurred when fetching favorites for user " + owner;
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    @Override
    public void add(String userId, String owner, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Schooltoring.dbSchema + ".favorite(student_id, owner) VALUES (?, ?);";
        JsonArray params = new JsonArray()
                .add(userId)
                .add(owner);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(event -> {
            if (event.isRight()) {
                userService.getUsers(new JsonArray().add(userId), userEvent -> {
                    if (userEvent.isRight()) {
                        JsonObject userinfo = userEvent.right().getValue().getJsonObject(0);
                        JsonObject result = new JsonObject()
                                .put("id", userId)
                                .put("userinfo", userinfo);

                        handler.handle(new Either.Right<>(result));
                    } else {
                        String errorMessage = "[DefaultFavoriteService@add] An error occurred when fetching profile for user " + userId;
                        LOGGER.error(errorMessage);
                        handler.handle(new Either.Left<>(errorMessage));
                    }
                });
            } else {
                String errorMessage = "[DefaultFavoriteService@add] An error occurred when adding user " + userId + " in " + owner + " favorite list";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    @Override
    public void delete(String userId, String owner, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Schooltoring.dbSchema + ".favorite WHERE student_id = ? AND owner = ?;";
        JsonArray params = new JsonArray()
                .add(userId)
                .add(owner);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }
}
