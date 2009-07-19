package hudson.drools;

import hudson.model.Hudson;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.html.SubmittableElement;

public class CreateProjectTest extends HudsonTestCase {

	public void testCreate() throws IOException {
        DroolsProject project = hudson.createProject(DroolsProject.class, "test-" + hudson.getItems().size() );
        project.onLoad(Hudson.getInstance(),project.getName());
        String processXML = IOUtils.toString(getClass().getResourceAsStream("staging-1.rf"));
        
        project.updateProcess(processXML);
	}
	
	public void testCreateViaBrowser() throws IOException, SAXException {
		HtmlPage createProjectPage = new WebClient().goTo("/newJob");
		HtmlForm form = (HtmlForm) createProjectPage.getFirstByXPath("//form[@action='createItem']");
		((HtmlTextInput) form.getInputByName("name")).type("project name");
		((HtmlRadioButtonInput) form.getFirstByXPath("//input[@value='"+DroolsProject.DescriptorImpl.class.getName()+"']")).click();
		
		
		HtmlButton button = (HtmlButton) form.getFirstByXPath("//button");
		HtmlPage projectPage = (HtmlPage) form.submit(button);
		
		System.out.println(projectPage.getTitleText());
		
		DroolsProject project = (DroolsProject) hudson.getItem("project name");
		Assert.assertNotNull("no project created", project);
		Assert.assertTrue("project not disabled after creation", project.isDisabled());
		
		form = (HtmlForm) projectPage.getFirstByXPath("//form[@action='configSubmit']");
		String description = "a description";
		form.getTextAreaByName("description").setText(description);
		String triggerSpec = "1 2 3 4 5";
		form.getTextAreaByName("triggerSpec").setText(triggerSpec);
		String processXML = IOUtils.toString(getClass().getResourceAsStream("staging-1.rf"));
		form.getTextAreaByName("processXML").setText(processXML);
		
		form.submit((SubmittableElement) form.getFirstByXPath("//button"));
		
		Assert.assertEquals(processXML, project.getProcessXML());
		Assert.assertEquals(triggerSpec, project.getTriggerSpec());
		Assert.assertNotNull(project.getSession());
		Assert.assertFalse(project.isDisabled());
		Assert.assertEquals(description, project.getDescription());
		
	}
	
}
