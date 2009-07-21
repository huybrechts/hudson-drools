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

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		String s = in.readUTF();
		run = stringToRun(s);
	}
	
	public static String runToString(Run run) {
		if (run == null) return "";
		Job<?, ?> job = run.getParent();
		String convertedValue = job.getName() + "#" + run.getNumber();
		return convertedValue;
	}

	public static Run stringToRun(String id) {
		if ("".equals(id)) return null;
		
		int hash = id.lastIndexOf('#');
		String jobName = id.substring(0, hash);
		String runNumber = id.substring(hash + 1);
		Hudson hudson = Hudson.getInstance();
		if (hudson == null) return null; // in simple unit test
		Job<?, ?> job = (Job<?, ?>) hudson.getItemMap().get(jobName);
		if (job == null) return null;
		Run<?, ?> run = job.getBuildByNumber(Integer.parseInt(runNumber));

		return run;

	}

    public static final class ConverterImpl implements Converter {
        public ConverterImpl() {
        }

        public boolean canConvert(Class type) {
            return type==RunWrapper.class;
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            RunWrapper src = (RunWrapper) source;
            writer.setValue(runToString(src.run));
        }

        public Object unmarshal(HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return stringToRun(reader.getValue());
        }
    }
}
