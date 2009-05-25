package hudson.drools.eclipse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Hudson {
	
	private String userName;
	private String password;

	public Hudson(String url, final String userName, final String password) {
		super();
		this.url = url;
		this.userName = userName;
		this.password = password;

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password.toCharArray());
			}
		});
	}

	private final String url;

	public String getUrl() {
		return url;
	}

	public List<String> getWorkflowProjects() throws IOException {
		URL u = new URL(url + "/plugin/drools/workflowProjects");
		
		List<String> result = new ArrayList<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(u.openStream()));
			String line =null;
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} finally {
			if (reader != null) reader.close();
		}
		
		return result;
		
	}

	public void validateProject(String project) throws IOException,
			NoSuchProjectException, NotADroolsProjectException {
		URL u = new URL(url + "/job/" + project + "/api/xml");
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setRequestProperty("Authorization", "Basic " + Base64Converter.encode(userName + ":" + password));

		String xml = Util.read(conn.getInputStream());

		if (conn.getResponseCode() != 200) {
			throw new NoSuchProjectException("Project '" + project
					+ "' does not exist.", xml);
		}

		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = builder
					.parse(new InputSource(new StringReader(xml)));
			if (!doc.getFirstChild().getNodeName().equals("droolsProject")) {
				throw new NotADroolsProjectException("Project '" + project
						+ "' is not a Drools project.");
			}
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException("Could not read XML from "
					+ u.toExternalForm());
		}

	}

	public void deploy(String project, IFile ruleFlowFile) throws IOException,
			CoreException, NoSuchProjectException, NotADroolsProjectException {
		validateProject(project);

		URL u = new URL(url + "/job/" + project + "/submitWorkflow");
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		addAuthentication(conn, userName, password);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn
				.getOutputStream()));

		String xml = Util.read(ruleFlowFile.getContents());
		writer.write(xml);
		writer.flush();

		writer.close();

		System.out.println(conn.getResponseCode());
	}

	public void create(String project, IFile ruleFlowFile) throws IOException,
			CoreException {

		String projectXml = createProjectXml(project, ruleFlowFile);

		URL u = new URL(url + "/createItem?name="
				+ URLEncoder.encode(project, "UTF-8"));
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setRequestProperty("Authorization", "Basic " + Base64Converter.encode(userName + ":" + password));
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestProperty("Content-Type", "application/xml");
		conn.setRequestMethod("POST");

		Util.write(conn.getOutputStream(), projectXml);

		System.out.println(Util.read(conn.getInputStream()));

		System.out.println("resp: " + conn.getResponseCode());
	}

	private String createProjectXml(String project, IFile ruleFlowFile)
			throws IOException, CoreException,
			TransformerFactoryConfigurationError {
		try {
			String droolsXml = Util.read(ruleFlowFile.getContents());
			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element root = (Element) doc.appendChild(doc
					.createElement("hudson.drools.DroolsProject"));
			root.appendChild(doc.createElement("description")).setTextContent(
					"");
			root.appendChild(doc.createElement("name")).setTextContent(project);
			root.appendChild(doc.createElement("processXML")).setTextContent(
					droolsXml);

			StringWriter writer = new StringWriter();
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			Source source = new DOMSource(doc);
			Result output = new StreamResult(writer);
			transformer.transform(source, output);

			return writer.toString();

		} catch (DOMException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	public static String discover() {
		// get a datagram socket
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(5000); // this is local, so 5000 is very long
										// already...

			// send request
			byte[] buf = new byte[2048];
			InetAddress address = InetAddress.getByName("255.255.255.255");
			DatagramPacket packet = new DatagramPacket(buf, buf.length,
					address, 33848);
			socket.send(packet);

			// get response
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);

			// display response
			String received = new String(packet.getData(), 0, packet
					.getLength());

			String url = XPathFactory.newInstance().newXPath().evaluate(
					"/hudson/url/text()",
					new InputSource(new StringReader(received)));

			return url;
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			return null;
		} finally {
			if (socket != null)
				socket.close();
		}
	}

	public boolean verify() {
		try {
			URL u = new URL(url + "/manage");
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			addAuthentication(conn, userName, password);
			String xml = Util.read(conn.getInputStream());

			return conn.getResponseCode() == 200;
		} catch (Exception e) {
			return false;
		}
		
	}
	
	private void addAuthentication(HttpURLConnection conn, String userName, String password) {
		if (userName != null && password != null)
		conn.setRequestProperty("Authorization", "Basic " + Base64Converter.encode(userName + ":" + password));
	}
	
	public static void main(String[] args) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/manage").openConnection();
		System.out.println(conn.getResponseCode());
		
		// 401 = bad username or password
		// 403 = authentication required
	}
}
