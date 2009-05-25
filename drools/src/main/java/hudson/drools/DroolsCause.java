/**
 * 
 */
package hudson.drools;

import hudson.model.Cause;

public class DroolsCause extends Cause {

	private final String cause;

	public DroolsCause(String cause) {
		super();
		this.cause = cause;
	}

	@Override
	public String getShortDescription() {
		return cause;
	}

}