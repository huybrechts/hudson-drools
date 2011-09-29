package hudson.drools;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;

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

	private Run<?, ?> run;
    private String jobName;
    private int buildNumber;

	public RunWrapper(Run<?, ?> run) {
		super();
		this.run = run;
        jobName = run.getParent().getName();
        buildNumber = run.getNumber();
	}

    public RunWrapper(String id) {
        setRunId(id);
    }

    private void setRunId(String id) {
        int hash = id.lastIndexOf('#');
        if (hash < 0) {
            //\new IllegalArgumentException("No # in " + id).printStackTrace();
            return;
        }
        jobName = id.substring(0, hash);
        buildNumber = Integer.parseInt(id.substring(hash + 1));
    }

	public RunWrapper() {
	}

	public synchronized Run getRun() {
        if (run == null) {
            run = getRun(jobName, buildNumber);
        }
		return run;
	}

	public String getDisplayName() {
		return toString();
	}

	public Result getResult() {
        Run<?,?> run = getRun();
		return run != null ? getRun().getResult() : Result.NOT_BUILT;
	}

	public boolean isSuccess() {
		return getResult() == Result.SUCCESS;
	}

	public boolean isUnstable() {
		return getResult() == Result.UNSTABLE;
	}

	public String getProjectName() {
		return jobName;
	}
	
	public int getBuildNumber() {
		return buildNumber;
	}

	@Override
	public String toString() {
		return jobName + " #" + buildNumber;
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
		out.writeUTF(jobName + "#" + buildNumber);
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		setRunId(in.readUTF());
	}

	public static Run getRun(String jobName, int buildNumber) {
		Hudson hudson = Hudson.getInstance();
		if (hudson == null)
			return null; // in simple unit test
		Job<?, ?> job = (Job<?, ?>) hudson.getItemMap().get(jobName);
		if (job == null)
			return null;
		Run<?, ?> run = job.getBuildByNumber(buildNumber);

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
			writer.setValue(src.jobName + "#" + src.buildNumber);
		}

		public Object unmarshal(HierarchicalStreamReader reader,
				final UnmarshallingContext context) {
			String id = reader.getValue();
			return new RunWrapper(id);
		}
	}

    public static class NullRunWrapper extends RunWrapper {
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
         return "<unknown>";
 }

 @Override
 public String toString() {
                 return "<unknown>";
 }
        public void writeExternal(ObjectOutput out) throws IOException {
        }

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
        }
    }

}
