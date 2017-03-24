package hudson.drools;

import hudson.Util;
import hudson.model.Hudson;
import hudson.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.fileupload.FileItem;

public class ArchiveManager {

	private static final ArchiveManager INSTANCE = new ArchiveManager();

	private Map<File, WeakReference<ClassLoader>> classloaders = new HashMap<File, WeakReference<ClassLoader>>();

	public static ArchiveManager getInstance() {
		return INSTANCE;
	}

	public File getArchiveStorage() {
		return new File(Hudson.getInstance().getRootDir(), "drools");
	}

	public File uploadFile(FileItem item) throws IOException {
		String md5 = Util.getDigestOf(item.getInputStream());
		File target = new File(getArchiveStorage(), md5 + ".jar");
		if (target.exists()) {
			return target;
		} else {
			if (!getArchiveStorage().exists())
				getArchiveStorage().mkdirs();
			IOUtils.copy(item.getInputStream(), target);
		}

		return target;
	}

	public synchronized ClassLoader getClassLoader(File f) throws IOException {
		WeakReference<ClassLoader> result = classloaders.get(f);
		if (result == null || result.get() == null) {
			result = new WeakReference<ClassLoader>(new URLClassLoader(new URL[] { f.toURI().toURL() }, Hudson
					.getActiveInstance().getPluginManager().uberClassLoader));
			classloaders.put(f, result);
		}
		return result.get();
	}

	public synchronized String getInfo(File f) throws IOException {
		FileInputStream is = new FileInputStream(f);
		
		String md5; 
		try {
			md5 = Util.getDigestOf(is);
		} finally {
			is.close();
		}
		
		JarFile jf = new JarFile(f, false);
		try {
			Enumeration<JarEntry> en = jf.entries();
			while (en.hasMoreElements()) {
				JarEntry entry = en.nextElement();
				if (!entry.getName().endsWith("/pom.properties"))
					continue;

				Properties p = new Properties();
				p.load(jf.getInputStream(entry));

				return p.getProperty("groupId") + ":"
						+ p.getProperty("artifactId") + ":"
						+ p.getProperty("version") + " (" + md5 + ")";
			}
			
			throw new IOException("no pom.properties found in " + f.getName());
		} finally {
			jf.close();
		}
	}
}
