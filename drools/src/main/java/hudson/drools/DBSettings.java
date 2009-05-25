package hudson.drools;

import hudson.DescriptorExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

public abstract class DBSettings implements Describable<DBSettings>{
	
	public static DescriptorExtensionList<DBSettings, Descriptor<DBSettings>> all() {
		return Hudson.getInstance().getDescriptorList(DBSettings.class);
	}

	public void start() throws NamingException {
	}

	public void stop() {
	}
	
	public abstract String getDataSourceName();

	public abstract String getHibernateDialect();

	public abstract String getTransactionManagerLookupClass();
	
//	public abstract SessionFactory createSessionFactory();
	
	public abstract EntityManagerFactory createEntityManagerFactory();
}
