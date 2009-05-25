package hudson.drools.eclipse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class Util {

	public static String read(Reader r) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(r);
			String line = null;
			StringBuilder content = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}

			reader.close();

			return content.toString();
		} finally {
			r.close();
		}
	}

	public static String read(InputStream r) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(r));
			String line = null;
			StringBuilder content = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}

			return content.toString();
		} finally {
			r.close();
		}
	}
	
	public static void write(Writer w, String content) throws IOException {
		w.write(content);
		w.flush();
		w.close();
	}
	
	public static void write(OutputStream os, String content) throws IOException {
		os.write(content.getBytes());
		os.flush();
		os.close();
	}
}
