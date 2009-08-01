package hudson.drools;

import java.net.URL;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;

public class PluginImplTest extends DroolsTestCase {

	public void testDoWorkflowProjects() throws Exception {
		createProject("test", "staging-1.rf");
		
		URL url = new URL("http://localhost:"+localPort+"/plugin/drools/workflowProjects");
		String s = IOUtils.toString(url.openStream());
		Assert.assertEquals("test", s.trim());
	}
}
