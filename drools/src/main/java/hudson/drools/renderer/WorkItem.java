/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

public class WorkItem extends RendererNode {

	private Color color = new Color(255, 250, 205);

	public WorkItem(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}

	@Override
	public Shape getShape() {
		return new RoundRectangle2D.Double(x, y, width, height, 25, 25);
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public Image getImage() {
		return RendererConstants.workItemImage;
	}

    @Override
    public String getImageURL() {
        return "/plugin/drools/icons/dsl.png";
    }

}