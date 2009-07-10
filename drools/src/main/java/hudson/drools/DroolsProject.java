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
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.process.Process;
import org.drools.io.impl.ReaderResource;
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
		
		if (PluginImpl.getInstance().getKnowledgeBase() != null) {
			updateProcess();
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

		public String[] getAvailableProcessIds() {
			return new String[] { "agility-ris-release-workflow",
					"asb-trunk-release-workflow" };
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
		processId = updateProcess(processXML);
		this.processXML = processXML;

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

		super.doConfigSubmit(req, rsp);
	}

	private String updateProcess(String processXML) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());
			KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
					.newKnowledgeBuilder(new PackageBuilderConfiguration());
			kbuilder.add(new ReaderResource(new StringReader(processXML)),
					ResourceType.DRF);
			KnowledgeBuilderErrors errors = kbuilder.getErrors();
			StringBuilder sb = new StringBuilder();
			if (errors.size() > 0) {
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

			processId = process.getId();

			PluginImpl.getInstance().getKnowledgeBase().addKnowledgePackages(
					knowledgePackages);

			return processId;
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	public void updateProcess() {
		processId = updateProcess(processXML);
	}

	public void validateWorkflow() {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder(new PackageBuilderConfiguration());
		kbuilder.add(new ReaderResource(new StringReader(processXML)),
				ResourceType.DRF);
		KnowledgeBuilderErrors errors = kbuilder.getErrors();
		if (errors.size() > 0) {
			for (KnowledgeBuilderError error : errors) {
				System.err.println(error);
			}
			throw new IllegalArgumentException("Could not parse knowledge.");
		}
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
		return disabled;
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

	public void doProcessInstanceImage(StaplerRequest req, StaplerResponse rsp)
			throws IOException, XPathExpressionException, DocumentException {
		ServletOutputStream output = rsp.getOutputStream();
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
		processId = updateProcess(processXML);
		this.processXML = processXML;

		save();
	}

	@Override
	protected void performDelete() throws IOException, InterruptedException {
		PluginImpl.getInstance().getKnowledgeBase().removeProcess(processId);
	}

	public List<String> getUsersWithBuildPermission() {
		List<String> result = new ArrayList<String>();
		
		AuthorizationMatrixProperty amp = getProperty(AuthorizationMatrixProperty.class);
		if (amp != null && amp.isUseProjectSecurity()) {
			for (String sid: amp.getAllSIDs()) {
				if (amp.hasPermission(sid, Job.BUILD)) {
					result.add(sid);
				}
			}
		}
		
		return result;
	}

}
