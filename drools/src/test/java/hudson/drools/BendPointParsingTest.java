package hudson.drools;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class BendPointParsingTest extends TestCase{

	public void test() {
		
		String s = "[1,2;3,4;5,6]";
		Matcher m = Pattern.compile("\\[(?:(\\d+),(\\d+))(?:;(\\d+),(\\d+))*\\]").matcher(s);
		
		System.out.println(m.groupCount());
		
		s = s.substring(1, s.length() - 1);
		String[] ss = s.split("[,;]");
		System.out.println(Arrays.asList(ss));
		int[][] result = new int[ss.length / 2][];
		for (int i = 0; i < result.length; i++) {
			result[i] = new int [] { Integer.parseInt(ss[i*2],Integer.parseInt(ss[i*2+1]))};
		}
		
		
	}
	
}
