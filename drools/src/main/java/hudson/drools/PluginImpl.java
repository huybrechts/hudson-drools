package hudson.drools;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;
import hudson.security.ACL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Callable;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SessionConfiguration;
import org.drools.impl.EnvironmentFactory;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.marshalling.Marshaller;
import org.drools.marshalling.MarshallerFactory;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItemManager;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class PluginImpl extends Plugin {

	private StatefulKnowledgeSession ksession;

	private KnowledgeBase kbase;
	private Marshaller marshaller;

	public static final boolean SERIALIZATION_PERSISTENCE = true;

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

					kbase = KnowledgeBaseFactory.newKnowledgeBase();

					if (SERIALIZATION_PERSISTENCE) {
						marshaller = MarshallerFactory.newMarshaller(kbase);
					}

					for (DroolsProject p : Hudson.getInstance().getItems(
							DroolsProject.class)) {
						p.updateProcess();
					}

					ksession = createSession();

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
		StatefulKnowledgeSession ksession = null;
		if (SERIALIZATION_PERSISTENCE) {
			try {
				File f = new File(getDirectory(), "session.ser");
				if (!f.exists()) {
					ksession = kbase.newStatefulKnowledgeSession(conf, env);
				} else {
					FileInputStream fis = new FileInputStream(f);
					ksession = marshaller.unmarshall(fis);
					fis.close();
				}

				final StatefulKnowledgeSession unwrapped = ksession;

				WorkItemManager workItemManager = ksession.getWorkItemManager();
				workItemManager.registerWorkItemHandler("Build",
						new BuildWorkItemHandler());
				workItemManager.registerWorkItemHandler("Human Task",
						new HumanTaskHandler());
				workItemManager.registerWorkItemHandler("Script",
						new ScriptHandler());
				workItemManager.registerWorkItemHandler("E-Mail",
						new EmailWorkItemHandler());

				// DefaultCommandService commandService = new
				// DefaultCommandService((StatefulSession)
				// ((StatefulKnowledgeSessionImpl) ksession).session) {
				// public synchronized <T extends Object> T
				// execute(org.drools.process.command.Command<T> command) {
				// try {
				// return super.execute(command);
				// } finally {
				// try {
				// saveSession(unwrapped);
				// } catch (IOException e) {
				// throw new RuntimeException(e);
				// }
				// }
				// };
				// };
				// ksession = new
				// CommandBasedStatefulKnowledgeSession(commandService);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			ksession = kbase.newStatefulKnowledgeSession(conf, env);
		}

		KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
		new WorkingMemoryHudsonLogger(ksession);

		return ksession;
	}

	public void saveSession(StatefulKnowledgeSession ksession)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(getDirectory(),
				"session.ser"));
		marshaller.marshall(fos, ksession);
		fos.close();
	}

	public <T> T run(Callable<T> callable) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		try {
			SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());

			T result = callable.call();
			
			saveSession(getSession());
			
			return result;
		} finally {
			SecurityContextHolder.getContext().setAuthentication(auth);
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	private static PluginImpl INSTANCE;

	public static PluginImpl getInstance() {
		return INSTANCE;
	}

	public File getDirectory() {
		return new File(Hudson.getInstance().getRootDir(), "drools");
	}

	public void doWorkflowProjects(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		PrintWriter pw = new PrintWriter(rsp.getOutputStream());
		for (DroolsProject project : Hudson.getInstance().getItems(
				DroolsProject.class)) {
			pw.println(project.getName());
		}
		pw.flush();
	}

}
