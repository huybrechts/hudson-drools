package hudson.drools;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildableItem;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.ResourceList;
import hudson.model.RunMap;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.Cause.UserCause;
import hudson.model.Queue.Executable;
import hudson.model.RunMap.Constructor;
import hudson.scheduler.CronTabList;
import hudson.security.AuthorizationMatrixProperty;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpressionException;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemManager;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import antlr.ANTLRException;

public class DroolsProject extends Job<DroolsProject, DroolsRun> implements
		TopLevelItem, hudson.model.Queue.Task, BuildableItem {

	private boolean disabled;
	private String processXML;

	private transient DroolsSession session;

	private String triggerSpec;
	private transient CronTabList tabs;

	private List<Script> scripts = new ArrayList<Script>();

	/**
	 * All the builds keyed by their build number.
	 */
	protected transient/* almost final */RunMap<DroolsRun> builds = new RunMap<DroolsRun>();

	protected DroolsProject(ItemGroup<?> parent, String name) {
		super(parent, name);
	}

	@Override
	protected SortedMap<Integer, ? extends DroolsRun> _getRuns() {
		return builds.getView();
	}

	@Override
	public boolean isBuildable() {
		return true;
	}

	@Override
	protected void removeRun(DroolsRun run) {
		this.builds.remove(run);
	}

	@Override
	public void onLoad(ItemGroup<? extends Item> parent, String name)
			throws IOException {
		super.onLoad(parent, name);

		this.builds = new RunMap<DroolsRun>();
		this.builds.load(this, new Constructor<DroolsRun>() {
			public DroolsRun create(File dir) throws IOException {
				DroolsRun newBuild = new DroolsRun(DroolsProject.this, dir);
				builds.put(newBuild);
				return newBuild;
			}
		});

		try {
			set(triggerSpec, processXML, scripts);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	void set(String triggerSpec, String processXML, List<Script> scripts)
			throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				PluginImpl.class.getClassLoader());
		try {
			DroolsSession session = processXML != null ? new DroolsSession(
					new File(getRootDir(), "session.ser"), processXML) : null;

			CronTabList tabs = null;
			if (!StringUtils.isEmpty(triggerSpec)) {
				try {
					tabs = CronTabList.create(triggerSpec);
				} catch (ANTLRException e) {
					e.printStackTrace();
				}
			} else {
				tabs = null;
				triggerSpec = null;
			}

			// all is well -- let's commit
			this.processXML = processXML;
			this.session = session;
			this.triggerSpec = triggerSpec;
			this.tabs = tabs;
			this.scripts = scripts != null ? scripts : new ArrayList<Script>();

			WorkItemManager workItemManager = session.getSession()
					.getWorkItemManager();
			workItemManager.registerWorkItemHandler("Build",
					new BuildWorkItemHandler(this));
			workItemManager.registerWorkItemHandler("Human Task",
					new HumanTaskHandler(this));
			workItemManager.registerWorkItemHandler("Script",
					new ScriptHandler(this));
			workItemManager.registerWorkItemHandler("E-Mail",
					new EmailWorkItemHandler());

			KnowledgeRuntimeLoggerFactory
					.newConsoleLogger(session.getSession());
			new WorkingMemoryHudsonLogger(session.getSession(), this);

		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	@Extension
	public static final class DescriptorImpl extends TopLevelItemDescriptor {

		@Override
		public String getDisplayName() {
			return "Drools Project";
		}

		@Override
		public DroolsProject newInstance(String name) {
			return new DroolsProject(Hudson.getInstance(), name);
		}

	}

	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) Hudson.getInstance().getDescriptor(
				DroolsProject.class);
	}

	@Override
	public Hudson getParent() {
		return Hudson.getInstance();
	}

	@Override
	public synchronized void doConfigSubmit(StaplerRequest req,
			StaplerResponse rsp) throws IOException, ServletException {
		checkPermission(CONFIGURE);

		JSONObject form = req.getSubmittedForm();
		String processXML = form.getString("processXML");
		String triggerSpec = form.getString("triggerSpec");
		List<Script> scripts = req.bindJSONToList(Script.class, form
				.get("scripts"));

		set(triggerSpec, processXML, scripts);

		super.doConfigSubmit(req, rsp);
	}

	/**
	 * Schedules a new build command.
	 */
	public void doBuild(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
		checkPermission(BUILD);

		Cause cause = new UserCause();

		scheduleBuild(cause);

		rsp.forwardToPreviousPage(req);
	}

	public boolean scheduleBuild(Cause cause, Action... actions) {
		if (isDisabled())
			return false;

		List<Action> queueActions = new ArrayList(Arrays.asList(actions));
		if (cause != null) {
			queueActions.add(new CauseAction(cause));
		}

		return Hudson.getInstance().getQueue().add(this, 0,
				queueActions.toArray(new Action[queueActions.size()]));
	}

	public boolean isDisabled() {
		return session == null || disabled;
	}

	public void setDisabled(boolean disable) {
		this.disabled = disable;
	}

	public void checkAbortPermission() {
		checkPermission(AbstractProject.ABORT);
	}

	public Executable createExecutable() throws IOException {
		DroolsRun run = new DroolsRun(this);
		builds.put(run);
		return run;
	}

	public Label getAssignedLabel() {
		return null;
	}

	public long getEstimatedDuration() {
		return -1;
	}

	public Node getLastBuiltOn() {
		return null;
	}

	public String getWhyBlocked() {
		return null;
	}

	public boolean hasAbortPermission() {
		return false;
	}

	public boolean isBuildBlocked() {
		return false;
	}

	public ResourceList getResourceList() {
		return new ResourceList();
	}

	public String getProcessId() {
		return session.getProcessId();
	}

	private transient WeakReference<RuleFlowRenderer> renderer;

	public synchronized RuleFlowRenderer getRuleFlowRenderer() {
		if (renderer == null || renderer.get() == null) {
			renderer = new WeakReference<RuleFlowRenderer>(
					new RuleFlowRenderer(processXML));
		}
		return renderer.get();
	}

	public void doProcessImage(StaplerRequest req, StaplerResponse rsp)
			throws IOException, XPathExpressionException, DocumentException {
		ServletOutputStream output = rsp.getOutputStream();
		rsp.setContentType("image/png");
		getRuleFlowRenderer().write(output);
		output.flush();
		output.close();
	}

	@Exported
	public String getProcessXML() {
		return processXML;
	}

	@Exported
	public String getTriggerSpec() {
		return triggerSpec;
	}

	public boolean scheduleBuild() {
		return scheduleBuild(null, new Action[0]);
	}

	public boolean scheduleBuild(Cause c) {
		return scheduleBuild(c, new Action[0]);
	}

	public boolean scheduleBuild(int quietPeriod) {
		return scheduleBuild(null, new Action[0]);
	}

	public boolean scheduleBuild(int quietPeriod, Cause c) {
		return scheduleBuild(null, new CauseAction(c));
	}

	public CronTabList getTabs() {
		return tabs;
	}

	public void doSubmitWorkflow(StaplerRequest request, StaplerResponse rsp)
			throws IOException {
		checkPermission(CONFIGURE);

		if (!"POST".equals(request.getMethod())) {
			rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"POST expected");
			return;
		}

		String processXML = IOUtils.toString(request.getInputStream());
		set(triggerSpec, processXML, scripts);

		save();
	}

	@Override
	protected void performDelete() throws IOException, InterruptedException {
		if (session != null)
			session.dispose();
		super.performDelete();
	}

	public List<String> getUsersWithBuildPermission() {
		List<String> result = new ArrayList<String>();

		AuthorizationMatrixProperty amp = getProperty(AuthorizationMatrixProperty.class);
		if (amp != null && amp.isUseProjectSecurity()) {
			for (String sid : amp.getAllSIDs()) {
				if (amp.hasPermission(sid, Job.BUILD)) {
					result.add(sid);
				}
			}
		}

		return result;
	}

	/*
	 * We need two strategies two find the DroolsRun. When the process is
	 * starting, the DroolsRun does not know its processInstanceId yet, so we
	 * query the process variable "run".
	 * 
	 * After the process is completed, the processInstance or variable will be
	 * gone, so we need to iterate over all the builds to find the right one.
	 */
	public DroolsRun getFromProcessInstance(long processInstanceId) {
		DroolsRun result = null;
		ProcessInstance processInstance = session.getSession()
				.getProcessInstance(processInstanceId);
		if (processInstance != null) {
			result = DroolsRun.getFromProcessInstance(processInstance);
		}
		if (result == null) {
			// probably because the workflow has been completed
			for (DroolsRun run : getBuilds()) {
				if (run.getProcessInstanceId() == processInstanceId) {
					return run;
				}
			}
		}
		return result;
	}

	public <T> T run(SessionCallable<T> callable) throws Exception {
		return session.run(callable);
	}

	public void dispose() {
		session.dispose();
		for (DroolsRun run : getBuilds()) {
			run.dispose();
		}
	}

	public List<Script> getScripts() {
		return scripts;
	}

	public void setScripts(List<Script> scripts) {
		this.scripts = new ArrayList<Script>(scripts);
	}

	public void setScripts(Script... scripts) {
		this.scripts = new ArrayList<Script>(Arrays.asList(scripts));
	}

	public Script getScript(String scriptName) {
		for (Script script : scripts) {
			if (script.getId().equals(scriptName)) {
				return script;
			}
		}
		return DroolsManagement.getInstance().getScript(scriptName);
	}

	public DroolsSession getSession() {
		return session;
	}

}
