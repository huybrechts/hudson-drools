package hudson.drools;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;

public interface Script {
	
	public static Map OK = Collections.singletonMap("result", true);

	public abstract Map execute(StatefulKnowledgeSession session,
			PrintWriter output, Map<String, Object> parameters)
			throws Exception;

}