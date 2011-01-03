/**
 * 
 */
package hudson.drools.renderer;

import java.util.regex.Pattern;

public class Connection {
	
	public final RendererNode from;
    public final RendererNode to;

	public final int[][] bendPoints;

    public Connection(RendererNode from, RendererNode to, int[][] bendPoints) {
		super();
		this.from = from;
		this.to = to;
		this.bendPoints = bendPoints;
	}
    
}