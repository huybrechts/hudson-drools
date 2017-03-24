package hudson.drools;

import org.junit.Assert;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Ignore;

public class ScriptTest extends DroolsTestCase {

	private DroolsProject project;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		project = createProject("project", "script.rf");
	}

	public void testScriptExecution() throws Exception {
		project.scheduleBuild();
		waitForWorkflowComplete(project, 1);

		DroolsRun build = project.getLastBuild();
		Assert.assertTrue(build.getLog().contains("script executed"));

		Assert.assertEquals(1, build.getScriptExecutions().size());
		Assert.assertEquals(ScriptExecution.Result.COMPLETED, build
				.getScriptExecutions().get(0).getResult());
	}

    /*
	public void testScriptExecutionFailed() throws Exception {
		project.scheduleBuild();

		Thread.sleep(1000);

		DroolsRun build = project.getLastBuild();

		Assert.assertTrue(build.getLog().contains("java.lang.Exception"));
		Assert.assertTrue(build.isRunning());
		Assert.assertEquals(1, build.getScriptExecutions().size());
		ScriptExecution scriptExecution = build.getScriptExecutions().get(0);
		Assert.assertEquals(ScriptExecution.Result.FAILED, scriptExecution
				.getResult());

		HtmlPage page = new WebClient().goTo(build.getUrl());
		HtmlAnchor anchor = (HtmlAnchor) page.getFirstByXPath("//a[@href='/"
				+ scriptExecution.getUrl() + "/run']");
		Assert.assertNotNull(anchor);

//		DroolsManagement.getInstance().setScripts(new GroovyScript("script", ""));

		anchor.click();

		waitForWorkflowComplete(project, 1);

		Assert.assertEquals(ScriptExecution.Result.COMPLETED, scriptExecution
				.getResult());

		page = new WebClient().goTo(build.getUrl());
		anchor = (HtmlAnchor) page.getFirstByXPath("//a[@href='/"
				+ scriptExecution.getUrl() + "/run']");
		Assert.assertNull(anchor);
	}
	*/

	public void testScriptParameters() throws Exception {
		StringBuilder source = new StringBuilder();
		source.append("assert hudson != null\n");
		source.append("assert session != null\n");
		source.append("assert args != null\n");
		source.append("assert out != null\n");
//		DroolsManagement.getInstance().setScripts(
//				new GroovyScript("script", source.toString()));

		project.scheduleBuild();
		waitForWorkflowComplete(project, 1);

		Assert.assertEquals(ScriptExecution.Result.COMPLETED, project
				.getLastBuild().getScriptExecutions().get(0).getResult());
	}
}
