package hudson.drools;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class PluginImpl extends Plugin {

	private static final Logger logger = Logger.getLogger(Plugin.class.getName());

	@Override
	@SuppressWarnings( { "deprecation", "unchecked" })
	public void start() throws Exception {
		INSTANCE = this;

		RunListener.all().add(new RunListener(Run.class) {
			@Override
			public void onFinalized(Run r) {
				WorkItemAction action = r.getAction(WorkItemAction.class);
				if (action != null) {
					action.buildComplete(r);
				}
				
				for (DroolsProject project: Hudson.getInstance().getItems(DroolsProject.class)) {
					try {
						if (!project.isDisabled()) {
							project.run(new SignalEventCallable(r));
						}
					} catch (Exception e) {
						logger.log(
								Level.WARNING, 
								String.format(
										"Error while sending BuildComplete event for %s to %s", 
										r.getDisplayName(),
										project.getDisplayName()),
								e);
					}
				}

			}
		});

	}

	@Override
	public void stop() throws Exception {
		for (DroolsProject project : Hudson.getInstance().getItems(
				DroolsProject.class)) {
			project.dispose();
		}
	}

	private static PluginImpl INSTANCE;

	public static PluginImpl getInstance() {
		return INSTANCE;
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
	}

}
