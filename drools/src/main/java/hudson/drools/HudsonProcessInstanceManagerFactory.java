package hudson.drools;

import org.drools.WorkingMemory;
import org.drools.process.instance.ProcessInstanceManager;
import org.drools.process.instance.ProcessInstanceManagerFactory;

public class HudsonProcessInstanceManagerFactory implements
		ProcessInstanceManagerFactory {
	
	public ProcessInstanceManager createProcessInstanceManager(
			WorkingMemory workingMemory) {
		return new HudsonProcessInstanceManager();
	}

}
