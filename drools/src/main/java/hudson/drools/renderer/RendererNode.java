/**
 * 
 */
package hudson.drools.renderer;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

public class RendererNode {
	public final String type, name, id;
	public final int x, y, width, height;

	public NodeState state = NodeState.INACTIVE;

	public RendererNode(String type, String name, String id, int x, int y,
			int width, int height) {
		super();
		this.type = type;
		this.name = name;
		this.id = id;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public String getUrl() {
		return null;
	}

	public Rectangle2D.Double getRectangle() {
		return new Rectangle2D.Double(x, y, width, height);
	}

	public Shape getShape() {
		return getRectangle();
	}

	public Image getImage() {
		return null;
	}
	
	public String getImageURL() {
	    return null;
	}

	public Color getColor() {
		return Color.WHITE;
	}

	public Color getStateColor() {
		switch (state) {
		case IN_PROGRESS:
			return RendererConstants.NODE_IN_PROGRESS_COLOR;
		case INACTIVE:
			return null;
		case COMPLETED:
			return RendererConstants.NODE_COMPLETE_COLOR;
		default:
			return null;
		}
	}

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

		int imageY = (int) (rect.y + rect.height / 2 - 16 / 2);
		int imageX = (int) rect.x + 6;

		boolean painted = paintIcon(g2, imageX, imageY);
		int textWidth = g2.getFontMetrics().stringWidth(name);
		int textHeight = g2.getFontMetrics().getAscent();

		g2.setColor(RendererConstants.TEXT_COLOR);
		g2.setFont(RendererConstants.FONT);

		g2
				.drawString(
						name,
						(int) (rect.x + (painted ? 10 : 0) + (rect.width - textWidth) / 2),
						(int) (rect.y + (rect.height + textHeight) / 2));

	}

	public boolean paintIcon(Graphics2D g2, int imageX, int imageY) {
		Image image = getImage();
		if (image != null) {
			g2.drawImage(image, imageX, imageY, null);
		}
		return image != null;
	}

}