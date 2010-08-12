import org.drools.runtime.StatefulKnowledgeSession;
import hudson.drools.Script;
import hudson.model.*
import hudson.plugins.sfee.*
import hudson.plugins.sfee.webservice.*

class CTFGate2B extends CTFSupport implements Script {
	
	/**
	 * input: RunWrapper build
	 */
	Map execute(StatefulKnowledgeSession session, PrintWriter output, Map parameters) {
		this.output = output
		
		Run  build = args["build"].run
		output.println "build=$build"
		
		def version = build.description
		if (!version) {
			throw new IllegalStateException("Description missing for ${build.displayName}.")
		}
		
		//decide what branch we are on
		def branch = branches.find { version.contains(it) } ?: "trunk"
		
		output.println "branch $branch"
		def name = branchNames[branch]
		
		def branchReleaseName = "$name Stream"
		def branchReleaseId = getRelease(branchReleaseName)
		def candidateBranchReleaseName = "Candidate $name Stream"
		def candidateBranchReleaseId = getRelease(candidateBranchReleaseName)
		
		def trackerId = getTracker(trackerNames[branch])
		def artifacts = getArtifacts([trackerId],[new SoapFilter("status","Fixed")])
		
		for (artifact in artifacts) {
			updateArtifact(
					artifact, { it.status == "Fixed" && it.resolvedInReleaseTitle == candidateBranchReleaseName }, { true }, { it.resolvedReleaseId = branchReleaseId }
					)
		}
		
		output.println "setting build to Verification"
		build.getAction(com.agfa.hudson.buildstate.BuildStateAction).state = "Verification"
		
		return [result:true]
	}
	
	
}