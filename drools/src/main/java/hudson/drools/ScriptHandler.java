package hudson.drools;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

/**
 * @TODO a way to to manually restart scripts when they fail
 */
public class ScriptHandler implements WorkItemHandler {

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		String scriptName = (String) workItem.getParameter("Script");
		
		DroolsRun run = DroolsRun.getFromProcessInstance(workItem.getProcessInstanceId());
		ScriptExecution execution = new ScriptExecution(run, scriptName, workItem.getId(), workItem.getParameters());
		execution.run();
	}
}
