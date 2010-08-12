import com.agfa.hudson.buildstate.BuildStateAction

import org.drools.runtime.StatefulKnowledgeSession;
import hudson.drools.Script;
import hudson.model.*
import hudson.plugins.sfee.*
import hudson.plugins.sfee.webservice.*

class SetPromotionLevel implements Script {
	/**
	 * input: RunWrapper build
	 */
	Map execute(StatefulKnowledgeSession session, PrintWriter output, Map parameters) {
		def build = args["build"]
		def state = args["state"]
		
		println "Set build state for $build to $state"
		
		build.run.getAction(BuildStateAction).state = state
		
	}
}
