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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class RunWrapper implements Externalizable {

	private static final long serialVersionUID = 1;

	private transient Run<?, ?> run;

	public RunWrapper(Run<?, ?> run) {
		super();
		this.run = run;
	}

	public RunWrapper() {
	}

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
	
	public int getBuildNumber() {
		return run.getNumber();
	}

	@Override
	public String toString() {
		return run != null ? run.getFullDisplayName() : "";
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
		String s = runToString(run);
		out.writeUTF(s);
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		String s = in.readUTF();
		if (!"".equals(s)) {
			run = stringToRun(s);
		}
	}

	public static String runToString(Run run) {
		if (run == null)
			return "";
		Job<?, ?> job = run.getParent();
		String convertedValue = job.getName() + "#" + run.getNumber();
		return convertedValue;
	}

	public static Run stringToRun(String id) {
		if (id == null || "".equals(id))
			return null;
		int hash = id.lastIndexOf('#');
		
		if (hash < 0) return null;
		
		String jobName = id.substring(0, hash);
		String runNumber = id.substring(hash + 1);
		Hudson hudson = Hudson.getInstance();
		if (hudson == null)
			return null; // in simple unit test
		Job<?, ?> job = (Job<?, ?>) hudson.getItemMap().get(jobName);
		if (job == null)
			return null;
		Run<?, ?> run = job.getBuildByNumber(Integer.parseInt(runNumber));

		return run;
	}

	public static final class ConverterImpl implements Converter {
		public ConverterImpl() {
		}

		public boolean canConvert(Class type) {
			return type == RunWrapper.class;
		}

		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			RunWrapper src = (RunWrapper) source;
			writer.setValue(runToString(src.run));
		}

		public Object unmarshal(HierarchicalStreamReader reader,
				final UnmarshallingContext context) {
			String id = reader.getValue();
			Run r = stringToRun(id);
			if (r != null) return new RunWrapper(r);
			
			int hash = id.lastIndexOf('#');
			if (hash > 0) {
				String jobName = id.substring(0, hash);
				String runNumber = id.substring(hash + 1);
				return new NullRunWrapper(jobName, runNumber);
			} else {
				return new NullRunWrapper();
			}
		}
	}
	
	public static class NullRunWrapper extends RunWrapper {
		private String buildNumber;
		private String projectName;
		
		public NullRunWrapper() {
			// required for Externalizable
		}

		NullRunWrapper(String projectName, String buildNumber) {
			super(null);
			this.projectName = projectName;
			this.buildNumber = buildNumber;
		}
		
		public String getDisplayName() {
			return "build unavailable";
		}

		public Result getResult() {
			return Result.NOT_BUILT;
		}

		public boolean isSuccess() {
			return false;
		}

		public boolean isUnstable() {
			return false;
		}

		public String getProjectName() {
			return projectName;
		}

		@Override
		public String toString() {
			if (projectName != null && buildNumber != null) {
				return projectName + " #" + buildNumber;
			} else {
				return "<unknown>";
			}
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}
	}
}
