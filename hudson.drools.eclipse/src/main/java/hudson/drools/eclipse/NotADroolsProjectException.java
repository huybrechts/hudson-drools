package hudson.drools.eclipse;
 
public class NotADroolsProjectException extends Exception {

	private final String message;

	public NotADroolsProjectException(String message) {
		super();
		this.message = message;
	}
	
}
