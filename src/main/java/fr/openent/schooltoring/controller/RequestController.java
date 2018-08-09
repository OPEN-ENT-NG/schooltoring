package fr.openent.schooltoring.controller;

import fr.openent.schooltoring.definition.Feature;
import fr.openent.schooltoring.definition.RequestStatus;
import fr.openent.schooltoring.security.APIRight;
import fr.openent.schooltoring.security.RequestFilter;
import fr.openent.schooltoring.service.RequestService;
import fr.openent.schooltoring.service.impl.DefaultRequestService;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
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

public class RequestController extends ControllerHelper {

    private final RequestService requestService = new DefaultRequestService();


    @Post("/request")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void postRequest(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "request", body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                requestService.create(user.getUserId(), body, defaultResponseHandler(request));
            });
        });
    }

    @Put("/request/:requestId/:status")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(RequestFilter.class)
    public void putRequest(HttpServerRequest request) {
        String status = request.params().get("status").toUpperCase();
        if (!"ACCEPT".equals(status) || !"CANCEL".equals(status)) {
            try {
                Integer requestId = Integer.parseInt(request.params().get("requestId"));
                status = "ACCEPT".equals(status) ? RequestStatus.ACCEPTED.toString() : RequestStatus.CANCELED.toString();
                requestService.updateStatus(requestId, status, defaultResponseHandler(request));
            } catch (ClassCastException err) {
                badRequest(request);
                throw err;
            }
        } else {
            badRequest(request);
        }
    }

    @Get("/requests")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void getRequests(HttpServerRequest request) {
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
                }
                requestService.getRequests(user.getUserId(), state, status, arrayResponseHandler(request));
            });
        }
    }
}
