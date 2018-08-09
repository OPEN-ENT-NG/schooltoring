package fr.openent.schooltoring.security;

import fr.openent.schooltoring.Schooltoring;
import fr.openent.schooltoring.security.utils.WorkflowActionUtils;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class RequestFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        try {
            String query = "SELECT COUNT(*) FROM " + Schooltoring.dbSchema + ".request WHERE id = ?" +
                    "AND (owner = ? OR student_id = ?)";

            JsonArray params = new JsonArray().add(Integer.parseInt(request.params().get("requestId")))
                    .add(user.getUserId()).add(user.getUserId());
            request.pause();
            Sql.getInstance().prepared(query, params, event -> {
                request.resume();
                Long count = SqlResult.countResult(event);
                handler.handle(WorkflowActionUtils.hasRight(user, WorkflowActions.VIEW.toString()) && count != null && count > 0);
            });
        } catch (ClassCastException err) {
            handler.handle(false);
            throw err;
        }
    }
}
