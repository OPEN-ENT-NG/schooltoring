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

public class ConversationFilter implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        try {
            String query = "SELECT count(*) FROM " + Schooltoring.dbSchema + ".conversation_users " +
                    "WHERE id = ?  AND conversation_id = ?";

            JsonArray params = new JsonArray()
                    .add(user.getUserId())
                    .add(Integer.parseInt(request.params().get("conversationId")));
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