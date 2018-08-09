package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.service.UserService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

public class DefaultUserService implements UserService {

    @Override
    public void getUsers(JsonArray users, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[:USERBOOK]->(ub:UserBook) " +
                "WHERE u.id IN {users} " +
                "return u.id as id, u.displayName as username, u.classes as classNames, ub.picture as avatar";
        JsonObject params = new JsonObject()
                .put("users", users);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}
