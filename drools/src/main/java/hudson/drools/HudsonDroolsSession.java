package hudson.drools;

import hudson.model.Hudson;
import hudson.util.IOException2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

public class HudsonDroolsSession {

	private StatefulKnowledgeSession ksession;
	private KnowledgeBase kbase;
	private Marshaller marshaller;
	public static final boolean SERIALIZATION_PERSISTENCE = true;
	private final File persistenceDir;
	
	public HudsonDroolsSession(File persistenceDir) {
		this.persistenceDir = persistenceDir;
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
	
	private StatefulKnowledgeSession createSession() throws IOException {
		KnowledgeSessionConfiguration conf = new SessionConfiguration();
		Environment env = EnvironmentFactory.newEnvironment();
		File f = new File(persistenceDir, "session.ser");
		StatefulKnowledgeSession ksession = null;
		if (SERIALIZATION_PERSISTENCE) {
			try {
				if (!f.exists()) {
					ksession = kbase.newStatefulKnowledgeSession(conf, env);
				} else {
					FileInputStream fis = new FileInputStream(f);
					ksession = marshaller.unmarshall(fis);
					fis.close();
				}

				WorkItemManager workItemManager = ksession.getWorkItemManager();
				workItemManager.registerWorkItemHandler("Build",
						new BuildWorkItemHandler());
				workItemManager.registerWorkItemHandler("Human Task",
						new HumanTaskHandler());
				workItemManager.registerWorkItemHandler("Script",
						new ScriptHandler());
				workItemManager.registerWorkItemHandler("E-Mail",
						new EmailWorkItemHandler());
			} catch (ClassNotFoundException e) {
				throw new IOException2("Class not found while unmarshalling " + f.getAbsolutePath(),  e);
			}
		} else {
			ksession = kbase.newStatefulKnowledgeSession(conf, env);
		}

		KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
		new WorkingMemoryHudsonLogger(ksession); // TODO !!!

		return ksession;
	}
}
