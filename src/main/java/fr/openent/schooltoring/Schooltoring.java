package fr.openent.schooltoring;

import fr.openent.schooltoring.controller.ProfileController;
import fr.openent.schooltoring.controller.SchooltoringController;
import org.entcore.common.http.BaseServer;

public class Schooltoring extends BaseServer {

    public static String dbSchema;

	@Override
	public void start() throws Exception {
		super.start();
		dbSchema = config.getString("db-schema");

		addController(new SchooltoringController());
		addController(new ProfileController());
	}

}
