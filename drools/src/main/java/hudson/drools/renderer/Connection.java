/**
 * 
 */
package hudson.drools.renderer;

public class Connection {
	public final RendererNode from;
    public final RendererNode to;

    public Connection(RendererNode from, RendererNode to) {
		super();
		this.from = from;
		this.to = to;
	}
}