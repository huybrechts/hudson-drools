package hudson.drools;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ForwardToPreviousPage implements HttpResponse {

	public void generateResponse(StaplerRequest req, StaplerResponse rsp,
			Object node) throws IOException, ServletException {
		rsp.forwardToPreviousPage(req);
	}

}
