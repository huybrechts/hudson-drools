package hudson.drools;

import java.io.IOException;

import junit.framework.Assert;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.html.SubmittableElement;

public class DroolsManagementTest extends HudsonTestCase {

	public void testSubmit() throws IOException, SAXException {
		
		HtmlPage page = new WebClient().goTo("drools");
		HtmlForm form = (HtmlForm) page.getFirstByXPath("//form[@action='submit']");
		
		((HtmlTextInput) form.getFirstByXPath("//input[@name='id']")).type("id");
		((HtmlTextArea) form.getFirstByXPath("//textarea[@name='source']")).setText("source");
		
		form.submit((SubmittableElement) form.getFirstByXPath("//span[@name='Submit']//button"));
		
		Assert.assertEquals(1, DroolsManagement.getInstance().getScripts().size());
		Script script = DroolsManagement.getInstance().getScript("id");
		Assert.assertNotNull("no script with id 'id'", script);
		Assert.assertEquals("id", script.getId());
		Assert.assertEquals("source", script.getSource());
		
	}
	
}
