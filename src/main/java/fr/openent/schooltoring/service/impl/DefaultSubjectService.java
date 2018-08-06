package fr.openent.schooltoring.service.impl;

import fr.openent.schooltoring.service.SubjectService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

public class DefaultSubjectService implements SubjectService {
    @Override
    public void getSubjectsById(JsonArray subjects, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (sub:Subject) WHERE sub.id IN {subjects} return sub.id as subjectId, sub.label as subjectLabel";

        Neo4j.getInstance().execute(query, new JsonObject().put("subjects", subjects), Neo4jResult.validResultHandler(handler));
    }
}
