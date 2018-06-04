package fr.openent.schooltoring.controller;

import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

public class SchooltoringController extends ControllerHelper {

    @Get("")
    @SecuredAction("schooltoring.view")
    public void view(HttpServerRequest request) {
        renderView(request);
    }
}
