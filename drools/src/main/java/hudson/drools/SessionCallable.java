package hudson.drools;

import org.drools.runtime.StatefulKnowledgeSession;

public interface SessionCallable<T> {

	T call(StatefulKnowledgeSession session) throws Exception;
	
}
