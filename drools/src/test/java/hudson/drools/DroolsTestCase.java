package hudson.drools;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.HudsonTestCase;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public abstract class DroolsTestCase extends HudsonTestCase {

	@Override
	protected void setUp() throws Exception {
		System.setProperty("hudson.bundled.plugins", "");
		super.setUp();

		new PluginImpl().start();
	}

	public DroolsProject createProject(String projectName, String resource)
			throws IOException {
		DroolsProject result = hudson.createProject(DroolsProject.class,
				projectName);
		String processXML = IOUtils.toString(getClass().getResourceAsStream(
				resource));
		result.set(null, processXML, null);

		return result;

	}

	public void assertBuildResult(Job<?, ?> job, Result result, int number) {
		for (int i = 0; i < 50; i++) {
			Run<?, ?> build = job.getBuildByNumber(number);
			if (build != null && !build.isBuilding()) {
				if (result != build.getResult()) {
					try {
						System.out.println(build.getLog());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Assert.assertEquals(result, build.getResult());
				System.out.println(build.getDisplayName() + " completed with " + result);
				return;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}

		}

		Run<?, ?> build = job.getBuildByNumber(number);
		if (build == null) {
			Assert.fail("No build for " + job.getDisplayName() + " after 10s");
		} else {
			Assert.fail(build.getDisplayName() + " not completed after 10s");
		}
	}

	public void assertBuildResult(DroolsProject job, Result result, int number) {
		for (int i = 0; i < 50; i++) {
			DroolsRun build = job.getBuildByNumber(number);
			if (build != null && !build.isBuilding()) {
				Assert.assertEquals(result, build.getResult());
				return;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}

		}
		Assert.fail("No build for " + job.getDisplayName() + " after 10s");
	}
	
	public void waitForWorkflowComplete(DroolsProject job, int number) {
		for (int i = 0; i < 50; i++) {
			DroolsRun build = job.getBuildByNumber(number);
			if (build != null && build.getStatus() != DroolsRun.Status.STARTED) {
				return;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}

		}
		Assert.fail("No build for " + job.getDisplayName() + " after 10s");
	}

	public HtmlForm getFormByAction(HtmlPage page, String action) {
		return (HtmlForm) page.getFirstByXPath("//form[@action='" + action
				+ "']");
	}
	
	public HtmlPage submitForm(HtmlForm form) throws IOException {
		HtmlButton button = (HtmlButton) form.getFirstByXPath("//button");
		return (HtmlPage) form.submit(button);
	}
}
