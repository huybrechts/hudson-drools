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
import hudson.security.ACL;
import hudson.security.AuthorizationMatrixProperty;
import hudson.util.IOException2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpressionException;

import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SessionConfiguration;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.process.Process;
import org.drools.impl.EnvironmentFactory;
import org.drools.io.impl.ReaderResource;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.marshalling.Marshaller;
import org.drools.marshalling.MarshallerFactory;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemManager;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import antlr.ANTLRException;

public class DroolsProject extends Job<DroolsProject, DroolsRun> implements
		TopLevelItem, hudson.model.Queue.Task, BuildableItem {

	private boolean disabled;
	private transient String processId;
	private String processXML;

	private String triggerSpec;
	private transient CronTabList tabs;

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
		if (triggerSpec != null) {
			try {
				tabs = CronTabList.create(triggerSpec);
			} catch (ANTLRException e) {
				e.printStackTrace();
			}
		}

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				PluginImpl.class.getClassLoader());
		try {
			kbase = KnowledgeBaseFactory.newKnowledgeBase();
			marshaller = MarshallerFactory.newMarshaller(kbase);

			if (processXML != null) {
				updateProcess(processXML);
				session = createSession();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
		;

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
		return (DescriptorImpl) Hudson.getInstance().getDescriptor(DroolsProject.class);
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
		updateProcess(processXML);

		triggerSpec = form.getString("triggerSpec");
		if (!StringUtils.isEmpty(triggerSpec)) {
			try {
				tabs = CronTabList.create(triggerSpec);
			} catch (ANTLRException e) {
				e.printStackTrace();
			}
		} else {
			tabs = null;
			this.triggerSpec = null;
		}
		
		session = createSession();

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
		return processId;
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

	public void setProcessId(String processId) {
		this.processId = processId;
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
		updateProcess(processXML);

		save();
	}

	@Override
	protected void performDelete() throws IOException, InterruptedException {
		if (session != null) session.dispose();
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
		ProcessInstance processInstance = getSession().getProcessInstance(
				processInstanceId);
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
		synchronized(session) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		try {
			SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());

			T result = callable.call(session);

			saveSession();

			return result;
		} finally {
			SecurityContextHolder.getContext().setAuthentication(auth);
			Thread.currentThread().setContextClassLoader(cl);
		}
		}
	}

	public static <T> T run(Callable<T> callable) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		try {
			SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
			Thread.currentThread().setContextClassLoader(
					PluginImpl.class.getClassLoader());

			T result = callable.call();

			return result;
		} finally {
			SecurityContextHolder.getContext().setAuthentication(auth);
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	private transient StatefulKnowledgeSession session;
	private transient KnowledgeBase kbase;
	private transient Marshaller marshaller;

	public void updateProcess(final String processXML) {
		try {
			run(new Callable<Void>() {

				public Void call() throws Exception {
					KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
							.newKnowledgeBuilder(new PackageBuilderConfiguration());
					kbuilder.add(new ReaderResource(
							new StringReader(processXML)), ResourceType.DRF);
					KnowledgeBuilderErrors errors = kbuilder.getErrors();
					StringBuilder sb = new StringBuilder();
					if (errors.size() > 0) {
						setDisabled(true);
						
						for (KnowledgeBuilderError error : errors) {
							sb.append(error.getMessage()).append("\n");
						}
						
						throw new IllegalArgumentException(
								"Could not parse knowledge:\n" + sb);
					}

					Collection<KnowledgePackage> knowledgePackages = kbuilder
							.getKnowledgePackages();

					Process process = knowledgePackages.iterator().next()
							.getProcesses().iterator().next();

					String processId = process.getId();

					if (kbase == null) {
						kbase = KnowledgeBaseFactory.newKnowledgeBase();
						marshaller = MarshallerFactory.newMarshaller(kbase);
					}

					kbase.addKnowledgePackages(knowledgePackages);

					DroolsProject.this.processId = processId;
					DroolsProject.this.processXML = processXML;
					
					session = createSession();
					
					return null;
				}

			});
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception", e);
		}
	}

	private StatefulKnowledgeSession createSession() throws IOException {
		KnowledgeSessionConfiguration conf = new SessionConfiguration();
		Environment env = EnvironmentFactory.newEnvironment();
		File f = new File(getRootDir(), "session.ser");
		StatefulKnowledgeSession ksession = null;
		if (!f.exists() || f.length() == 0) {
			ksession = kbase.newStatefulKnowledgeSession(conf, env);
		} else {
			InputStream is = null;
			try {
				is = new FileInputStream(f);
				ksession = marshaller.unmarshall(is);
			} catch (ClassNotFoundException e) {
				throw new IOException2("Class not found while unmarshalling "
						+ f.getAbsolutePath(), e);
			} catch (IOException e) {
				throw new IOException2("Error while unmarshalling "
						+ f.getAbsolutePath(), e);
			} finally {
				is.close();
			}
		}

		WorkItemManager workItemManager = ksession.getWorkItemManager();
		workItemManager.registerWorkItemHandler("Build",
				new BuildWorkItemHandler(this));
		workItemManager.registerWorkItemHandler("Human Task",
				new HumanTaskHandler(this));
		workItemManager.registerWorkItemHandler("Script", new ScriptHandler(
				this));
		workItemManager.registerWorkItemHandler("E-Mail",
				new EmailWorkItemHandler());

		KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
		new WorkingMemoryHudsonLogger(ksession, this);

		return ksession;
	}

	public void saveSession() throws IOException {
		if (session != null) {
			OutputStream os = null;
			try {
				os = new FileOutputStream(new File(this.getRootDir(),
						"session.ser"));
				marshaller.marshall(os, session);
			} finally {
				os.close();
			}
		}
	}

	public void dispose() {
		session.dispose();
		for (DroolsRun run: getBuilds()) {
			run.dispose();
		}
	}

	public StatefulKnowledgeSession getSession() {
		return session;
	}

}
