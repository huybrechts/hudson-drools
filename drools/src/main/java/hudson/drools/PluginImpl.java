package hudson.drools;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;
import hudson.security.ACL;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SessionConfiguration;
import org.drools.impl.EnvironmentFactory;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItemManager;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class PluginImpl extends Plugin {

	private StatefulKnowledgeSession ksession;

	private KnowledgeBase kbase;

	public static final boolean PERSISTENCE = true;

	@Override
	@SuppressWarnings( { "deprecation", "unchecked" })
	public void start() throws Exception {
		INSTANCE = this;

		RuleFlowRenderer.class.getName(); // initialize this so icons are loaded

		RunListener.all().add(new RunListener(Run.class) {
			@Override
			public void onFinalized(Run r) {
				WorkItemAction action = r.getAction(WorkItemAction.class);
				if (action != null) {
					action.buildComplete(r);
				}

				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				try {
					Thread.currentThread().setContextClassLoader(
							getClass().getClassLoader());
					getSession().signalEvent(
							"BuildComplete:" + r.getParent().getName(),
							new RunWrapper(r));
				} catch (Exception e) {
					// don't fail unrelated builds on this
					e.printStackTrace();
				} finally {
					Thread.currentThread().setContextClassLoader(cl);
				}
			}

		});

		ItemListener.all().add(new ItemListener() {
			@Override
			public void onLoaded() {
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(
						PluginImpl.class.getClassLoader());
				try {

					if (PERSISTENCE) {
						DroolsManagement.getInstance().getDbSettings().start();
					}

					kbase = KnowledgeBaseFactory.newKnowledgeBase();
					ksession = createSession();

					for (DroolsProject p : Hudson.getInstance().getItems(
							DroolsProject.class)) {
						p.updateProcess();
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					Thread.currentThread().setContextClassLoader(cl);
				}
			}
		});
	}

	@Override
	public void stop() throws Exception {
		ksession.dispose();
		if (PERSISTENCE)
			DroolsManagement.getInstance().getDbSettings().stop();
	}

	public KnowledgeBase getKnowledgeBase() {
		return kbase;
	}

	public StatefulKnowledgeSession getSession() {
		return ksession;
	}

	private StatefulKnowledgeSession createSession() {
		KnowledgeSessionConfiguration conf = new SessionConfiguration();
		Environment env = EnvironmentFactory.newEnvironment();
		if (PERSISTENCE) {
			env
					.set(EnvironmentName.ENTITY_MANAGER_FACTORY,
							DroolsManagement.getInstance().getDbSettings()
									.createEntityManagerFactory());
		}
		StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession(
				conf, env);
		KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);

		new WorkingMemoryHudsonLogger(ksession);

		WorkItemManager workItemManager = ksession.getWorkItemManager();
		workItemManager.registerWorkItemHandler("Build", new BuildWorkItemHandler());
		workItemManager.registerWorkItemHandler("Human Task", new HumanTaskHandler());
		workItemManager.registerWorkItemHandler("Script", new ScriptHandler());
		workItemManager.registerWorkItemHandler("E-Mail", new EmailWorkItemHandler());

		return ksession;
	}

	public void completeWorkItem(long workItemId, Map<String, Object> results) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());

			getSession().getWorkItemManager().completeWorkItem(workItemId,
					results);
		} finally {
			SecurityContextHolder.getContext().setAuthentication(auth);
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	private static PluginImpl INSTANCE;

	public static PluginImpl getInstance() {
		return INSTANCE;
	}

	public void doWorkflowProjects(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		PrintWriter pw = new PrintWriter(rsp.getOutputStream());
		for (DroolsProject project : Hudson.getInstance().getItems(
				DroolsProject.class)) {
			pw.println(project.getName());
			System.out.println(project.getName());
		}
		pw.flush();
	}

}
