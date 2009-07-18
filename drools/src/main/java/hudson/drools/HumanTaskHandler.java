package hudson.drools;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class HumanTaskHandler implements WorkItemHandler {
	
	private static final String ACTOR_ID = "ActorId";
	private static final String CONTENT = "Content";
	private static final Logger logger = Logger.getLogger(HumanTaskHandler.class.getName());
	private final DroolsProject project;
	
	public HumanTaskHandler(DroolsProject project) {
		this.project = project;
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		HumanTask humanTask = HumanTask.getHumanTaskByWorkItemId((int) workItem.getId());
		if (humanTask != null) {
			try {
				humanTask.cancel();
			} catch (IOException e) {
				e.printStackTrace(humanTask.getRun().getLogWriter());
			}
		} else {
			logger.warning("couldn't find DroolsRun for work item " + workItem.getId());
		}
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		String script = (String) workItem.getParameter(CONTENT);
		script = "def task = { title,closure -> new hudson.drools.HumanTaskBuilder().task(title, closure) }\n" + script;
		
		Binding binding = new Binding(new HashMap(workItem.getParameters()));
		
		GroovyShell shell = new GroovyShell(HumanTaskBuilder.class.getClassLoader(), binding);
		GroovyCodeSource codeSource = new GroovyCodeSource(script, "name", ".");
		HumanTask question = (HumanTask) shell.evaluate(codeSource);
		
		long processInstanceId = workItem.getProcessInstanceId();
		DroolsRun run = project.getFromProcessInstance(processInstanceId);
		question.setWorkItemId(workItem.getId());
		question.setActorId((String) workItem.getParameter(ACTOR_ID));
		run.addHumanTask(question);
		
	}

}
