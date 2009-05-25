package hudson.drools;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.model.Descriptor.FormException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.hibernate.dialect.MySQLDialect;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@Extension
public class DroolsManagement extends ManagementLink {

	private List<Script> scripts;

	private DBSettings dbSettings;

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
		setScripts(req.bindJSONToList(Script.class, form.get("scripts")));

		String dbs = req.getParameter("dbSettings");
		int dbsIdx = Integer.parseInt(dbs);
		dbSettings = getDBSettingsDescriptors().get(dbsIdx).newInstance(req,
				form.getJSONObject("dbSettings"));

		save();

		rsp.forwardToPreviousPage(req);
	}

	public List<Script> getScripts() {
		return scripts;
	}

	public void setScripts(List<Script> scripts) {
		this.scripts = scripts;
	}

	private File getConfigFile() {
		return new File(Hudson.getInstance().getRootDir(), "drools.xml");
	}

	public void save() throws IOException {
		new XmlFile(getConfigFile()).write(this);
	}

	public Script getScript(String id) {
		for (Script script : scripts) {
			if (id.equals(script.getId())) {
				return script;
			}
		}
		return null;
	}

	public DBSettings getDbSettings() {
		if (dbSettings == null) {

			if (System.getProperty("jboss.home.dir") != null) {
				dbSettings = new JBossDBSettings();
			} else {
				dbSettings = new EmbeddedDBSettings();
			}
		}
		return dbSettings;
	}

	public DescriptorExtensionList<DBSettings, Descriptor<DBSettings>> getDBSettingsDescriptors() {
		return DBSettings.all();
	}

	public void setDbSettings(DBSettings dbSettings) {
		this.dbSettings = dbSettings;
	}

}
