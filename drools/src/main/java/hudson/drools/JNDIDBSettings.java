package hudson.drools;

import hudson.model.Descriptor;
import hudson.model.Hudson;

import javax.persistence.EntityManagerFactory;

import org.hibernate.transaction.JBossTransactionManagerLookup;

public class JNDIDBSettings extends DBSettings {

	private final String dataSourceName;
	private final String hibernateDialect;

	public JNDIDBSettings(String dataSourceName, String hibernateDialect) {
		super();
		this.dataSourceName = dataSourceName;
		this.hibernateDialect = hibernateDialect;
	}

	@Override
	public String getDataSourceName() {
		return dataSourceName;
	}

	@Override
	public String getHibernateDialect() {
		return hibernateDialect;
	}

	public Descriptor<DBSettings> getDescriptor() {
		return Hudson.getInstance().getDescriptor(JNDIDBSettings.class);
	}
	
//	@Extension
	public static class DescriptorImpl extends Descriptor<DBSettings> {
		@Override
		public String getDisplayName() {
			return "DataSource in JNDI";
		}
	}

	@Override
	public String getTransactionManagerLookupClass() {
		return JBossTransactionManagerLookup.class.getName();
	}

	@Override
	public EntityManagerFactory createEntityManagerFactory() {
		throw new UnsupportedOperationException("TODO");
	}
	
}
