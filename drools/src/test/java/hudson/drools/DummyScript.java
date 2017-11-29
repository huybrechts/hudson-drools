package hudson.drools;

import java.util.Collections;
import java.util.Map;

public class DummyScript extends Script {
    @Override
    public Map<String, Object> execute() throws Exception {
        getOutput().println("script executed");
        return OK;
    }
}
