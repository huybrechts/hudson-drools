package hudson.drools;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.HudsonTestCase;

public abstract class DroolsTestCase extends HudsonTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		new PluginImpl().start();
	}

	public DroolsProject createProject(String projectName, String resource)
			throws IOException {
		DroolsProject result = hudson.createProject(DroolsProject.class,
				projectName);
		result.onLoad(Hudson.getInstance(), result.getName());
		String processXML = IOUtils.toString(getClass().getResourceAsStream(
				resource));
		result.updateProcess(processXML);

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
		} else{
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
}
