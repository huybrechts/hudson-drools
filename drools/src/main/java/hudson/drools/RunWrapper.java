package hudson.drools;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterValue;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RunWrapper implements Externalizable {

	private transient Run<?, ?> run;

	public RunWrapper(Run<?, ?> run) {
		super();
		this.run = run;
	}
	
	public RunWrapper() {}

	public Run getRun() {
		return run;
	}

	public String getDisplayName() {
		return run.getFullDisplayName();
	}

	public Result getResult() {
		return run.getResult();
	}

	public boolean isSuccess() {
		return run.getResult() == Result.SUCCESS;
	}

	public boolean isUnstable() {
		return run.getResult() == Result.UNSTABLE;
	}

	public String getProjectName() {
		return run.getParent().getName();
	}

	@Override
	public String toString() {
		return run.getFullDisplayName();
	}

	private String getParameter(ParametersAction parameters, String name) {
		for (ParameterValue value : parameters.getParameters()) {
			if (name.equals(value.getName())) {
				return ((StringParameterValue) value).value;
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		return run.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RunWrapper other = (RunWrapper) obj;
		return run == other.run;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(runToString(run));
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		run = stringToRun(in.readUTF());
	}
	
	private static String runToString(Run run) {
		if (run == null) return "";
		Job<?, ?> job = run.getParent();
		String convertedValue = job.getName() + "#" + run.getNumber();
		return convertedValue;
	}

	private static Run stringToRun(String id) {
		if ("".equals(id)) return null;
		
		int hash = id.lastIndexOf('#');
		String jobName = id.substring(0, hash);
		String runNumber = id.substring(hash + 1);
		Hudson hudson = Hudson.getInstance();
		if (hudson == null) return null; // in simple unit test
		Job<?, ?> job = (Job<?, ?>) hudson.getItem(jobName);
		if (job == null) return null;
		Run<?, ?> run = job.getBuildByNumber(Integer.parseInt(runNumber));

		return run;

	}

}
