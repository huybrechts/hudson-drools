/**
 * 
 */
package hudson.drools;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class DroolsRunListener extends RunListener<Run> {
	private static final Logger logger = Logger.getLogger(Plugin.class
			.getName());

	public DroolsRunListener() {
		super(Run.class);
	}

	@Override
	public void onFinalized(Run r) {
		WorkItemAction action = r.getAction(WorkItemAction.class);
		if (action != null) {
			action.buildComplete(r);
		}

		for (DroolsProject project : Hudson.getInstance().getItems(
				DroolsProject.class)) {
			try {
				if (!project.isDisabled()) {
					project.run(new SignalEventCallable(r));
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, String.format(
						"Error while sending BuildComplete event for %s to %s",
						r.getDisplayName(), project.getDisplayName()), e);
			}
		}

	}
}