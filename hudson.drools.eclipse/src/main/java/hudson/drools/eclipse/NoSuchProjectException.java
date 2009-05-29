package hudson.drools.eclipse;

public class NoSuchProjectException extends Exception {

	private final String details;

	public NoSuchProjectException(String message, String details) {
		super(message);
		this.details = details;
	}
	
}
