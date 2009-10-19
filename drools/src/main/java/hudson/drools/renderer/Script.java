/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Image;

public class Script extends WorkItem {

	public Script(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}

	@Override
	public Image getImage() {
		return RendererConstants.scriptImage;
	}

    @Override
    public String getImageURL() {
        return "/plugin/drools/icons/open.gif";
    }
}