package hudson.drools;

import hudson.model.FreeStyleProject;
import hudson.model.Result;

import java.io.PrintWriter;
import java.util.Map;

import junit.framework.Assert;

import org.drools.runtime.StatefulKnowledgeSession;
import org.jvnet.hudson.test.FailureBuilder;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Staging3Test extends DroolsTestCase {

	private DroolsProject wf;
	private FreeStyleProject build;
	private FreeStyleProject test;
	private FreeStyleProject test2;

	private boolean deployScriptCalled;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		wf = createProject("staging-3", "staging-3.rf");
		
		build = hudson.createProject(FreeStyleProject.class, "Build");
		test = hudson.createProject(FreeStyleProject.class, "Automated Test");
		test2 = hudson.createProject(FreeStyleProject.class, "Another Automated Test");

		deployScriptCalled = false;

		DroolsManagement.getInstance().getScripts().add(
				new Script("DeployStagedRelease", "") {
					@Override
					public Map execute(StatefulKnowledgeSession session,
							PrintWriter output, Map<String, Object> parameters)
							throws Exception {
						deployScriptCalled = true;
						return super.execute(session, output, parameters);
					}
				});
	}

	public void xtestWorkflowWithTestSuccess() throws Exception {
		wf.scheduleBuild(0);

		try {
			assertBuildResult(wf, Result.SUCCESS, 1);
			assertBuildResult(build, Result.SUCCESS, 1);
			assertBuildResult(test, Result.SUCCESS, 1);
			assertBuildResult(test2, Result.SUCCESS, 1);

			Assert.assertTrue(deployScriptCalled);

		} finally {
			System.out.println(wf.getLastBuild().getLog());
		}

	}

	public void testWorkflowManualOverride() throws Exception {

		test2.getBuildersList().add(new FailureBuilder());

		wf.scheduleBuild(0);

		try {
			assertBuildResult(wf, Result.SUCCESS, 1);
			assertBuildResult(build, Result.SUCCESS, 1);
			assertBuildResult(test, Result.SUCCESS, 1);
			assertBuildResult(test2, Result.FAILURE, 1);

			Assert.assertFalse(deployScriptCalled);
			Assert.assertFalse(wf.getLastBuild().isCompleted());
			
			Assert.assertEquals(1, wf.getLastBuild().getHumanTasks().size());
			HumanTask humanTask = wf.getLastBuild().getHumanTasks().get(0);
			
			HtmlPage page = new WebClient().goTo(wf.getLastBuild().getUrl());
			
			HtmlForm form = page.getFormByName("drools-humanTask-" + humanTask.getWorkItemId());
			((HtmlCheckBoxInput) form.getInputByName("value")).setChecked(true);
			form.submit((HtmlButton) form.getFirstByXPath("//button"));
			
			Thread.sleep(500);
			
			Assert.assertTrue(deployScriptCalled);
			
			Assert.assertTrue(wf.getLastBuild().isCompleted());
			
		} finally {
			System.out.println(wf.getLastBuild().getLog());
		}

	}

	public void testWorkflowNoManualOverride() throws Exception {

		test2.getBuildersList().add(new FailureBuilder());

		wf.scheduleBuild(0);

		try {
			assertBuildResult(wf, Result.SUCCESS, 1);
			assertBuildResult(build, Result.SUCCESS, 1);
			assertBuildResult(test, Result.SUCCESS, 1);
			assertBuildResult(test2, Result.FAILURE, 1);

			Assert.assertFalse(deployScriptCalled);
			Assert.assertFalse(wf.getLastBuild().isCompleted());
			
			Assert.assertEquals(1, wf.getLastBuild().getHumanTasks().size());
			HumanTask humanTask = wf.getLastBuild().getHumanTasks().get(0);
			
			HtmlPage page = new WebClient().goTo(wf.getLastBuild().getUrl());
			
			HtmlForm form = page.getFormByName("drools-humanTask-" + humanTask.getWorkItemId());
			form.submit((HtmlButton) form.getFirstByXPath("//button"));
			
			Thread.sleep(500);
			
			Assert.assertFalse(deployScriptCalled);
			
			Assert.assertTrue(wf.getLastBuild().isCompleted());
			
		} finally {
			System.out.println(wf.getLastBuild().getLog());
		}

	}

	public void testWorkflowNoManualOverridex() throws Exception {

		test2.getBuildersList().add(new FailureBuilder());

		wf.scheduleBuild(0);

		try {
			assertBuildResult(wf, Result.SUCCESS, 1);
			assertBuildResult(build, Result.SUCCESS, 1);
			assertBuildResult(test, Result.SUCCESS, 1);
			assertBuildResult(test2, Result.FAILURE, 1);

			Assert.assertFalse(deployScriptCalled);
			Assert.assertFalse(wf.getLastBuild().isCompleted());
			
			Assert.assertEquals(1, wf.getLastBuild().getHumanTasks().size());
			HumanTask humanTask = wf.getLastBuild().getHumanTasks().get(0);
			
			HtmlPage page = new WebClient().goTo(wf.getLastBuild().getUrl());
			
			HtmlForm form = page.getFormByName("drools-humanTask-" + humanTask.getWorkItemId());
			form.submit((HtmlButton) form.getFirstByXPath("//button"));
			
			Thread.sleep(500);
			
			Assert.assertFalse(deployScriptCalled);
			
			Assert.assertTrue(wf.getLastBuild().isCompleted());
			
		} finally {
			System.out.println(wf.getLastBuild().getLog());
		}

	}

	public void testWorkflowNoManualOverridexx() throws Exception {

		test2.getBuildersList().add(new FailureBuilder());

		wf.scheduleBuild(0);

		try {
			assertBuildResult(wf, Result.SUCCESS, 1);
			assertBuildResult(build, Result.SUCCESS, 1);
			assertBuildResult(test, Result.SUCCESS, 1);
			assertBuildResult(test2, Result.FAILURE, 1);

			Assert.assertFalse(deployScriptCalled);
			Assert.assertFalse(wf.getLastBuild().isCompleted());
			
			Assert.assertEquals(1, wf.getLastBuild().getHumanTasks().size());
			HumanTask humanTask = wf.getLastBuild().getHumanTasks().get(0);
			
			HtmlPage page = new WebClient().goTo(wf.getLastBuild().getUrl());
			
			HtmlForm form = page.getFormByName("drools-humanTask-" + humanTask.getWorkItemId());
			form.submit((HtmlButton) form.getFirstByXPath("//button"));
			
			Thread.sleep(500);
			
			Assert.assertFalse(deployScriptCalled);
			
			Assert.assertTrue(wf.getLastBuild().isCompleted());
			
		} finally {
			System.out.println(wf.getLastBuild().getLog());
		}

	}
}
