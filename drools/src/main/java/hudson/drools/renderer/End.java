/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Image;

public class End extends RendererNode {

	public End(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}

	@Override
	public Image getImage() {
		return RendererConstants.endImage;
	}

    @Override
    public String getImageURL() {
        return "/plugin/drools/icons/process_stop.gif";
    }
}