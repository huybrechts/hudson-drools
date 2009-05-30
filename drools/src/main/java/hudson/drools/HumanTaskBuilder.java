package hudson.drools;

import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.util.BuilderSupport;
import hudson.model.BooleanParameterDefinition;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.StringParameterDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HumanTaskBuilder extends BuilderSupport {

	private HumanTask task;

	public HumanTaskBuilder() {
	}

	@Override
	protected Object createNode(Object name) {
		throw new IllegalArgumentException("HumanTaskBuilder.createNode("
				+ name + ")");
	}

	@Override
	protected Object createNode(Object name, Object value) {
		throw new IllegalArgumentException("unexpected: " + name + "(" + value
				+ ")");
	}

	private final List<String> validTypes = Arrays.asList("boolean", "string",
			"choice");

	@Override
	protected Object createNode(Object name, Map attributes) {
		if (task == null) {
			Boolean _privateTask = (Boolean) attributes.get("private");
			boolean privateTask = (_privateTask != null) ? _privateTask
					.booleanValue() : false;
			String title = (String) attributes.get("title").toString();
			return task = new HumanTask((String) title, privateTask);
		}
		Object type = attributes.get("type");
		if (!validTypes.contains(type)) {
			throw new IllegalArgumentException(type + " is not a valid type "
					+ validTypes);
		}

		String description = (String) attributes.get("description");

		if ("string".equals(type)) {
			return new StringParameterDefinition((String) name,
					(String) attributes.get("defaultValue"), description);
		} else if ("boolean".equals(type)) {
			Boolean defaultValue = (Boolean) attributes.get("defaultValue");
			return new BooleanParameterDefinition((String) name,
					defaultValue != null ? defaultValue : false, description);
		} else if ("choice".equals(type)) {
			List<String> choices = (List<String>) attributes.get("choices");
			return new ChoiceParameterDefinition((String) name,
					(String[]) choices.toArray(new String[choices.size()]),
					description);
		} else {
			throw new AssertionError("can't get here");
		}
	}

	@Override
	protected Object createNode(Object name, Map attributes, Object value) {
		throw new IllegalArgumentException(
				"unexpected HumanTaskBuilder.createNode(" + name + ","
						+ attributes + "," + value + ")");
	}

	@Override
	protected void setParent(Object parent, Object child) {
		if (parent instanceof HumanTask && child instanceof ParameterDefinition) {
			((HumanTask) parent).getParameterDefinitions().add(
					(ParameterDefinition) child);
		} else {
			throw new IllegalArgumentException(
					"unexpected HumanTaskBuilder.setParent(" + parent + ","
							+ child + ")");
		}
	}

	public static void main(String[] args) {
		String script = "def task = { title,closure -> new hudson.drools.HumanTaskBuilder().task(title, closure) }\n";

		script += "task(title:\"A Question\",private:false) {\n";
		script += "reply type: \"boolean\", description: \"description\", defaultValue:false\n";
		script += "}\n";

		GroovyShell shell = new GroovyShell(HumanTaskBuilder.class
				.getClassLoader());
		GroovyCodeSource codeSource = new GroovyCodeSource(script, "name", ".");
		HumanTask question = (HumanTask) shell.evaluate(codeSource);

		System.out.println(question.isPrivateTask());

	}

}
