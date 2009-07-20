package hudson.drools;

import hudson.Plugin;
import hudson.model.Hudson;

import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class PluginImpl extends Plugin {

	@Override
	public void stop() throws Exception {
		for (DroolsProject project : Hudson.getInstance().getItems(
				DroolsProject.class)) {
			project.dispose();
		}
	}

	/**
	 * Used by IDE plugin
	 */
	public void doWorkflowProjects(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		PrintWriter pw = new PrintWriter(rsp.getOutputStream());
		for (DroolsProject project : Hudson.getInstance().getItems(
				DroolsProject.class)) {
			pw.println(project.getName());
		}
		pw.flush();
		pw.close();
	}

}
