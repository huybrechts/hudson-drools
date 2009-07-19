TODO
----

- E-mail work item (done, untested)
- task assignment
- security (partially completed -- need to disable links in jelly)
- show task only to assigned users (boolean available, need to adapt jelly)
- update site for eclipse plugin (done, find some place to host it)
- documentation

idea: continue/stop workflow if certain tests have failed


postpone
--------
- warn when multiple processes have the same id
- svg workflow image
- eclipse plugin: remember project per .rf
- icon rendering 



Examples
--------
- staging workflow

Done
----
- eclipse or maven submit plugin
- don't show questions for canceled process
- log when process instance is not started correctly (set state to aborted or failed)
- confirmation for cancel, delete
- a groovy builder for questions
- deploy dialog: after navigating away from url, project value is overwritten
- undeploy package when deleting project
- abortWorkItem on human task


Required: 
- Eclipse 3.5
- Drools Workbench (update site: http://downloads.jboss.com/drools/updatesite3.4/)
- Hudson extension for Drools Workbench (update site: https://svn.dev.java.net/svn/hudson/trunk/hudson/plugins/drools/hudson.drools.updatesite, user:guest, password:none)
 

- put this pom in a new directory

<project>

	<modelVersion>4.0.0</modelVersion>
	<groupId>REPLACE_ME</groupId>
	<artifactId>REPLACE_ME</artifactId>
	<version>1.0-SNAPSHOT</version>
	<packaging>hpi</packaging>

	<dependencies>
		<dependency>
			<groupId>org.jvnet.hudson.plugins</groupId>
			<artifactId>drools</artifactId>
			<version>0.2</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.jvnet.hudson.tools</groupId>
				<artifactId>maven-hpi-plugin</artifactId>
				<extensions>true</extensions>
				<version>1.37</version>
				<dependencies>
					<dependency>
						<groupId>javax.servlet</groupId>
						<artifactId>servlet-api</artifactId>
						<version>2.4</version>
					</dependency>
					<dependency>
						<groupId>org.eclipse.jdt</groupId>
						<artifactId>core</artifactId>
						<version>3.4.2.v_883_R34x</version>
					</dependency>
				</dependencies>
			</plugin>
		</plugins>
	</build>

  <repositories>
    <repository>
      <id>m.g.o-public</id>
      <url>http://maven.glassfish.org/content/groups/public/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </repository>
  </repositories>

</project>


- create and import a project with 'mvn eclipse:eclipse' or use m2eclipse.

- create a new 'RuleFlow file' in the project

- you should see two Workitems bottom left called 'Script', 'Build' and 'E-Mail'

- Change the project properties. Set Package to MyFirstWorkflow and Id to MyFirstWorkflow

- Add a 'Build' node. In the properties view, set Project to 'Project1' and Name to 'Project1'

- Add an End node

- Add connections from Start to Project1 and from Project1 to End

- click the green checkmark to validate your workflow

- Download Hudson, start, add the Drools plugin, restart 

- Right-click the file, Hudson > Create Project
Hudson URL should be filled in, skip username and password, enter project name: "MyFirstWorkflow" 

- Go to Hudson, create a freestyle project called 'Project1' (no extra settings needed)

- start a MyFirstWorkflow build




Conventions
-----------
Build work item
* Parameters:
- Project: name of project to build
- Complete when failed: complete the work item (advancing the workflow) if the build failed
- Complete when unstable: complete the work item (advancing the workflow) if the build is unstable
When the work item is not completed after the build, the user will have the choice (in the UI) to either
accept the result or retry the build.
* All work item parameters of type string, boolean or RunWrapper are passed as parameters to the Hudson build.
* When build and work item are completed, the build will be available to the workflow as result parameter 'Build', wrapped in a RunWrapper.

DroolsRun
- when the workflow is started, the DroolsRun is available as a Run parameter

E-Mail work item
(untested)
* recipients: comma-separated. Can be e-mail address or hudson user ids.

Human Task
groovy dsl, specified as 'Content' of work item.
syntax:

task(title:"Question?", private:false) {
	id_of_parameter_1 (
		type: "string", 
		description: "What is your reply ?", 
		defaultValue: "default reply"
	) 
	id_of_parameter_2 type:"boolean", description: "bla", defaultValue: true
	id_of_parameter_3 type:"choice", description: "choice parameter", choices: [ "foo", "bar" ] 
}

- the single line and multiline parameters are equivalent
- valid types are string, boolean and choice
- defaultValue is optional (defaults to null or false)
- When completed, a result parameter will be created for every question, with the same id.
- if ActorId is present, only a Hudson user with the same id can complete the task
- if private is true, only a Hudson user equal to the actor id can see the task (not tested yet

Events
On every build completion in Hudson, an event is created, called 'BuildComplete:projectName'. You can
listen to this in a workflow, but you should now that this event may be triggered multiple times !
Consider starting this build from the workflow itself.

RuleFlowRenderer
- supports WorkItem, HumanTask, Build, Script, Split, Event, Start, End, ForEach
Other nodes work are rendered as a black&white box with only the name
- Connections are always rendered as straight lines from center to center.

Script
Scripts are written in Groovy. Then can access following parameters:
- session (the drools StatefulKnowledgeSession)
- hudson (Hudson.getInstance())
- args (a map with the parameters passed by the workflow)
- out (a PrintWriter that logs to the build log)

If the script returns a Map, that is passed as a result to the workflow.
Any other object will be wrapped in a Map with key 'result'.

When scripts fail (throw an exception), they can always be restarted.
If you don't want this, then don't throw an exception.


Demo
----
staging workflow
1. no workflow, simple build with tests and immediate deployment 
2. split up build and tests. add staging and workflow
2. add a second test project
3. make test fail (restart)
4. add manual test



