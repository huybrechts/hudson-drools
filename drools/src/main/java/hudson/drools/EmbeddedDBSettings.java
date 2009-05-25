package hudson.drools;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.io.File;

import javax.naming.NamingException;

import org.hibernate.transaction.BTMTransactionManagerLookup;
import org.kohsuke.stapler.DataBoundConstructor;

import bitronix.tm.jndi.BitronixInitialContextFactory;

public class EmbeddedDBSettings extends DriverDBSettings {

	@DataBoundConstructor
	public EmbeddedDBSettings() {
		super("org.h2.jdbcx.JdbcDataSource", "sa", "sasa", "jdbc:h2:file:"
				+ new File(Hudson.getInstance().getRootDir(), "drools-db")
						.getAbsolutePath(), "org.hibernate.dialect.H2Dialect");
	}

	@Override
	public Descriptor<DBSettings> getDescriptor() {
		return Hudson.getInstance().getDescriptor(EmbeddedDBSettings.class);
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<DBSettings> {
		@Override
		public String getDisplayName() {
			return "Embedded Database";
		}
	}

	@Override
	public String getTransactionManagerLookupClass() {
		return BTMTransactionManagerLookup.class.getName();
	}
	
}
