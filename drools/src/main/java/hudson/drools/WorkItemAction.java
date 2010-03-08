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

import org.kohsuke.stapler.HttpResponse;

public class WorkItemAction extends ParametersAction {

	private final static Logger logger = Logger.getLogger(WorkItemAction.class
			.getName());

	private final long workItemId;
	private final String projectName;

	// backward compatibility
	private final long processInstanceId;

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
		Job project = (Job) Hudson.getInstance().getItem(projectName);
		if (project == null) {
			throw new IllegalArgumentException("project " + projectName
					+ " does not exist (work item " + workItemId + ")");
		}
		if (project instanceof AbstractProject) {
			((AbstractProject) project).scheduleBuild(0, new DroolsCause("Started by workflow"), this);
		} else if (project instanceof DroolsProject) {
			((DroolsProject) project).scheduleBuild(new DroolsCause("Started by workflow"), this);
		} else {
			throw new IllegalArgumentException("project " + projectName + " has an unsupported type: " + project.getClass());
		}
	}

	public void buildComplete(Run r) {
		run = r;
		save();
		
		try {
			DroolsProject p = (DroolsProject) Hudson.getInstance().getItem(
					droolsProjectName);
			p.removePendingWorkItemBuild(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

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

	public HttpResponse doRestart() throws ServletException, IOException {
		if (run != null && run.getResult().isWorseOrEqualTo(Result.UNSTABLE)) {
			run.checkPermission(Job.BUILD);
			new WorkItemAction(droolsProjectName, workItemId,
					processInstanceId, projectName, completeWhenFailed,
					completeWhenUnstable, getParameters()).scheduleBuild();
			return new ForwardToPreviousPage();
		} else {
			throw new IllegalArgumentException(
					"Cannot restart a build that did not fail.");
		}
	}

	public HttpResponse doComplete() throws ServletException, IOException {
		if (run == null) {
			throw new IllegalArgumentException(
					"Cannot complete before the build is done");
		}

		run.checkPermission(Job.BUILD);

		complete();

		return new ForwardToPreviousPage();
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
		if (completed) {
			return false;
		}
		
		DroolsRun droolsRun = getDroolsRun();
		if (droolsRun == null || !droolsRun.isRunning()) {
			return false;
		}
		if (run == null) {
			return false;
		}

		return true;
		
	}

	public DroolsRun getDroolsRun() {
		DroolsProject p = (DroolsProject) Hudson.getInstance().getItem(
				droolsProjectName);

		return p != null ? p.getFromProcessInstance(processInstanceId) : null;
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