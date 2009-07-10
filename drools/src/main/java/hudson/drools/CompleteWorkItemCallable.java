package hudson.drools;

import hudson.model.Run;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItemManager;

public class CompleteWorkItemCallable implements Callable<Void> {

	private final Map<String, Object> results;
	private StatefulKnowledgeSession session;
	private long workItemId;


	public CompleteWorkItemCallable(StatefulKnowledgeSession session,
			long workItemId, Run<?, ?> run) {
		super();
		this.session = session;
		this.workItemId = workItemId;

		results = new HashMap<String, Object>();
		results.put(Constants.BUILD, new RunWrapper(run));
	}

	public CompleteWorkItemCallable(StatefulKnowledgeSession session,
			long workItemId, Map<String, Object> results) {
		super();
		this.session = session;
		this.workItemId = workItemId;
		this.results = results;

	}

	public Void call() throws Exception {
		WorkItemManager workItemManager = session.getWorkItemManager();
		workItemManager.completeWorkItem(workItemId, results);

		return null;
	}

}
