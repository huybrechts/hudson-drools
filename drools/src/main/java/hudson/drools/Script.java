package hudson.drools;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import hudson.model.Hudson;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;

public class Script {

	private final String id;
	private final String source;

	@DataBoundConstructor
	public Script(String id, String source) {
		super();
		this.id = id;
		this.source = source;
	}

	public String getId() {
		return id;
	}

	public String getSource() {
		return source;
	}

	public Map execute(PrintWriter output, Map<String, Object> parameters) throws Exception {
		Binding binding = new Binding();
		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			binding.setVariable(entry.getKey(), entry.getValue());
		}
		binding.setVariable("session", PluginImpl.getInstance().getSession());
		binding.setVariable("hudson", Hudson.getInstance());
		binding.setVariable("args", parameters);
 		binding.setVariable("out", output);

		GroovyShell shell = new GroovyShell(Hudson.getInstance()
				.getPluginManager().uberClassLoader, binding);
		GroovyCodeSource codeSource = new GroovyCodeSource(source, id, id);
		Object result = shell.evaluate(codeSource);

		if (result instanceof Map) {
			return (Map) result;
		} else {
			Map results = new HashMap();
			results.put("result", result);
			return results;
		}

	}

}
