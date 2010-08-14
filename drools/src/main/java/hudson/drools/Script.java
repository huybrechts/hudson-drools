package hudson.drools;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;

public abstract class Script {

	public static final Map<String,Object> OK = Collections.<String,Object>singletonMap("result", true);

	protected StatefulKnowledgeSession session;
	protected PrintWriter output;

	public StatefulKnowledgeSession getSession() {
		return session;
	}

	public void setSession(StatefulKnowledgeSession session) {
		this.session = session;
	}

	public PrintWriter getOutput() {
		return output;
	}

	public void setOutput(PrintWriter output) {
		this.output = output;
	}

	public abstract Map<String, Object> execute() throws Exception;

}