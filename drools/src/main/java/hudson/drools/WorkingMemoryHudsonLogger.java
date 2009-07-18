package hudson.drools;


import org.drools.audit.WorkingMemoryLogger;
import org.drools.audit.event.LogEvent;
import org.drools.audit.event.RuleFlowLogEvent;
import org.drools.audit.event.RuleFlowNodeLogEvent;
import org.drools.event.KnowledgeRuntimeEventManager;


public class WorkingMemoryHudsonLogger extends WorkingMemoryLogger {
    
    private final DroolsProject project;

    public WorkingMemoryHudsonLogger(KnowledgeRuntimeEventManager session, DroolsProject project) {
    	super(session);
		this.project = project;
    }

    public void logEventCreated(LogEvent logEvent) {
        switch (logEvent.getType()) {
            case LogEvent.BEFORE_RULEFLOW_CREATED:
                RuleFlowLogEvent processEvent = (RuleFlowLogEvent) logEvent;
                addProcessLog(processEvent.getProcessInstanceId(), processEvent.getProcessId());
                break;
            case LogEvent.BEFORE_RULEFLOW_COMPLETED:
            	processEvent = (RuleFlowLogEvent) logEvent;
                processCompleted(processEvent.getProcessInstanceId());
                break;
            case LogEvent.BEFORE_RULEFLOW_NODE_TRIGGERED:
            	RuleFlowNodeLogEvent nodeEvent = (RuleFlowNodeLogEvent) logEvent;
            	addNodeEnterLog(nodeEvent.getProcessInstanceId(), nodeEvent.getProcessId(), nodeEvent.getNodeInstanceId(), nodeEvent.getNodeId());
                break;
            case LogEvent.BEFORE_RULEFLOW_NODE_EXITED:
            	nodeEvent = (RuleFlowNodeLogEvent) logEvent;
            	addNodeExitLog(nodeEvent.getProcessInstanceId(), nodeEvent.getProcessId(), nodeEvent.getNodeInstanceId(), nodeEvent.getNodeId());
                break;
            default:
                // ignore all other events
        }
        
        if (logEvent instanceof RuleFlowLogEvent) {
        	long processInstanceId = ((RuleFlowLogEvent) logEvent).getProcessInstanceId();
        	DroolsRun processInstance = project.getFromProcessInstance(processInstanceId);
        	if (processInstance != null) {
        		processInstance.getLogWriter().println(logEvent);
        	}
        }
        
    }

    private void addProcessLog(long processInstanceId, String processId) {
    }
    
    private void processCompleted(long processInstanceId) {
		DroolsRun run = project.getFromProcessInstance(processInstanceId);
		if (run != null) {
			run.markCompleted();
		}
    }
    
    private void addNodeEnterLog(long processInstanceId, String processId, String nodeInstanceId, String nodeId) {
		DroolsRun run = project.getFromProcessInstance(processInstanceId);
		if (run != null)
		run.addLog(new NodeInstanceLog(NodeInstanceLog.TYPE_ENTER,
				processInstanceId, processId, nodeInstanceId,
				nodeId));
    }
    
    private void addNodeExitLog(long processInstanceId, String processId, String nodeInstanceId, String nodeId) {
		DroolsRun run = project.getFromProcessInstance(processInstanceId);
		if (run != null)
		run.addLog(new NodeInstanceLog(NodeInstanceLog.TYPE_EXIT,
				processInstanceId, processId, nodeInstanceId,
				nodeId));
    }
    
    public void dispose() {
	}

}
