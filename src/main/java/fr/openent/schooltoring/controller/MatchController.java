package fr.openent.schooltoring.controller;

import fr.openent.schooltoring.definition.Feature;
import fr.openent.schooltoring.security.APIRight;
import fr.openent.schooltoring.service.MatchService;
import fr.openent.schooltoring.service.impl.DefaultMatchService;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class MatchController extends ControllerHelper {

    private final MatchService matchService;

    public MatchController() {
        super();
        this.matchService = new DefaultMatchService();
    }

    @Get("/match/:state")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void matchUser(final HttpServerRequest request) {
        String state = request.params().get("state").toUpperCase();
        if (Feature.WEAKNESS.toString().equals(state) || Feature.STRENGTH.toString().equals(state)) {
            Integer page = request.params().contains("page")
                    ? Integer.parseInt(request.params().get("page"))
                    : 0;

            UserUtils.getUserInfos(eb, request, user -> {
                matchService.get(user.getUserId(), user.getStructures().get(0), state, page, arrayResponseHandler(request));
            });
        } else {
            badRequest(request);
        }
    }
}
