package hudson.drools;

import static hudson.drools.Constants.RUN;

import java.util.HashMap;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;

public class StartProcessCallable implements SessionCallable<ProcessInstance> {

	private final DroolsRun run;
	private final String processId;

	public StartProcessCallable(DroolsRun run, String processId) {
		this.run = run;
		this.processId = processId;
	}

	public ProcessInstance call(StatefulKnowledgeSession session)
			throws Exception {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(RUN, new RunWrapper(run));
		long processInstanceId = session.startProcess(processId, parameters)
				.getId();
		ProcessInstance instance = session
				.getProcessInstance(processInstanceId);

		return instance;
	}

}
