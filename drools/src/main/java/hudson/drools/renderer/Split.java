/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

public class Split extends RendererNode {

	private Color color = new Color(70, 130, 180);

	public Split(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}

	@Override
	public Shape getShape() {
		return new Ellipse2D.Double(x, y, width, height);
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public Image getImage() {
		return RendererConstants.joinImage;
	}

    @Override
    public String getImageURL() {
        return "/plugin/drools/icons/join.gif";
    }
}