package hudson.drools;

import java.util.concurrent.Callable;

import org.drools.runtime.StatefulKnowledgeSession;

public class CancelProcessCallable implements Callable<Void> {

	public CancelProcessCallable(StatefulKnowledgeSession session,
			long processInstanceId) {
		super();
		this.session = session;
		this.processInstanceId = processInstanceId;
	}

	private final StatefulKnowledgeSession session;
	private final long processInstanceId;

	public Void call() throws Exception {
		session.abortProcessInstance(processInstanceId);

		return null;
	}

}
