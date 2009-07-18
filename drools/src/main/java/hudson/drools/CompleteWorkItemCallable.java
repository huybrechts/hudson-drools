package hudson.drools;

import hudson.model.Run;

import java.util.HashMap;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItemManager;

public class CompleteWorkItemCallable implements SessionCallable<Void> {

	private final Map<String, Object> results;
	private long workItemId;


	public CompleteWorkItemCallable(
			long workItemId, Run<?, ?> run) {
		this.workItemId = workItemId;

		results = new HashMap<String, Object>();
		results.put(Constants.BUILD, new RunWrapper(run));
	}

	public CompleteWorkItemCallable(
			long workItemId, Map<String, Object> results) {
		this.workItemId = workItemId;
		this.results = results;

	}

	public Void call(StatefulKnowledgeSession session) throws Exception {
		WorkItemManager workItemManager = session.getWorkItemManager();
		workItemManager.completeWorkItem(workItemId, results);

		return null;
	}

}
