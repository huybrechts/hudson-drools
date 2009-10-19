/**
 * 
 */
package hudson.drools.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

public class ForEach extends RendererNode {

	public ForEach(String type, String name, String id, int x, int y,
			int width, int height) {
		super(type, name, id, x, y, width, height);
	}
	
	@Override
	public void paint(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		Rectangle2D.Double rect = getRectangle();

		Shape shape = getShape();

		g2.setPaint(getColor());

		g2.fill(shape);

		Shape clip = g2.getClip();

		Color stateColor = getStateColor();
		if (stateColor != null) {
			g2.setPaint(stateColor);
			g2.setClip((int) rect.x, (int) rect.y, 25, (int) rect.height);
			g2.fill(shape);
		}

		g2.setClip(clip);
		g2.setPaint(RendererConstants.LINE_COLOR);
		g2.draw(shape);

		g2.setColor(RendererConstants.TEXT_COLOR);
		g2.setFont(RendererConstants.FONT);

		g2
				.drawString(
						name,
						(int) (rect.x + 6),
						(int) (rect.y + 18));

	}


}