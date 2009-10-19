/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Image;

public class Event extends RendererNode {

	public Event(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}

	@Override
	public Image getImage() {
		return RendererConstants.eventImage;
	}

    @Override
    public String getImageURL() {
        return "/plugin/drools/icons/event.gif";
    }
}