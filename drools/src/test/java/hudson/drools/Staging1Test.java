package hudson.drools;

import java.io.PrintWriter;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase.WebClient;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Assert;

public class Staging1Test extends DroolsTestCase {

	private DroolsProject wf;
	private FreeStyleProject build;
	private FreeStyleProject test;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		wf = createProject("staging-1", "staging-1.rf");

		build = hudson.createProject(FreeStyleProject.class, "Build");
		test = hudson.createProject(FreeStyleProject.class, "Automated Test");

	}

	public void testWorkflowWithTestSuccess() throws Exception {
		wf.scheduleBuild(0);

		try {
			assertBuildResult(wf, Result.SUCCESS, 1);
			assertBuildResult(build, Result.SUCCESS, 1);
			assertBuildResult(test, Result.SUCCESS, 1);

			waitForWorkflowComplete(wf, 1);

            Assert.assertTrue(wf.getLastBuild().getLog().contains("script executed"));

		} finally {
			// System.out.println(wf.getLastBuild().getLog());
		}

	}

	public void testWorkflowTestFailureAccepted() throws Exception {

		test.getBuildersList().add(new FailureBuilder());

		wf.scheduleBuild(0);

		assertBuildResult(wf, Result.SUCCESS, 1);
		assertBuildResult(build, Result.SUCCESS, 1);
		assertBuildResult(test, Result.FAILURE, 1);

        Assert.assertFalse(wf.getLastBuild().getLog().contains("script executed"));
		Assert.assertFalse(wf.getLastBuild().isCompleted());

		HtmlPage page = new WebClient().goTo(test.getLastBuild().getUrl());
		((HtmlAnchor) page.getElementById("drools_accept")).click();

		Thread.sleep(500);

		Assert.assertTrue(wf.getLastBuild().isCompleted());
	}

	public void testWorkflowTestFailureRestarted() throws Exception {

		test.getBuildersList().add(new FailureBuilder());

		wf.scheduleBuild(0);

		try {
			assertBuildResult(wf, Result.SUCCESS, 1);
			assertBuildResult(build, Result.SUCCESS, 1);
			assertBuildResult(test, Result.FAILURE, 1);

            Assert.assertFalse(wf.getLastBuild().getLog().contains("script executed"));
			Assert.assertFalse(wf.getLastBuild().isCompleted());

			// fix build
			test.getBuildersList().clear();

			HtmlPage page = new WebClient().goTo(test.getLastBuild().getUrl());
			((HtmlAnchor) page.getElementById("drools_restart")).click();

			System.out.println(test.getBuilds());

			assertBuildResult(test, Result.SUCCESS, 2);

            Assert.assertTrue(wf.getLastBuild().getLog().contains("script executed"));

			Assert.assertTrue(wf.getLastBuild().isCompleted());

		} finally {
			System.out.println(wf.getLastBuild().getLog());
		}

	}
}
