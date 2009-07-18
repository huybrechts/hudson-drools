package hudson.drools;

import hudson.model.Hudson;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.HudsonTestCase;

public class CreateProjectTest extends HudsonTestCase {

	public void testCreate() throws IOException {
        DroolsProject project = hudson.createProject(DroolsProject.class, "test-" + hudson.getItems().size() );
        project.onLoad(Hudson.getInstance(),project.getName());
        String processXML = IOUtils.toString(getClass().getResourceAsStream("staging-1.rf"));
        
        project.updateProcess(processXML);
	}
	
}
