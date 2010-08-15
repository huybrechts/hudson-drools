import hudson.model.*
import hudson.plugins.sfee.*
import hudson.plugins.sfee.webservice.*

import hudson.drools.Script;

class CTFSupport {
	
	def site = SourceForgeSite.DESCRIPTOR.site
	def trackerApp = site.trackerApp
	def frsApp = site.frsApp
	def output
	
	def branchNames = ["RIS":"RIS","IA":"IA","SERVER":"Server"]
	def branches = ["RIS","IA","SERVER"] 
	def branchesSVN = ["RIS":"ris-dev","IA":"ia-dev","SERVER":"server-dev"]
	def trackerNames = [
	"RIS":"Issues - List & Text Area",
	"IA":"Issues - Image Area",
	"SERVER":"Issues - Server components"
	]
	
	//one more recent then two (on same branch)
	def isVersionMoreRecent(one,two) {
		def m1 = one =~ /(\d+)\.(\d+)\.(\w+)\.b(\d+)\.(\d{8})/
		def m2 = two =~ /(\d+)\.(\d+)\.(\w+)\.b(\d+)\.(\d{8})/
		if (!m1.matches() || !m2.matches()) return false
		def (_1,major1,minor1,branch1,build1,date1) = m1[0]
		def (_2,major2,minor2,branch2,build2,date2) = m2[0]
		minor1 = Integer.parseInt(minor1)                                                 
		minor2 = Integer.parseInt(minor2)                                                 
		build1 = Integer.parseInt(build1)                                                 
		build2 = Integer.parseInt(build2)                                                 
		return (major1 == major2) && (minor1 > minor2 || minor1 == minor2 && build1 > build2)
	}
	
	def getFlexField(artifact,fieldName) {
		def result = null
		artifact.flexFields.names.eachWithIndex { name,index -> if (name == fieldName) result = artifact.flexFields.values[index] }
		return result
	}
	
	def setFlexField(artifact, fieldName, fieldValue) {
		output.println "setFlexField $fieldName to $fieldValue for ${artifact.id}"
		artifact.flexFields.names.eachWithIndex {  name,index ->
			if (name == fieldName) artifact.flexFields.values[index] = fieldValue 
		}	
	} 
	
	def getBranchRevisions(build) { 
		def pattern = ~/\/branches\/(.*):1-(.*) \/branches\/(.*):1-(.*) \/branches\/(.*):1-(.*)/
		def revisions
		build.logReader.withReader { reader -> 
			reader.eachLine { line ->
				def matcher = pattern.matcher(line.trim())
				if (matcher.matches()) {
					revisions = [(matcher[0][1]):matcher[0][2],(matcher[0][3]):matcher[0][4],(matcher[0][5]):matcher[0][6]]
				}
			}
		}
		return revisions
	}
	
	def getArtifacts(trackerIds, filters) { 
		trackerIds.collect {
			output.println "getting artifacts for tracker $it"
			trackerApp.getArtifactDetailList(site.sessionId, it, null, filters as SoapFilter[], null, 0, -1, false, true).dataRows
		}.flatten()
	}
	
	def getTracker(trackerName) { 
		def result = trackerApp.getTrackerList(site.sessionId, projectId).dataRows.find { it.title == trackerName }.id
		output.println "Tracker $trackerName has id $result"
		if (!result) throw new IllegalStateException("unknown tracker $trackerName")
		return result
	}
	
	def updateArtifact(artifact,rowCondition,dataCondition,update) {
		if (!rowCondition(artifact)) return
			output.println "rowCondition matched on ${artifact.id}"    
		output.println "getting artifact data for ${artifact.id}"
		def data = trackerApp.getArtifactData(site.sessionId, artifact.id)
		if (dataCondition(data)) {
			output.println "dataCondition matched on ${artifact.id}"
			update(data)
			output.println "updating artifact ${data.id} ${data.title}"
			trackerApp.setArtifactData(site.sessionId, data, "artifact update by workflow",null,null,null)
			output.println "updating complete ${data.id} ${data.title}"
		}
	}
	
	def getRelease(versionName) {
		def packages = frsApp.getPackageList(site.sessionId, projectId).dataRows
		for (p in packages) {
			def result = frsApp.getReleaseList(site.sessionId, p.id).dataRows.find { it.title == versionName }?.id
			if (result) {
				output.println "release $versionName has id $result"
				return result
			}
		}
		return null
	}
	
	def obsoleteRelease(v) {
		output.println "SFEE: finding release $v"
		def oldReleaseId = site.getReleaseId(packageId, v)
		if (oldReleaseId) {
			output.println "SFEE: release found -- obsoleting"
			site.obsoleteRelease(oldReleaseId)
		} else {
			output.println "SFEE: release not found"
		}
	}
}
