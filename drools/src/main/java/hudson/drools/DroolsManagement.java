package hudson.drools;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.model.Descriptor.FormException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@Extension
public class DroolsManagement extends ManagementLink {

	private List<GroovyScript> scripts = new ArrayList<GroovyScript>();

	public DroolsManagement() {
		try {
			if (getConfigFile().exists())
				new XmlFile(getConfigFile()).unmarshal(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getIconFileName() {
		return "/plugin/drools/icons/drools.gif";
	}

	@Override
	public String getUrlName() {
		return "drools";
	}

	public String getDisplayName() {
		return "Drools Configuration";
	}

	public static DroolsManagement getInstance() {
		return ManagementLink.all().get(DroolsManagement.class);
	}

	public void doSubmit(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException, FormException {
		Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

		JSONObject form = req.getSubmittedForm();
		setScripts(req.bindJSONToList(GroovyScript.class, form.get("scripts")));

		save();

		rsp.forwardToPreviousPage(req);
	}

	public List<GroovyScript> getScripts() {
		return scripts;
	}

	public void setScripts(List<GroovyScript> scripts) {
		this.scripts = new ArrayList<GroovyScript>(scripts);
	}

	public void setScripts(GroovyScript... scripts) {
		this.scripts = new ArrayList<GroovyScript>(Arrays.asList(scripts));
	}
	
	private File getConfigFile() {
		return new File(Hudson.getInstance().getRootDir(), "drools.xml");
	}

	public void save() throws IOException {
		new XmlFile(getConfigFile()).write(this);
	}

	public GroovyScript getScript(String id) {
		for (GroovyScript script : scripts) {
			if (id.equals(script.getId())) {
				return script;
			}
		}
		return null;
	}

}
