/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Image;

public class Start extends RendererNode {

	public Start(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}

	@Override
	public Image getImage() {
		return RendererConstants.startImage;
	}
	
    @Override
    public String getImageURL() {
        return "/plugin/drools/icons/process_start.gif";
    }


}