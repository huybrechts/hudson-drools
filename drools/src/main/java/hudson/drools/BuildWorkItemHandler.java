package hudson.drools;

import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.RunParameterValue;
import hudson.model.StringParameterValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class BuildWorkItemHandler implements WorkItemHandler {

	private static final String COMPLETE_WHEN_UNSTABLE = "Complete when unstable";
	private static final String COMPLETE_WHEN_FAILED = "Complete when failed";
	private static final String PROJECT = "Project";

	private final DroolsProject project;

	public BuildWorkItemHandler(DroolsProject project) {
		super();
		this.project = project;
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		String projectName = (String) workItem.getParameter(PROJECT);
		Boolean completeWhenFailed = (Boolean) workItem
				.getParameter(COMPLETE_WHEN_FAILED);
		Boolean completeWhenUnstable = (Boolean) workItem
				.getParameter(COMPLETE_WHEN_UNSTABLE);

		List<ParameterValue> values = new ArrayList<ParameterValue>();

		for (Map.Entry<String, Object> parameter : workItem.getParameters()
				.entrySet()) {
			if (parameter.getValue() instanceof String) {
				values.add(new StringParameterValue(parameter.getKey(),
						(String) parameter.getValue(), "drools parameter"));
			}
			if (parameter.getValue() instanceof Boolean) {
				values.add(new BooleanParameterValue(parameter.getKey(),
						(Boolean) parameter.getValue(), "drools parameter"));
			}
			if (parameter.getValue() instanceof RunWrapper) {
				values.add(new RunParameterValue(parameter.getKey(),
						((RunWrapper) parameter.getValue()).getRun()
								.getExternalizableId(), "drools parameter"));
			}
		}

		new WorkItemAction(project.getName(), workItem.getId(), workItem
				.getProcessInstanceId(), projectName,
				completeWhenFailed != null ? completeWhenFailed : false,
				completeWhenUnstable != null ? completeWhenUnstable : false,
				values).scheduleBuild();

	}

}
