package agility;

import hudson.drools.RunWrapper;
import hudson.drools.Script;

import java.io.PrintWriter;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;

import com.agfa.hudson.buildstate.BuildStateAction;

public class SetPromotionLevel implements Script {

	@Override
	public Map execute(StatefulKnowledgeSession session, PrintWriter output,
			Map<String, Object> parameters) throws Exception {
		
		RunWrapper run = (RunWrapper) parameters.get("build");
		String state = (String) parameters.get("state");
		
		output.printf("Setting build state for %s to %s\n", run, state);
		
		run.getRun().getAction(BuildStateAction.class).setState(state);
		
		return OK;
	}

}
