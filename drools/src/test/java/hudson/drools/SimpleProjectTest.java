package hudson.drools;

import junit.framework.Assert;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;

import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.HudsonTestCase;

public class SimpleProjectTest extends HudsonTestCase {

	public void testWorkflow1() throws Exception {

		DroolsProject wf = hudson.createProject(DroolsProject.class,
				"SimpleProjectTest-" + hudson.getItems().size());
		wf.onLoad(Hudson.getInstance(), wf.getName());
		String processXML = IOUtils.toString(getClass().getResourceAsStream(
				"SimpleProjectTest-1.rf"));

		wf.updateProcess(processXML);
		
		FreeStyleProject project1 = hudson.createProject(FreeStyleProject.class, "Project1");
		
		wf.scheduleBuild(0);
		
		Thread.sleep(3000);
		
		Assert.assertEquals(1, project1.getBuilds().size() == 1);
		Assert.assertEquals(1, wf.getBuilds().size() == 1);
		Assert.assertEquals(Result.SUCCESS, wf.getLastBuild().getResult());

	}
}
