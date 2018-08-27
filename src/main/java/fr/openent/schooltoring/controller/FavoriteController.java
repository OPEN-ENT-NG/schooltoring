package fr.openent.schooltoring.controller;

import fr.openent.schooltoring.security.APIRight;
import fr.openent.schooltoring.security.FavoriteFilter;
import fr.openent.schooltoring.service.FavoriteService;
import fr.openent.schooltoring.service.impl.DefaultFavoriteService;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class FavoriteController extends ControllerHelper {

    private final FavoriteService favoriteService = new DefaultFavoriteService();

    @Get("/favorites")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void getFavorites(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            favoriteService.get(user.getUserId(), arrayResponseHandler(request));
        });
    }

    @Post("/favorite")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(APIRight.class)
    public void postFavorite(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "favorite", body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                favoriteService.add(body.getString("id"), user.getUserId(), defaultResponseHandler(request));
            });
        });
    }

    @Delete("/favorite/:id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(FavoriteFilter.class)
    public void deleteFavorite(HttpServerRequest request) {
        String userId = request.params().get("id");
        UserUtils.getUserInfos(eb, request, user -> {
            favoriteService.delete(userId, user.getUserId(), defaultResponseHandler(request));
        });
    }
}
