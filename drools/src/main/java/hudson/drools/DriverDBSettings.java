package hudson.drools;

import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.transaction.BTMTransactionManagerLookup;
import org.kohsuke.stapler.DataBoundConstructor;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class DriverDBSettings extends DBSettings {

	private transient PoolingDataSource dataSource;

	private final String className;// = "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource";
	private final int maxPoolSize;// = 20;
	private final String user;// = "tom";
	private final String password;// = "tom";
	private final String url;// = "jdbc:mysql://smlsw10.be.local/tom";
	private final String hibernateDialect;
	
	@DataBoundConstructor
	public DriverDBSettings(String className, String user,
			String password, String url, String hibernateDialect) {
		super();
		this.className = className;
		this.maxPoolSize = 20;
		this.user = user;
		this.password = password;
		this.url = url;
		this.hibernateDialect = hibernateDialect;
	}

	@Override
	public void start() throws NamingException {
		InitialContext initialContext = new InitialContext();
		try {
			initialContext.unbind(getDataSourceName());
		} catch (NamingException e) {
		}
		try {
			initialContext.unbind("java:comp/UserTransaction");
		} catch (NamingException e) {
		}

		try {
			initialContext.createSubcontext("jdbc");
		} catch (NamingException e2) {
		}
		try {
			initialContext.createSubcontext("java:comp");
		} catch (NamingException e2) {
		}
		
		dataSource = new PoolingDataSource();
		dataSource.setUniqueName(getDataSourceName());
		dataSource
				.setClassName(className);
		dataSource.setMaxPoolSize(maxPoolSize);
		dataSource.setAllowLocalTransactions(true);
		dataSource.getDriverProperties().put("user", user);
		dataSource.getDriverProperties().put("password", password);
		dataSource.getDriverProperties().put("URL",
				url);
		dataSource.init();
		
		initialContext.bind(getDataSourceName(), dataSource);
		initialContext.bind("java:comp/UserTransaction", TransactionManagerServices.getTransactionManager());
		
	}
	
	@Override
	public void stop() {
		TransactionManagerServices.getTransactionManager().shutdown();
		dataSource.close();
	}

	public PoolingDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(PoolingDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getClassName() {
		return className;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}
	
	@Override
	public String getDataSourceName() {
		return "jdbc/processInstanceDS";
	}

	@Override
	public String getHibernateDialect() {
		return hibernateDialect;
	}

	public Descriptor<DBSettings> getDescriptor() {
		return Hudson.getInstance().getDescriptor(DriverDBSettings.class);
	}
	
//	@Extension
	public static class DescriptorImpl extends Descriptor<DBSettings> {
		@Override
		public String getDisplayName() {
			return "Driver and connection";
		}
	}

	@Override
	public String getTransactionManagerLookupClass() {
		return BTMTransactionManagerLookup.class.getName();
	}

	@Override
	public EntityManagerFactory createEntityManagerFactory() {
		Map<String, String> overrides = new HashMap<String, String>();
		overrides.put(HibernatePersistence.JTA_DATASOURCE, getDataSourceName() /* "java:DroolsLoggingDS"*/);
		overrides.put("hibernate.dialect", getHibernateDialect());
		overrides.put("hibernate.transaction.manager_lookup_class",
				getTransactionManagerLookupClass());
		EntityManagerFactory emf = Persistence.createEntityManagerFactory(
				"org.drools.persistence.jpa", overrides);
		return emf;
	}
}
