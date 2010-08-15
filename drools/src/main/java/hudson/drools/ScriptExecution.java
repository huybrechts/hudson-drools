package hudson.drools;

import hudson.model.Job;
import hudson.security.ACL;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.acegisecurity.context.SecurityContextHolder;
import org.drools.runtime.StatefulKnowledgeSession;
import org.kohsuke.stapler.HttpResponse;

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
		
		final Class<Script> klazz = run.getParent().getScript(scriptName);

		new Thread(new Runnable() {

			public void run() {
				SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
				result = Result.RUNNING;

				PrintWriter output = run.getLogWriter();
				try {
					Map<String,Object> parameters = new HashMap<String, Object>(ScriptExecution.this.parameters);
					parameters.put("parameters", parameters);
					
					Script script = ConstructionHelper.newInstance(klazz, parameters);
					
					StatefulKnowledgeSession session = run.getParent().getSession().getSession();
					script.setSession(session);
					script.setOutput(output);
					
					output.println("Running script " + scriptName + " with parameters " + ScriptExecution.this.parameters);
					
					Map<String, Object> scriptResults = script.execute();
					
					result = Result.COMPLETED;
					
					run.getParent().run(new CompleteWorkItemCallable(workItemId, scriptResults));

				} catch (Exception e) {
					result = Result.FAILED;
					output.println("Exception while running script " + scriptName);
					e.printStackTrace(output);
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

	public HttpResponse doRun()
			throws ServletException, IOException {
		run.checkPermission(Job.BUILD);
		
		run();

		return new ForwardToPreviousPage();
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
