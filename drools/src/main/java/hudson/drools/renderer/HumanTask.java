/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Image;

public class HumanTask extends WorkItem {

	public HumanTask(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}

	@Override
	public Image getImage() {
		return RendererConstants.humanTaskImage;
	}

    @Override
    public String getImageURL() {
        return "/plugin/drools/icons/human_task.gif";
    }
}