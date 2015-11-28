package hudson.drools;

import hudson.model.Result;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.html.SubmittableElement;
import org.junit.Ignore;

public class DroolsProjectTest extends DroolsTestCase {

	public void testCreate() throws Exception {
		DroolsProject project = createProject("project", "staging-1.rf");
		String processXML = IOUtils.toString(getClass().getResourceAsStream(
				"staging-1.rf"));

		Assert.assertNotNull(hudson.getItem("project"));
		Assert.assertEquals(processXML, project.getProcessXML());
		Assert.assertNotNull(project.getSession());
		Assert.assertFalse(project.isDisabled());
	}

	public void testCreateViaBrowser() throws Exception {
		HtmlPage createProjectPage = new WebClient().goTo("/newJob");
		HtmlForm form = createProjectPage.getFirstByXPath("//form[@action='createItem']");
		form.getInputByName("name").type("project name");
		((HtmlRadioButtonInput) form.getFirstByXPath("//input[@value='"
				+ DroolsProject.DescriptorImpl.class.getName() + "']")).click();

		HtmlButton button = form.getFirstByXPath("//button");
		HtmlPage projectPage = button.click();

		DroolsProject project = (DroolsProject) hudson.getItem("project name");
		Assert.assertNotNull("no project created", project);
		Assert.assertTrue("project not disabled after creation", project
				.isDisabled());

		form = (HtmlForm) projectPage
				.getFirstByXPath("//form[@action='configSubmit']");
		String description = "a description";
		form.getTextAreaByName("description").setText(description);
		String triggerSpec = "1 2 3 4 5";
		form.getTextAreaByName("triggerSpec").setText(triggerSpec);
		String processXML = IOUtils.toString(getClass().getResourceAsStream(
				"staging-1.rf"));
		form.getTextAreaByName("processXML").setText(processXML);

		((HtmlButton) form.getFirstByXPath("//button")).click();

		Assert.assertEquals(processXML, project.getProcessXML());
		Assert.assertEquals(triggerSpec, project.getTriggerSpec());
		Assert.assertNotNull(project.getSession());
		Assert.assertFalse(project.isDisabled());
		Assert.assertEquals(description, project.getDescription());

	}

	public void testDelete() throws IOException, InterruptedException {
		DroolsProject project = createProject("project",
				"SimpleProjectTest-1.rf");
		Assert.assertNotNull(hudson.getItem("project"));
		project.delete();
		Assert.assertNull(hudson.getItem("project"));
	}

	public void testDeleteViaBrowser() throws Exception {
		createProject("project", "SimpleProjectTest-1.rf");

		Assert.assertNotNull(hudson.getItem("project"));

		HtmlPage page = new WebClient().goTo("job/project");
		page = (HtmlPage) page.getAnchorByHref("/job/project/delete").click();
		HtmlForm form = getFormByAction(page, "doDelete");
		submitForm(form);

		Assert.assertNull(hudson.getItem("project"));
	}

    @Ignore("not updated for jar-upload")
	public void testBuildViaBrowser() throws Exception {
       /*
		DroolsProject project = createProject("project",
				"SimpleProjectTest-1.rf");
		createFreeStyleProject("Project1");

		HtmlPage page = new WebClient().goTo("job/project");
		((HtmlAnchor) page.getFirstByXPath("//a[text()='Build Now']")).click();
		assertBuildResult(project, Result.SUCCESS, 1);

		waitForWorkflowComplete(project, 1);
		*/
	}

    @Ignore("not updated for jar-upload")
	public void testDoUpdateViaIDE() throws Exception {
    /*
		DroolsProject project = createProject("project",
				"SimpleProjectTest-1.rf");
		createFreeStyleProject("Project1");

		String processXML = IOUtils.toString(getClass().getResourceAsStream(
				"staging-1.rf"));

		URL url = new URL("http://localhost:" + localPort + "/"
				+ project.getUrl() + "submitWorkflow");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn
				.setRequestProperty(hudson.getCrumbIssuer().getDescriptor()
						.getCrumbRequestField(), hudson.getCrumbIssuer()
						.getCrumb(null));
		PrintStream stream = new PrintStream(conn.getOutputStream());
		stream.print(processXML);
		stream.close();
		assertEquals(200, conn.getResponseCode());
		Assert.assertEquals(processXML, project.getProcessXML());
	*/
	}
}
