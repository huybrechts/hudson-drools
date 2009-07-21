package hudson.drools;

import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class RuleFlowRendererTest extends DroolsTestCase {

	public void testWorkflow1() throws Exception {
		DroolsProject wf = createProject("SimpleProject",
				"SimpleProjectTest-1.rf");

		FreeStyleProject project1 = hudson.createProject(
				FreeStyleProject.class, "Project1");

		wf.scheduleBuild(0);

		assertBuildResult(wf, Result.SUCCESS, 1);
		assertBuildResult(project1, Result.SUCCESS, 1);

		WebClient wc = new WebClient();
		wc.goTo(wf.getUrl() + "/processImage", "image/png");
		wc.goTo(wf.getLastBuild().getUrl() + "/processInstanceImage",
				"image/png");

	}

	public void testWorkflow2() throws Exception {
		DroolsProject wf = createProject("staging-3", "staging-3.rf");

		FreeStyleProject build = hudson.createProject(FreeStyleProject.class,
				"Build");
		FreeStyleProject test = hudson.createProject(FreeStyleProject.class,
				"Automated Test");
		FreeStyleProject test2 = hudson.createProject(FreeStyleProject.class,
				"Another Automated Test");

		DroolsManagement.getInstance().getScripts().add(
				new Script("DeployStagedRelease", ""));
		wf.scheduleBuild(0);

		WebClient wc = new WebClient();
		wc.goTo(wf.getUrl() + "/processImage", "image/png");
		wc.goTo(wf.getLastBuild().getUrl() + "/processInstanceImage",
				"image/png");

		waitForWorkflowComplete(wf, 1);

		wc.goTo(wf.getLastBuild().getUrl() + "/processInstanceImage",
				"image/png");

	}

	public void testWorkflow3() throws Exception {
		DroolsProject wf = createProject("release-vote", "release-vote.rf");

		FreeStyleProject build = hudson.createProject(FreeStyleProject.class,
				"Build");

		DroolsManagement.getInstance().getScripts().add(
				new Script("DeployStagedRelease", ""));

		WebClient wc = new WebClient();
		wc.goTo(wf.getUrl() + "/processImage", "image/png");
		
		wf.scheduleBuild(0);
		
		assertBuildResult(wf, Result.SUCCESS, 1);
		
		wc.goTo(wf.getLastBuild().getUrl() + "/processInstanceImage",
				"image/png");
		
		wf.getLastBuild().cancel();

	}
}
