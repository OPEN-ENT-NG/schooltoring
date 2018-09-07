package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.service.MessageService;
import fr.openent.schooltoring.service.UserService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.notification.NotificationUtils;
import org.entcore.common.notification.ws.OssFcm;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import static fr.wseduc.webutils.http.Renders.getHost;

public class DefaultMessageService implements MessageService {

    private final OssFcm ossFcm;
    private final UserService userService = new DefaultUserService();
    Logger LOGGER = LoggerFactory.getLogger(DefaultMessageService.class);

    public DefaultMessageService(OssFcm ossFcm) {
        this.ossFcm = ossFcm;
    }

    @Override
    public void addMessage(HttpServerRequest request, Integer requestId, String owner, String text, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Schooltoring.dbSchema + ".message(owner, request_id, text) " +
                "VALUES (?, ?, ?) RETURNING date;";
        JsonArray params = new JsonArray()
                .add(owner)
                .add(requestId)
                .add(text);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                handler.handle(new Either.Right<>(event.right().getValue()));
                sendMessageToUser(request, requestId, owner, text, event.right().getValue().getString("date"));
            } else {
                String errorMessage = "[DefaultMessageService@addMessage] An error occurred when creating message";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }

    /**
     * Send message to matched user
     *
     * @param request Http request. Needs by i18n API.
     * @param requestId request id
     * @param messageOwner message owner
     * @param text message text
     * @param date message date
     */
    private void sendMessageToUser(HttpServerRequest request, Integer requestId, String messageOwner, String text, String date) {
        this.getMatchId(requestId, messageOwner, event -> {
            if (event.isRight()) {
                String matchId = event.right().getValue();
                userService.getUsers(new JsonArray().add(messageOwner), userEvent -> {
                    if (userEvent.isRight()) {
                        JsonObject user = userEvent.right().getValue().getJsonObject(0);
                        NotificationUtils.getFcmTokensByUser(matchId, fcmEvent -> {
                            if (fcmEvent.isRight()) {
                                JsonArray tokens = fcmEvent.right().getValue();
                                String title = I18n.getInstance().translate("schooltoring.message.new", getHost(request), I18n.acceptLanguage(request), user.getString("username"));
                                JsonObject notification = new JsonObject()
                                        .put("title", title)
                                        .put("body", text);
                                JsonObject data = new JsonObject()
                                        .put("type", "NEW_MESSAGE")
                                        .put("request", requestId.toString())
                                        .put("owner", messageOwner)
                                        .put("date", date);
                                JsonObject message = new JsonObject()
                                        .put("notification", notification)
                                        .put("data", data);
                                for (Object token : tokens) {
                                    try {
                                        message.put("token", token);
                                        ossFcm.sendNotifications(new JsonObject().put("message", message));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        String errorMessage = "[DefaultMessageService@sendMessageToUser] Unable to send message to " + token + "for user " + matchId;
                                        LOGGER.error(errorMessage);
                                    }
                                }
                            } else {
                                String errorMessage = "[DefaultMessageService@sendMessageToUser] An error occurred when matching user fcm tokens";
                                LOGGER.error(errorMessage);
                            }
                        });
                    } else {
                        String errorMessage = "[DefaultMessageService@sendMessageToUser] An error occurred when matching user info";
                        LOGGER.error(errorMessage);
                    }
                });
            } else {
                String errorMessage = "[DefaultMessageService@sendMessageToUser] An error occurred when matching user";
                LOGGER.error(errorMessage);
            }
        });
    }

    /**
     * Get user id to send message
     *
     * @param requestId    request id
     * @param messageOwner message owner id
     * @param handler      Function handler returning data
     */
    private void getMatchId(Integer requestId, String messageOwner, Handler<Either<String, String>> handler) {
        String query = "SELECT CASE WHEN student_id = ? THEN owner ELSE student_id END as user " +
                "FROM " + Schooltoring.dbSchema + ".request " +
                "WHERE id = ? " +
                "AND (student_id = ? OR owner = ?)";

        JsonArray params = new JsonArray()
                .add(messageOwner)
                .add(requestId)
                .add(messageOwner)
                .add(messageOwner);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight() && event.right().getValue().size() > 0) {
                JsonObject result = event.right().getValue().getJsonObject(0);
                handler.handle(new Either.Right<>(result.getString("user")));
            } else {
                String errorMessage = "[DefaultMessageService@getMatchId] An error occurred when matching user";
                LOGGER.error(errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            }
        }));
    }
}
