package hudson.drools;

import hudson.Functions;
import hudson.model.BallColor;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.xml.xpath.XPathExpressionException;

import org.dom4j.DocumentException;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class DroolsRun extends Run<DroolsProject, DroolsRun> implements
		Queue.Executable {

	private static Logger logger = Logger.getLogger(DroolsRun.class.getName());

	private List<HumanTask> humanTasks;
	private List<ScriptExecution> scriptExecutions;
	private List<NodeInstanceLog> logs;

	private transient PrintWriter logWriter;

	private long processInstanceId;

	private String processXML;

	private Object readResolve() {
		for (HumanTask task : humanTasks) {
			task.setRun(this);
		}
		return this;
	}

	protected DroolsRun(DroolsProject project) throws IOException {
		super(project);
		processXML = project.getProcessXML();
		humanTasks = new CopyOnWriteArrayList<HumanTask>();
		status = Status.STARTED;
		scriptExecutions = new CopyOnWriteArrayList<ScriptExecution>();
		logs = new CopyOnWriteArrayList<NodeInstanceLog>();
	}

	public DroolsRun(DroolsProject project, File dir) throws IOException {
		super(project, dir);
	}

	public List<HumanTask> getHumanTasks() {
		return humanTasks;
	}

	public void addHumanTask(HumanTask humanTask) {
		humanTasks.add(humanTask);
		humanTask.setRun(this);
		try {
			save();
		} catch (IOException e) {
			throw new RuntimeException("Could not save!", e);
		}
	}

	public HumanTask getHumanTask(int workItemId) {
		for (HumanTask task : humanTasks) {
			if (workItemId == task.getWorkItemId()) {
				return task;
			}
		}
		return null;
	}

	public ProcessInstance getProcessInstance() {
		return PluginImpl.getInstance().getSession().getProcessInstance(
				processInstanceId);
	}

	/*
	 * We need two strategies two find the DroolsRun. When the process is
	 * starting, the DroolsRun does not now it processInstanceId yet, so we
	 * query the process variable "run".
	 * 
	 * After the process is completed, the processInstance or variable will be
	 * gone, so we need to iterate over all the builds to find the right one.
	 */
	public static DroolsRun getFromProcessInstance(long processInstanceId) {
		DroolsRun result = null;
		ProcessInstance processInstance = PluginImpl.getInstance().getSession()
				.getProcessInstance(processInstanceId);
		if (processInstance != null) {
			result = getFromProcessInstance(processInstance);
		}
		if (result == null) {
			// probably because the workflow has been completed
			for (Item item : Hudson.getInstance().getItemMap().values()) {
				if (item instanceof DroolsProject)
					for (DroolsRun run : ((DroolsProject) item).getBuilds()) {
						if (run.getProcessInstanceId() == processInstanceId) {
							return run;
						}
					}
			}
		}
		return result;
	}

	public static DroolsRun getFromProcessInstance(
			ProcessInstance processInstance) {
		RunWrapper wrapper = (RunWrapper) ((WorkflowProcessInstance) processInstance)
				.getVariable(Constants.RUN);
		if (wrapper == null) {
			return null;
		}
		DroolsRun run = (DroolsRun) wrapper.getRun();
		return run;
	}

	public List<NodeInstanceLog> getLogs() {
		return logs;
	}

	public void run() {
		run(new RunnerImpl());
	}

	protected class RunnerImpl implements Runner {

		public void cleanUp(BuildListener listener) throws Exception {
		}

		public void post(BuildListener listener) throws Exception {
		}

		public Result run(BuildListener listener) throws Exception,
				hudson.model.Run.RunnerAbortedException {

			ProcessInstance instance = PluginImpl.getInstance().run(
					new StartProcessCallable(DroolsRun.this, PluginImpl
							.getInstance().getSession(), getParent()
							.getProcessId()));

			if (instance != null
					&& instance.getState() != ProcessInstance.STATE_ABORTED) {
				return Result.SUCCESS;
			} else {
				return Result.FAILURE;
			}
		}

	}

	public String getUpUrl() {
		return Functions.getNearestAncestorUrl(Stapler.getCurrentRequest(),
				getParent()) + '/';
	}

	public synchronized RuleFlowRenderer getRuleFlowRenderer() {
		return new RuleFlowRenderer(processXML, getLogs());
	}

	public void doProcessInstanceImage(StaplerRequest req, StaplerResponse rsp)
			throws IOException, XPathExpressionException, DocumentException {
		ServletOutputStream output = rsp.getOutputStream();
		getRuleFlowRenderer().write(output);
		output.flush();
		output.close();
	}

	public long getProcessInstanceId() {
		return processInstanceId;
	}

	public void addScriptExecution(ScriptExecution execution) {
		scriptExecutions.add(execution);
	}

	public ScriptExecution getScriptExecution(int workItemId) {
		for (ScriptExecution execution : scriptExecutions) {
			if (execution.getWorkItemId() == workItemId) {
				return execution;
			}
		}
		return null;
	}

	public List<ScriptExecution> getScriptExecutions() {
		return scriptExecutions;
	}

	public enum Status {
		STARTED, COMPLETED, ABORTED
	}

	private Status status;

	public Status getStatus() {
		return status;
	}

	public boolean isCompleted() {
		return status == Status.COMPLETED;
	}

	public boolean isAborted() {
		return status == Status.ABORTED;
	}

	public boolean isRunning() {
		return status == Status.STARTED;
	}

	@Override
	public BallColor getIconColor() {
		if (status == Status.STARTED) {
			return BallColor.BLUE_ANIME;
		} else if (status == Status.ABORTED) {
			return BallColor.GREY;
		} else {
			return BallColor.BLUE;
		}
	}

	public synchronized void markCompleted() {
		setStatus(Status.COMPLETED);
	}

	public synchronized void markAborted() {
		setStatus(Status.ABORTED);
	}

	private synchronized void setStatus(Status status) {
		this.status = status;
		PrintWriter logWriter = getLogWriter();
		try {
			save();
		} catch (IOException e) {
			e.printStackTrace(logWriter);
		}
		logWriter.close();
	}

	public void addLog(NodeInstanceLog log) {
		logs.add(log);
		try {
			save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized PrintWriter getLogWriter() {
		if (logWriter == null) {
			try {
				logWriter = new PrintWriter(new FileWriter(getLogFile(), true),
						true);
			} catch (IOException e) {
				throw new RuntimeException(
						"Error opening log file for reading", e);
			}
		}
		return logWriter;
	}

	public void doDoCancel(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException {
		checkPermission(Job.BUILD);

		try {
			PluginImpl.getInstance().run(
					new CancelProcessCallable(PluginImpl.getInstance()
							.getSession(), processInstanceId));
		} catch (Exception e) {
			throw new ServletException(
					"Error while canceling process instance #"
							+ processInstanceId, e);
		}

		// TODO check if we get this through events already ?
		setStatus(Status.ABORTED);

		rsp.sendRedirect2(req.getContextPath() + '/' + getUrl());
	}

}
