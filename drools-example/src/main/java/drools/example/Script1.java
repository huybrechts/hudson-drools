package drools.example;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;

import hudson.drools.Script;

public class Script1 implements Script {
 
	@Override
	public Map execute(StatefulKnowledgeSession session, PrintWriter output,
			Map<String, Object> parameters) throws Exception {
		output.println("Script1 !");
		return Collections.singletonMap("result", "result");
	}
	

}
