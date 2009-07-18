package hudson.drools;

import org.drools.runtime.StatefulKnowledgeSession;


public class CancelProcessCallable implements SessionCallable<Void> {

	public CancelProcessCallable(
			long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	private final long processInstanceId;

	public Void call(StatefulKnowledgeSession session) throws Exception {
		session.abortProcessInstance(processInstanceId);

		return null;
	}

}
