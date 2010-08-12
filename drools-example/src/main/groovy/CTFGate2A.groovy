import org.drools.runtime.StatefulKnowledgeSession;
import hudson.drools.Script;
import hudson.model.*
import hudson.plugins.sfee.*
import hudson.plugins.sfee.webservice.*

class CTFGate2A extends CTFSupport implements Script {
	
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
					artifact, { it.status == "Fixed" && it.resolvedInReleaseTitle == (branch!="IA"?null:"Passed IA Gate 1") }, { true }, { 
						it.resolvedReleaseId = candidateBranchReleaseId; 
						if (getFlexField(it,"Fixed in Release (Text)") == null || getFlexField(it,"Fixed in Release (Text)").trim() == "")
							setFlexField(it,"Fixed in Release (Text)",version) 
					}
					)
			updateArtifact(
					artifact, { it.reportedInReleaseTitle == candidateBranchReleaseName }, { getFlexField(it,"Reported in Release (Text)") == null || getFlexField(it,"Reported in Release (Text)").trim() == "" }, { setFlexField(it,"Reported in Release (Text)",version) }
					)
		}
		for (artifact in getArtifacts([trackerId], [new SoapFilter("status","New")])) {
			updateArtifact(
					artifact, { it.reportedInReleaseTitle == candidateBranchReleaseName }, { getFlexField(it,"Reported in Release (Text)") == null || getFlexField(it,"Reported in Release (Text)").trim() == "" }, { setFlexField(it,"Reported in Release (Text)",version) }
					)
		}
		
		return [result:true]
	}
	
	
}