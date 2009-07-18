package hudson.drools;

import org.drools.runtime.StatefulKnowledgeSession;

public enum SessionProvider {
	
	PER_PROJECT {
		
		@Override
		public StatefulKnowledgeSession getSession(DroolsProject project) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	GLOBAL {
		
		
		
		@Override
		public StatefulKnowledgeSession getSession(DroolsProject project) {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	public abstract StatefulKnowledgeSession getSession(DroolsProject project);  

}
