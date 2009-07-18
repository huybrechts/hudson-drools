package hudson.drools;

import hudson.model.Run;

import org.drools.runtime.StatefulKnowledgeSession;

public class SignalEventCallable implements SessionCallable<Void> {

	private final Run<?, ?> run;

	public Run<?, ?> getRun() {
		return run;
	}

	public SignalEventCallable(Run<?, ?> run) {
		super();
		this.run = run;
	}

	public Void call(StatefulKnowledgeSession session) throws Exception {
		session.signalEvent(
			String.format(
					Constants.BUILD_COMPLETE_EVENT, 
					run.getParent().getName()
			), 
			new RunWrapper(run));
		return null;
	}

}
