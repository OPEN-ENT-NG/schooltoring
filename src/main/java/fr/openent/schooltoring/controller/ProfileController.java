package fr.openent.schooltoring.controller;

import fr.openent.schooltoring.security.APIRight;
import fr.openent.schooltoring.service.ProfileService;
import fr.openent.schooltoring.service.impl.DefaultProfileService;
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

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ProfileController extends ControllerHelper {
    private final ProfileService profileService;

    public ProfileController() {
        this.profileService = new DefaultProfileService();
    }

    @Get("/profile")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void getProfile(HttpServerRequest request) {
        UserUtils.getSession(eb, request, user -> profileService.get(user.getString("userId"), defaultResponseHandler(request)));
    }

    @Post("/profile")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void postProfile(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "profile", profile -> {
            UserUtils.getUserInfos(eb, request, user -> profileService.set(user.getUserId(), profile, defaultResponseHandler(request)));
        });
    }

    @Put("/profile")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void putProfile(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "profile", profile -> {
            UserUtils.getUserInfos(eb, request, user -> profileService.update(user.getUserId(), profile, defaultResponseHandler(request)));
        });
    }
}
