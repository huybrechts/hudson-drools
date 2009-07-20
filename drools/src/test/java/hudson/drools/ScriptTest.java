package hudson.drools;

import junit.framework.Assert;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class ScriptTest extends DroolsTestCase {

	private DroolsProject project;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		project = createProject("project", "script.rf");
	}

	public void testScriptExecution() throws Exception {
		String source = "println \"script executed\"";
		DroolsManagement.getInstance().setScripts(new Script("script", source));

		project.scheduleBuild();
		waitForWorkflowComplete(project, 1);

		DroolsRun build = project.getLastBuild();
		Assert.assertTrue(build.getLog().contains("script executed"));

		Assert.assertEquals(1, build.getScriptExecutions().size());
		Assert.assertEquals(ScriptExecution.Result.COMPLETED, build
				.getScriptExecutions().get(0).getResult());
	}

	public void testScriptExecutionFailed() throws Exception {
		String source = "throw new Exception()";
		DroolsManagement.getInstance().setScripts(new Script("script", source));

		project.scheduleBuild();

		Thread.sleep(1000);

		DroolsRun build = project.getLastBuild();

		Assert.assertTrue(build.getLog().contains("java.lang.Exception"));
		Assert.assertTrue(build.isRunning());
		Assert.assertEquals(1, build.getScriptExecutions().size());
		ScriptExecution scriptExecution = build
				.getScriptExecutions().get(0);
		Assert.assertEquals(ScriptExecution.Result.FAILED, scriptExecution.getResult());

		HtmlPage page = new WebClient().goTo(build.getUrl());
		HtmlAnchor anchor = (HtmlAnchor) page.getFirstByXPath("//a[@href='/"+scriptExecution.getUrl()+"/run']");
		Assert.assertNotNull(anchor);

		DroolsManagement.getInstance().setScripts(new Script("script", ""));
		
		anchor.click();

		waitForWorkflowComplete(project, 1);
		
		Assert.assertEquals(ScriptExecution.Result.COMPLETED, scriptExecution.getResult());
		
		page = new WebClient().goTo(build.getUrl());
		anchor = (HtmlAnchor) page.getFirstByXPath("//a[@href='/"+scriptExecution.getUrl()+"/run']");
		Assert.assertNull(anchor);
	}
}
