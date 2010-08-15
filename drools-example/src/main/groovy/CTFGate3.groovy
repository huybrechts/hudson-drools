import org.drools.runtime.StatefulKnowledgeSession;
import hudson.drools.Script;
import hudson.model.*
import hudson.plugins.sfee.*
import hudson.plugins.sfee.webservice.*

class CTFGate3 implements Script {
	
	/**
	 * input: RunWrapper build
	 */
	Map execute(StatefulKnowledgeSession session, PrintWriter output, Map parameters) {
		this.output = output
		
		Run  build = args["build"].run
		output.println "build=$build"
		Job job = build.parent
		def projectId = job.getProperty(SourceForgeProject).projectId
		def packageId = job.getProperty(SourceForgeProject).releasePackageId
		
		def version = build.description
		if (!version) {
			throw new IllegalStateException("Description missing for ${build.displayName}.")
		}
		
		def releaseId = getRelease(version)
		if (releaseId) output.println "Release $version in package $packageId already exists (${releaseId})"
		else {
			output.println "Creating release $version in package $packageId"
			releaseId = frsApp.createRelease(site.sessionId, packageId, version, build.url, "active", "Development Build").id;
			
			def oldVersion = build.previousNotFailedBuild?.description
			if (oldVersion) { obsoleteRelease(oldVersion)
			}
		}
		
		def previousBuild = job.getAction(com.agfa.hudson.buildstate.BuildStateProjectAction).getLast("Verification")
		def previousRevisions = getBranchRevisions(previousBuild)
		def currentRevisions = getBranchRevisions(build)
		
		def trackerIds = trackerNames.collect { k,v -> getTracker(v) }
		def artifacts = getArtifacts(trackerIds, [new SoapFilter("status","Fixed")])
		output.println "checking artifacts " + (artifacts as List).join(",")
		
		def changed = false
		for (b in branches) {
			def prevRev = previousRevisions[branchesSVN[b]]
			def currRev = currentRevisions[branchesSVN[b]] 
			if (prevRev != currRev) {
				changed = true
				output.println "$b branch merged $prevRev -> $currRev"
				
				def branchReleaseName = "${branchNames[b]} Stream"
				def branchReleaseId = getRelease(branchReleaseName)
				
				for (artifact in artifacts) {
					updateArtifact(
							artifact, { it.status == "Fixed" && it.resolvedInReleaseTitle == branchReleaseName }, { true }, {
								it.resolvedReleaseId = releaseId; it.status = "Resolved"; setFlexField(it, "Fixed in Release (text)", null);
							}
							)
				}
				
			}
		}
		if (!changed) output.println "there were no branch merges"
		output.println "setting build to Verification"
		build.getAction(com.agfa.hudson.buildstate.BuildStateAction).state = "Verification"
		
		return [result:true]
	}
	
	
}