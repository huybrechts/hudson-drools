package hudson.drools;

import java.util.Map;

public class DummyFailedScript extends Script {
    @Override
    public Map<String, Object> execute() throws Exception {
        throw new Exception();
    }
}
