package hudson.drools;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.transaction.JBossTransactionManagerLookup;
import org.kohsuke.stapler.DataBoundConstructor;

public class JBossDBSettings extends DBSettings {

	@DataBoundConstructor
	public JBossDBSettings() {
	}

	@Override
	public void start() throws NamingException {
	}

	@Override
	public void stop() {
		super.stop();
	}

	public Descriptor<DBSettings> getDescriptor() {
		return Hudson.getInstance().getDescriptor(JBossDBSettings.class);
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<DBSettings> {
		@Override
		public String getDisplayName() {
			return "JBoss DB Settings (DroolsDS)";
		}
	}

	@Override
	public String getDataSourceName() {
		return "java:DroolsDS";
	}

	@Override
	public String getHibernateDialect() {
		return MySQL5Dialect.class.getName();
	}

	@Override
	public String getTransactionManagerLookupClass() {
		return JBossTransactionManagerLookup.class.getName();
	}

	@Override
	public EntityManagerFactory createEntityManagerFactory() {
		Map<String, String> overrides = new HashMap<String, String>();
		overrides.put(HibernatePersistence.JTA_DATASOURCE, getDataSourceName());
		overrides.put("hibernate.dialect", getHibernateDialect());
		overrides.put("hibernate.transaction.manager_lookup_class",
				getTransactionManagerLookupClass());
		EntityManagerFactory emf = Persistence.createEntityManagerFactory(
				"org.drools.persistence.jpa", overrides);
		return emf;
	}
}
