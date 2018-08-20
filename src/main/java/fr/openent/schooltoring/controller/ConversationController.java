package fr.openent.schooltoring.controller;

import fr.openent.schooltoring.definition.Feature;
import fr.openent.schooltoring.definition.RequestStatus;
import fr.openent.schooltoring.security.APIRight;
import fr.openent.schooltoring.security.RequestFilter;
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

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ConversationController extends ControllerHelper {

    private final ConversationService conversationService = new DefaultConversationService();
    private MessageService messageService;

    @Post("/conversation/:requestId/message")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(RequestFilter.class)
    public void postMessage(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "message", body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                try {
                    Integer requestId = Integer.parseInt(request.params().get("requestId"));
                    messageService.addMessage(request, requestId, user.getUserId(), body.getString("text"), defaultResponseHandler(request));
                } catch (ClassCastException err) {
                    badRequest(request);
                    throw err;
                }
            });
        });
    }

    @Get("/conversation/:requestId/messages")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(RequestFilter.class)
    public void getMessages(HttpServerRequest request) {
        try {
            Integer requestId = Integer.parseInt(request.params().get("requestId"));
            Integer page = request.params().contains("page") ? Integer.parseInt(request.params().get("page")) : 0;

            conversationService.getMessages(requestId, page, arrayResponseHandler(request));
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
        if (state != null && !Feature.WEAKNESS.toString().equals(state) && !Feature.STRENGTH.toString().equals(state)) {
            badRequest(request);
        } else {
            UserUtils.getUserInfos(eb, request, user -> {
                List<String> status;
                if (request.params().contains("status")) {
                    status = request.params().getAll("status");
                } else {
                    status = new ArrayList<>();
                    status.add(RequestStatus.WAITING.toString());
                    status.add(RequestStatus.ACCEPTED.toString());
                }
                conversationService.getConversations(user.getUserId(), state, status, arrayResponseHandler(request));
            });
        }
    }

    public void setMessageService(MessageService messageService) {
        this.messageService = messageService;
    }
}
