package fr.openent.schooltoring;

import fr.openent.schooltoring.controller.SchooltoringController;
import org.entcore.common.http.BaseServer;

public class Schooltoring extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();

		addController(new SchooltoringController());
	}

}
