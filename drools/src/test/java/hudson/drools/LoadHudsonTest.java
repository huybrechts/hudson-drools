package hudson.drools;

import junit.framework.Assert;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

public class LoadHudsonTest extends HudsonTestCase {

	@LocalData
	public void testLoad() {
		
		Assert.assertNotNull(hudson.getItem("Staging Workflow 1"));
		Assert.assertFalse(((DroolsProject) hudson.getItem("Staging Workflow 1")).isDisabled());

		Assert.assertNotNull(hudson.getItem("Staging Workflow 2"));
		Assert.assertFalse(((DroolsProject) hudson.getItem("Staging Workflow 2")).isDisabled());

		Assert.assertNotNull(hudson.getItem("Staging Workflow 3"));
		Assert.assertFalse(((DroolsProject) hudson.getItem("Staging Workflow 3")).isDisabled());
		
	}
	
	
}
