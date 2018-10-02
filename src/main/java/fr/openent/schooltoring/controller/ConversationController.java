package fr.openent.schooltoring.controller;

import fr.openent.schooltoring.security.APIRight;
import fr.openent.schooltoring.security.ConversationFilter;
import fr.openent.schooltoring.service.ConversationService;
import fr.openent.schooltoring.service.MessageService;
import fr.openent.schooltoring.service.impl.DefaultConversationService;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.sql.Timestamp;
import java.util.Date;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ConversationController extends ControllerHelper {

    private final ConversationService conversationService = new DefaultConversationService();
    private MessageService messageService;

    @Post("/conversation/:conversationId/message")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ConversationFilter.class)
    public void postMessage(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "message", body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                try {
                    Integer requestId = Integer.parseInt(request.params().get("conversationId"));
                    messageService.addMessage(request, requestId, user.getUserId(), body.getString("text"), defaultResponseHandler(request));
                } catch (ClassCastException err) {
                    badRequest(request);
                    throw err;
                }
            });
        });
    }

    @Get("/conversation/:conversationId/messages")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ConversationFilter.class)
    public void getMessages(HttpServerRequest request) {
        try {
            Integer requestId = Integer.parseInt(request.params().get("conversationId"));
            String lastMessage = request.params().contains("lastMessage") ? request.params().get("lastMessage") : new Timestamp(new Date().getTime()).toString();

            conversationService.getMessages(requestId, lastMessage, arrayResponseHandler(request));
        } catch (ClassCastException err) {
            badRequest(request);
            throw err;
        }
    }

    @Get("/conversations")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void getConvesations(HttpServerRequest request) {
        String state = request.params().get("state");
        UserUtils.getUserInfos(eb, request, user -> {
            conversationService.getConversations(user.getUserId(), arrayResponseHandler(request));
        });
    }

    public void setMessageService(MessageService messageService) {
        this.messageService = messageService;
    }
}
