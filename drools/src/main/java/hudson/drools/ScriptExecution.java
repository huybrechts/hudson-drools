package hudson.drools;

import hudson.model.Job;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ScriptExecution {

	private String scriptName;
	private Map<String, Object> parameters;
	private Result result = Result.NOT_RUN;
	private final long workItemId;
	private final DroolsRun run;

	public enum Result {
		NOT_RUN("Not Run"), RUNNING("Running"), FAILED("Failed"), COMPLETED(
				"Completed");
		private String displayName;

		Result(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String toString() {
			return displayName;
		}
	};

	public ScriptExecution(DroolsRun run, String scriptName, long workItemId,
			Map<String, Object> parameters) {
		super();
		this.run = run;
		this.scriptName = scriptName;
		this.workItemId = workItemId;
		this.parameters = parameters;
		run.addScriptExecution(this);
		try {
			run.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getId() {
		return scriptName;
	}

	public void setId(String id) {
		this.scriptName = id;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public void run() {
		final Script script = DroolsManagement.getInstance().getScript(
				scriptName);
		if (script == null) {
			throw new IllegalArgumentException("Unknown script " + scriptName);
		}

		new Thread(new Runnable() {

			public void run() {
				result = Result.RUNNING;

				try {
					Map<String, Object> scriptResults = script.execute(run
							.getLogWriter(), parameters);
					result = Result.COMPLETED;

					PluginImpl.getInstance().completeWorkItem(workItemId,
							scriptResults);

				} catch (Exception e) {
					result = Result.FAILED;
					run.getLogWriter().println("Exception while running script " + scriptName);
					e.printStackTrace(run.getLogWriter());
				} finally {
					try {
						run.save();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}

		}).start();
	}

	public void doRun(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException {
		run.checkPermission(Job.BUILD);
		
		run();

		rsp.forwardToPreviousPage(req);
	}

	public String getUrl() {
		return run.getUrl() + "scriptExecution/" + workItemId;
	}

	public Result getResult() {
		return result;
	}

	public boolean hasFailed() {
		return result == Result.FAILED;
	}

	public DroolsRun getRun() {
		return run;
	}

	public String getScriptName() {
		return scriptName;
	}

	public long getWorkItemId() {
		return workItemId;
	}
}
