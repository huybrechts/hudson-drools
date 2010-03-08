package hudson.drools;

import hudson.security.ACL;
import hudson.util.IOException2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SessionConfiguration;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.common.InternalWorkingMemory;
import org.drools.common.InternalWorkingMemoryEntryPoint;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.process.Process;
import org.drools.impl.EnvironmentFactory;
import org.drools.io.impl.ReaderResource;
import org.drools.marshalling.Marshaller;
import org.drools.marshalling.MarshallerFactory;
import org.drools.marshalling.impl.ProcessMarshallerRegistry;
import org.drools.ruleflow.core.RuleFlowProcess;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;

public class DroolsSession {

	private static Logger LOGGER = Logger.getLogger(DroolsSession.class.getName());
	
	private final StatefulKnowledgeSession session;
	private final KnowledgeBase kbase;
	private final Marshaller marshaller;
	private final String processId;
	private File saved;

	public StatefulKnowledgeSession getSession() {
		return session;
	}

	public KnowledgeBase getKbase() {
		return kbase;
	}

	public String getProcessId() {
		return processId;
	}

	public DroolsSession(File saved, String processXML,
			int initialProcessInstanceId) throws IOException {
		this.saved = saved;

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

			throw new IllegalArgumentException("Could not parse knowledge:\n"
					+ sb);
		}

		Collection<KnowledgePackage> knowledgePackages = kbuilder
				.getKnowledgePackages();

		Process process = knowledgePackages.iterator().next().getProcesses()
				.iterator().next();

		processId = process.getId();

		kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(knowledgePackages);

		marshaller = MarshallerFactory.newMarshaller(kbase);

		SessionConfiguration conf = new SessionConfiguration();
		Properties p = new Properties();
		p.setProperty("drools.processInstanceManagerFactory", HudsonProcessInstanceManagerFactory.class.getName());
		conf.addProperties(p);
		Environment env = EnvironmentFactory.newEnvironment();
		if (!saved.exists() || saved.length() == 0) {
			session = kbase.newStatefulKnowledgeSession(conf, env);
		} else {
			InputStream is = null;
			try {
				is = new FileInputStream(saved);
				session = marshaller.unmarshall(is, conf, env);
			} catch (ClassNotFoundException e) {
				throw new IOException2("Class not found while unmarshalling "
						+ saved.getAbsolutePath(), e);
			} catch (IOException e) {
				throw new IOException2("Error while unmarshalling "
						+ saved.getAbsolutePath(), e);
			} finally {
				is.close();
			}
		}

		((HudsonProcessInstanceManager) ((InternalWorkingMemoryEntryPoint) session)
				.getInternalWorkingMemory().getProcessInstanceManager())
				.setProcessCounter(initialProcessInstanceId);
	}

	public synchronized void save() throws IOException {
		OutputStream os = null;
		try {
			File newSaved = new File(saved.getParentFile(), saved.getName()
					+ ".new");
			File backupSaved = new File(saved.getParentFile(), saved.getName()
					+ ".bak");
			os = new FileOutputStream(newSaved);
			marshaller.marshall(os, session);
			os.close();
			os = null;

			if (backupSaved.exists()) {
				if (!backupSaved.delete()) {
					throw new IOException("could not remove backup "
							+ backupSaved.getAbsolutePath());
				}
			}
			if (saved.exists()) {
				if (!saved.renameTo(backupSaved)) {
					throw new IOException("could not backup "
							+ saved.getAbsolutePath());
				}
			}
			if (!newSaved.renameTo(saved)) {
				backupSaved.renameTo(saved);
				throw new IOException("could not rename "
						+ saved.getAbsolutePath());
			}

		} finally {
			if (os != null)
				os.close();
		}
	}

	public void dispose() {
		session.dispose();
	}

	public synchronized <T> T run(SessionCallable<T> callable) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());

			T result = callable.call(session);

			save();

			return result;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while running " + callable + " on " + processId, e);
			throw e;
		} finally {
			SecurityContextHolder.getContext().setAuthentication(auth);
			Thread.currentThread().setContextClassLoader(cl);
		}
	}
}
