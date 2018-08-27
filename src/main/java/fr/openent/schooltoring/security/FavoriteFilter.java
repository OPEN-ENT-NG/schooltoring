package fr.openent.schooltoring.security;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.security.utils.WorkflowActionUtils;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;

public class FavoriteFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String query = "SELECT student_id FROM " + Schooltoring.dbSchema + ".favorite WHERE owner = ? AND student_id = ?;";
        JsonArray params = new JsonArray()
                .add(user.getUserId())
                .add(request.params().get("id"));

        request.pause();
        Sql.getInstance().prepared(query, params, event -> {
            request.resume();
            JsonObject body = event.body();
            Integer count = "ok".equals(body.getString("status")) ? body.getInteger("rows") : null;
            handler.handle(WorkflowActionUtils.hasRight(user, WorkflowActions.VIEW.toString()) && count != null && count > 0);
        });
    }
}
