package hudson.drools;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.kohsuke.stapler.DataBoundConstructor;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixInitialContextFactory;

public class DevelopmentDBSettings extends DriverDBSettings {

	private transient BitronixTransactionManager transactionManager;

	@DataBoundConstructor
	public DevelopmentDBSettings(String className, String user,
			String password, String url, String hibernateDialect) {
		super(className, user, password, url, hibernateDialect);
	}

	@Override
	public void start() throws NamingException {
		System.setProperty("java.naming.factory.initial",
				BitronixInitialContextFactory.class.getName());
		super.start();

		transactionManager = TransactionManagerServices.getTransactionManager();

		try {
			new InitialContext().bind("java:comp/UserTransaction",
					transactionManager);
		} catch (Exception e) {
		}
	}

	@Override
	public void stop() {
		super.stop();
		transactionManager.shutdown();
	}

	public BitronixTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(
			BitronixTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public Descriptor<DBSettings> getDescriptor() {
		return Hudson.getInstance().getDescriptor(DevelopmentDBSettings.class);
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<DBSettings> {
		@Override
		public String getDisplayName() {
			return "Development DB Settings";
		}
	}
}
