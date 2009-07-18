/**
 * 
 */
package hudson.drools;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class WorkItemAction extends ParametersAction {

	private final static Logger logger = Logger.getLogger(WorkItemAction.class
			.getName());

	private final long workItemId;
	private final long processInstanceId;
	private final String projectName;

	private Run<?, ?> run;

	private final boolean completeWhenUnstable;
	private final boolean completeWhenFailed;

	private final String droolsProjectName;

	private boolean completed = false;

	public WorkItemAction(String droolsProjectName, long workItemId,
			long processInstanceId, String projectName,
			boolean completeWhenFailed, boolean completeWhenUnstable,
			List<ParameterValue> parameters) {
		super(parameters);
		this.droolsProjectName = droolsProjectName;
		this.workItemId = workItemId;
		this.processInstanceId = processInstanceId;
		this.projectName = projectName;
		this.completeWhenUnstable = completeWhenUnstable;
		this.completeWhenFailed = completeWhenFailed;
	}

	public long getWorkItemId() {
		return workItemId;
	}

	public long getProcessInstanceId() {
		return processInstanceId;
	}

	public String getProjectName() {
		return projectName;
	}

	public void scheduleBuild() {
		AbstractProject project = (AbstractProject) Hudson.getInstance()
				.getItem(projectName);
		if (project == null) {
			throw new IllegalArgumentException("project " + projectName
					+ " does not exist (work item " + workItemId + ")");
		}
		project.scheduleBuild(0, new DroolsCause("Started by workflow"), this);
	}

	public void buildComplete(Run r) {
		run = r;
		save();

		// TODO add logging when this happens
		if (!completeWhenUnstable && r.getResult() == Result.UNSTABLE) {
			return;
		}
		if (!completeWhenFailed
				&& r.getResult().isWorseOrEqualTo(Result.FAILURE)) {
			return;
		}

		complete();

	}

	private void complete() {

		DroolsProject p = (DroolsProject) Hudson.getInstance().getItem(
				droolsProjectName);

		try {
			p.run(new CompleteWorkItemCallable(workItemId, run));

			completed = true;

			save();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error while finalizing "
					+ run.getDisplayName() + " and completing WorkItem "
					+ workItemId, e);
		}
	}

	private void save() {
		try {
			run.save();
		} catch (IOException e) {
			logger.log(Level.WARNING, "error while saving run", e);
		}
	}

	public void doRestart(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException {
		run.checkPermission(Job.BUILD);

		if (run != null && run.getResult().isWorseOrEqualTo(Result.UNSTABLE)) {
			new WorkItemAction(droolsProjectName, workItemId,
					processInstanceId, projectName, completeWhenFailed,
					completeWhenUnstable, getParameters()).scheduleBuild();
		} else {
			throw new IllegalArgumentException(
					"Cannot restart a build that did not fail.");
		}

		rsp.forwardToPreviousPage(req);
	}

	public void doComplete(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException {
		run.checkPermission(Job.BUILD);
		if (run == null) {
			throw new IllegalArgumentException(
					"Cannot complete before the build is done");
		}

		complete();

		rsp.forwardToPreviousPage(req);
	}

	@Override
	public String getDisplayName() {
		return "Work Item";
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getUrlName() {
		return "workItem";
	}

	public Run<?, ?> getRun() {
		return run;
	}

	public void setRun(Run<?, ?> run) {
		this.run = run;
	}

	public boolean isAllowRestart() {
		DroolsRun droolsRun = getDroolsRun();
		if (droolsRun == null || !droolsRun.isRunning()) {
			return false;
		}
		if (run == null) {
			return false;
		}
		if (run.getResult() == Result.UNSTABLE) {
			return !completeWhenUnstable;
		}
		if (run.getResult().isWorseOrEqualTo(Result.FAILURE)) {
			return !completeWhenFailed;
		}

		return false;
	}

	public boolean isAllowComplete() {
		DroolsRun droolsRun = getDroolsRun();
		return !completed && run != null && droolsRun != null
				&& !droolsRun.isCompleted();
	}

	public DroolsRun getDroolsRun() {
		for (DroolsProject project : Hudson.getInstance().getItems(
				DroolsProject.class)) {
			for (DroolsRun run : project.getBuilds()) {
				if (run.getProcessInstanceId() == processInstanceId) {
					return run;
				}
			}
		}
		return null;
	}

	public static Run findRun(Job<?, ?> project, long processInstanceId) {
		for (Run run : project.getBuilds()) {
			WorkItemAction w = run.getAction(WorkItemAction.class);
			if (w != null && w.processInstanceId == processInstanceId) {
				return run;
			}
		}
		return null;
	}

	public String getUrl() {
		return getRun().getUrl() + "/workItem";
	}

	public boolean shouldSchedule(List<Action> actions) {
		return true;
	}
}