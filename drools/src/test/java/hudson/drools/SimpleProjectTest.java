package hudson.drools;

import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class SimpleProjectTest extends DroolsTestCase {

	public void testWorkflow1() throws Exception {
		DroolsProject wf = createProject("SimpleProject",
				"SimpleProjectTest-1.rf");

		FreeStyleProject project1 = hudson.createProject(
				FreeStyleProject.class, "Project1");

		wf.scheduleBuild(0);

		assertBuildResult(wf, Result.SUCCESS, 1);
		assertBuildResult(project1, Result.SUCCESS, 1);
		
		waitForWorkflowComplete(wf, 1);

	}
}
