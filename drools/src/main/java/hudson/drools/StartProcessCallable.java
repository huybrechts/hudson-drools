package hudson.drools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;

import static hudson.drools.Constants.*;

public class StartProcessCallable implements Callable<ProcessInstance> {

	private final DroolsRun run;
	private final StatefulKnowledgeSession session;
	private final String processId;

	public StartProcessCallable(DroolsRun run,
			StatefulKnowledgeSession session, String processId) {
		super();
		this.run = run;
		this.session = session;
		this.processId = processId;
	}


	public ProcessInstance call() throws Exception {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(RUN, new RunWrapper(run));
		long processInstanceId = session.startProcess(processId, parameters)
				.getId();
		ProcessInstance instance = session
				.getProcessInstance(processInstanceId);

		return instance;
	}

}
