package hudson.drools.eclipse;

public class NoSuchProjectException extends Exception {

	private final String message, details;

	public NoSuchProjectException(String message, String details) {
		super();
		this.message = message;
		this.details = details;
	}
	
}
