package fr.openent.schooltoring;

import fr.openent.schooltoring.controller.*;
import fr.openent.schooltoring.service.MessageService;
import fr.openent.schooltoring.service.impl.DefaultMessageService;
import fr.wseduc.webutils.http.oauth.OAuth2Client;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.ws.OssFcm;

import java.net.URI;

public class Schooltoring extends BaseServer {

    public static String dbSchema;

    @Override
    public void start() throws Exception {
        super.start();
        dbSchema = config.getString("db-schema");

        final ConversationController conversationController = new ConversationController();

        addController(new SchooltoringController());
        addController(new ProfileController());
        addController(new MatchController());
        addController(new RequestController());
        addController(conversationController);
        addController(new FavoriteController());

        JsonObject messaging = config.getJsonObject("messaging");

        if (messaging != null) {
            OAuth2Client googleOAuth2SSO = new OAuth2Client(URI.create(messaging.getString("uri")),
                    null, null, null,
                    messaging.getString("tokenUrn"), null, vertx,
                    messaging.getInteger("poolSize", 16), true);
            OssFcm oss = new OssFcm(googleOAuth2SSO, messaging.getString("client_mail"), messaging.getString("scope"),
                    messaging.getString("aud"), messaging.getString("url"), messaging.getString("key"));

            MessageService messageService = new DefaultMessageService(oss);
            conversationController.setMessageService(messageService);
        } else {
            vertx.setTimer(1000, event -> {
                log.error("[Schooltoring@start] messaging configuration undefined. Killing mod... Please set messaging configuration.");
                LocalMap<String, String> deploymentsIdMap = vertx.sharedData().getLocalMap("deploymentsId");
                vertx.undeploy(deploymentsIdMap.get("fr.openent.schooltoring"));
            });
        }
    }
}
