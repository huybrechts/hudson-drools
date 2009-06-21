package hudson.drools;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class RuleFlowRenderer {

	private static Color LINE_COLOR = Color.BLACK;
	private static final Color TEXT_COLOR = Color.BLACK;
	private static final Font FONT = new Font("Arial", Font.PLAIN, 11);

	public static final Color NODE_IN_PROGRESS_COLOR = new Color(193, 247, 160);
	public static final Color NODE_COMPLETE_COLOR = new Color(132, 217, 50);
	public static final Color BUILD_SUCCESS_COLOR = new Color(110, 110, 255);
	public static final Color BUILD_FAILED_COLOR = new Color(220, 80, 80);
	public static final Color BUILD_CANCELED_COLOR = new Color(200, 200, 200);
	public static final Color BUILD_UNSTABLE_COLOR = new Color(246, 248, 64);

	private Map<String, RFNode> nodes = new HashMap<String, RFNode>();
	private List<RFConnection> connections = new ArrayList<RFConnection>();
	private List<RFConnection> compositeConnections = new ArrayList<RFConnection>();

	private int width, height;
//	private List<NodeInstanceLog> logs;

	public RuleFlowRenderer(String xml) {
		try {
			readResource(new SAXReader().read(new StringReader(xml)));
		} catch (DocumentException e) {
			throw new IllegalArgumentException("Cannot parse workflow xml");
		}
	}

	public RuleFlowRenderer(String xml, List<NodeInstanceLog> logs) {
		this(xml);
//		this.logs = logs;

		for (NodeInstanceLog log : logs) {
			RFNode node = nodes.get(log.getNodeId());
			if (node == null) {
				System.out.println("unknown node for " + log);
				continue;
			} 
			if (log.getType() == NodeInstanceLog.TYPE_ENTER) {
				node.state = NodeState.IN_PROGRESS;
			} else if (log.getType() == NodeInstanceLog.TYPE_EXIT) {
				node.state = NodeState.COMPLETED;
			}
			if (node instanceof Build) {
				String projectName = ((Build) node).project;
				Job project = getJobUrl(projectName);
				if (project != null) {
					Run run = WorkItemAction.findRun(project, log
							.getProcessInstanceId());
					if (run != null) {
						((Build) node).run = run;
					}
				}
			}
		}
	}

	private Job getJobUrl(String projectName) {
		return (Hudson.getInstance() != null) ? (Job) Hudson.getInstance()
				.getItem(projectName) : null;
	}

	private void readResource(Document document) throws DocumentException {

		int maxX = 0;
		int maxY = 0;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;

		Element root = document.getRootElement();

		Iterator it = root.element("nodes").elementIterator();
		while (it.hasNext()) {
			Element el = (Element) it.next();
			int x = Integer.parseInt(el.attributeValue("x"));
			int y = Integer.parseInt(el.attributeValue("y"));
			int width = Integer.parseInt(el.attributeValue("width"));
			int height = Integer.parseInt(el.attributeValue("height"));
			maxX = Math.max(maxX, x + width);
			maxY = Math.max(maxY, y + height);
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
		}

		int offsetX = minX - 5;
		int offsetY = minY - 5;

		it = root.element("nodes").elementIterator();
		while (it.hasNext()) {
			Element el = (Element) it.next();
			RFNode node = createNode(el, offsetX, offsetY);
			nodes.put(node.id, node);
		}

		width = maxX - minX + 10;
		height = maxY - minY + 10;

		it = root.element("connections").elementIterator();
		while (it.hasNext()) {
			Element el = (Element) it.next();
			String from = el.attributeValue("from");
			String to = el.attributeValue("to");
			connections.add(new RFConnection(nodes.get(from), nodes.get(to)));
		}
	}

	private RFNode createNode(Element el, int offsetX, int offsetY) {
		String type = el.getName();
		String name = el.attributeValue("name");
		String id = el.attributeValue("id");
		int x = Integer.parseInt(el.attributeValue("x")) - offsetX;
		int y = Integer.parseInt(el.attributeValue("y")) - offsetY;
		int width = el.attributeValue("width") != null ? Integer.parseInt(el.attributeValue("width")) : 80;
		int height = el.attributeValue("height") != null ? Integer.parseInt(el.attributeValue("height")) : 40;
		RFNode node;
		if ("workItem".equals(type)) {
			String workName = el.element("work").attributeValue("name");
			if ("Script".equals(workName)) {
				node = new Script(type, name, id, x, y, width, height);
			} else if ("Build".equals(workName)) {
				Iterator<Element> eit = el.element("work")
						.elementIterator();
				String project = null;
				while (eit.hasNext()) {
					Element param = eit.next();
					if ("Project".equals(param.attributeValue("name"))) {
						project = param.elementText("value");
					}
				}
				node = new Build(type, name, id, project, x, y, width,
						height);
			} else {
				node = new WorkItem(type, name, id, x, y, width, height);
			}
		} else if ("humanTask".equals(type)) {
			node = new HumanTask(type, name, id, x, y, width, height);
		} else if ("start".equals(type)) {
			node = new Start(type, name, id, x, y, width, height);
		} else if ("end".equals(type)) {
			node = new End(type, name, id, x, y, width, height);
		} else if ("split".equals(type)) {
			node = new Split(type, name, id, x, y, width, height);
		} else if ("join".equals(type)) {
			node = new Split(type, name, id, x, y, width, height);
		} else if ("eventNode".equals(type)) {
			node = new Event(type, name, id, x, y, width, height);
		} else if ("forEach".equals(type)) {
			node = new ForEach(type, name, id, x, y, width, height);
			Iterator it = el.element("nodes").elementIterator();
			while (it.hasNext()) {
				Element e = (Element) it.next();
				RFNode child = createNode(e, -x, -y);
				nodes.put(node.id + ":2:" + child.id, child);
			}
			it = el.element("connections").elementIterator();
			while (it.hasNext()) {
				Element conn = (Element) it.next();
				String from = node.id  + ":2:" + conn.attributeValue("from");
				String to = node.id + ":2:" + conn.attributeValue("to");
				compositeConnections.add(new RFConnection(nodes.get(from), nodes.get(to)));
			}
		} else {
			node = new RFNode(type, name, id, x, y, width, height);
		}
		return node;
	}

	public Collection<RFNode> getNodes() {
		return nodes.values();
	}

	public void paint(Graphics2D g2) {
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, getWidth(), getHeight());

		for (RFConnection connection : connections) {
			Rectangle2D.Double fromRect = connection.from.asRectangle();
			Rectangle2D.Double toRect = connection.to.asRectangle();
			paintLine(g2, fromRect, toRect);
		}

		for (RFNode node : nodes.values()) {
			if (node instanceof ForEach) node.paint(g2);
		}

		for (RFConnection connection : compositeConnections) {
			Rectangle2D.Double fromRect = connection.from.asRectangle();
			Rectangle2D.Double toRect = connection.to.asRectangle();
			paintLine(g2, fromRect, toRect);
		}
		
		for (RFNode node : nodes.values()) {
			if (!(node instanceof ForEach)) node.paint(g2);
		}
	}

	public static void paintLine(Graphics2D g2, Rectangle2D.Double from,
			Rectangle2D.Double to) {

		Point2D.Double fromRectCenter = new Point2D.Double(from.getCenterX(),
				from.getCenterY());
		Point2D.Double toRectCenter = new Point2D.Double(to.getCenterX(), to
				.getCenterY());
		Line2D.Double line = new Line2D.Double(fromRectCenter, toRectCenter);

		Double p1 = new Point2D.Double();
		GraphicsUtil.getLineRectangleIntersection(from, line, p1);
		Double p2 = new Point2D.Double();
		GraphicsUtil.getLineRectangleIntersection(to, line, p2);

		// drawArrow(g2, new Line2D.Double(p1,p2), 1, true);
		drawArrow(g2, line, 1, true);

	}

	public static void drawArrow(Graphics2D g2d, Line2D.Double line,
			float stroke, boolean arrow) {
		int xCenter = (int) line.getX1();
		int yCenter = (int) line.getY1();
		double x = line.getX2();
		double y = line.getY2();
		double aDir = Math.atan2(xCenter - x, yCenter - y);
		int i1 = 12 + (int) (stroke * 2);
		int i2 = 6 + (int) stroke; // make the arrow head the same size

		Line2D.Double base = new Line2D.Double(x + xCor(i1, aDir + .5), y
				+ yCor(i1, aDir + .5), x + xCor(i1, aDir - .5), y
				+ yCor(i1, aDir - .5));
		Point2D.Double intersect = new Point2D.Double();
		GraphicsUtil.getLineLineIntersection(line, base, intersect);

		g2d.setPaint(LINE_COLOR);
		if (arrow) {
			g2d.draw(new Line2D.Double(xCenter, yCenter, intersect.x,
					intersect.y));

			g2d.setStroke(new BasicStroke(1f)); // make the arrow head solid
			// even if
			// dash pattern has been specified
			Polygon tmpPoly = new Polygon();
			// regardless of the length
			tmpPoly.addPoint((int) x, (int) y); // arrow tip
			tmpPoly.addPoint((int) x + xCor(i1, aDir + .5), (int) y
					+ yCor(i1, aDir + .5));
			// tmpPoly.addPoint(x + xCor(i2, aDir), y + yCor(i2, aDir));
			tmpPoly.addPoint((int) x + xCor(i1, aDir - .5), (int) y
					+ yCor(i1, aDir - .5));
			tmpPoly.addPoint((int) x, (int) y); // arrow tip
			g2d.drawPolygon(tmpPoly);
		} else {
			g2d.draw(new Line2D.Double(xCenter, yCenter, x, y));
		}
		// g2d.setPaint(Color.WHITE);
	}

	private static int yCor(int len, double dir) {
		return (int) (len * Math.cos(dir));
	}

	private static int xCor(int len, double dir) {
		return (int) (len * Math.sin(dir));
	}

	public String getNodeName(String id) {
		RFNode node = nodes.get(id);
		return node != null ? node.name : null;
	}

	public class RFNode {
		public final String type, name, id;
		public final int x, y, width, height;

		public NodeState state = NodeState.INACTIVE;
		private String url;

		public RFNode(String type, String name, String id, int x, int y,
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
			return url;
		}

		public Rectangle2D.Double asRectangle() {
			return new Rectangle2D.Double(x, y, width, height);
		}

		public Shape getShape() {
			return asRectangle();
		}

		public Image getImage() {
			return null;
		}

		public Color getColor() {
			return Color.WHITE;
		}

		public Color getStateColor() {
			switch (state) {
			case IN_PROGRESS:
				return NODE_IN_PROGRESS_COLOR;
			case INACTIVE:
				return null;
			case COMPLETED:
				return NODE_COMPLETE_COLOR;
			default:
				return null;
			}
		}

		public void paint(Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			Rectangle2D.Double rect = asRectangle();

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
			g2.setPaint(LINE_COLOR);
			g2.draw(shape);

			int imageY = (int) (rect.y + rect.height / 2 - 16 / 2);
			int imageX = (int) rect.x + 6;

			boolean painted = paintIcon(g2, imageX, imageY);
			int textWidth = g2.getFontMetrics().stringWidth(name);
			int textHeight = g2.getFontMetrics().getAscent();

			g2.setColor(TEXT_COLOR);
			g2.setFont(FONT);

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

	public class WorkItem extends RFNode {

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
			return workItemImage;
		}

	}

	public class HumanTask extends WorkItem {

		public HumanTask(String type, String name, String id, int x, int y,
				int width, int height) {
			super(type, name, id, x, y, width, height);
		}

		@Override
		public Image getImage() {
			return humanTaskImage;
		}

	}

	public class Build extends WorkItem {

		public String project;
		private Run run;

		public Build(String type, String name, String id, String project,
				int x, int y, int width, int height) {
			super(type, name, id, x, y, width, height);
			this.project = project;
		}

		@Override
		public Image getImage() {
			return workItemImage;
		}

		@Override
		public boolean paintIcon(Graphics2D g2, int x, int y) {
			if (run == null || run.getResult() == null) {
				return super.paintIcon(g2, x, y);
			}
			Color c;
			if (run.getResult() == Result.SUCCESS) {
				c = BUILD_SUCCESS_COLOR;
			} else if (run.getResult() == Result.UNSTABLE) {
				c = BUILD_UNSTABLE_COLOR;
			} else if (run.getResult() == Result.ABORTED) {
				c = BUILD_CANCELED_COLOR;
			} else {
				c = BUILD_FAILED_COLOR;
			}
			g2.translate(x, y);
			paintBall(g2, c);
			g2.translate(-x, -y);

			return true;
		}

		@Override
		public String getUrl() {
			if (run != null) {
				return run.getUrl();
			}
			
			Job job = getJobUrl(project);
			if (job != null) {
				return job.getUrl();
			}
			
			return null;
		}

	}

	public class Script extends WorkItem {

		public Script(String type, String name, String id, int x, int y,
				int width, int height) {
			super(type, name, id, x, y, width, height);
		}

		@Override
		public Image getImage() {
			return scriptImage;
		}

	}

	public class Split extends RFNode {

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
			return joinImage;
		}

	}

	public class Event extends RFNode {

		public Event(String type, String name, String id, int x, int y,
				int width, int height) {
			super(type, name, id, x, y, width, height);
		}

		@Override
		public Image getImage() {
			return eventImage;
		}

	}

	public class Start extends RFNode {

		public Start(String type, String name, String id, int x, int y,
				int width, int height) {
			super(type, name, id, x, y, width, height);
		}

		@Override
		public Image getImage() {
			return startImage;
		}

	}

	public class End extends RFNode {

		public End(String type, String name, String id, int x, int y,
				int width, int height) {
			super(type, name, id, x, y, width, height);
		}

		@Override
		public Image getImage() {
			return endImage;
		}

	}
	
	public class ForEach extends RFNode {

		public ForEach(String type, String name, String id, int x, int y,
				int width, int height) {
			super(type, name, id, x, y, width, height);
		}
		
		@Override
		public void paint(Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			Rectangle2D.Double rect = asRectangle();

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
			g2.setPaint(LINE_COLOR);
			g2.draw(shape);

			g2.setColor(TEXT_COLOR);
			g2.setFont(FONT);

			g2
					.drawString(
							name,
							(int) (rect.x + 6),
							(int) (rect.y + 18));

		}


	}

	private static class RFConnection {
		private final RFNode from, to;

		public RFConnection(RFNode from, RFNode to) {
			super();
			this.from = from;
			this.to = to;
		}
	}

	private static Toolkit toolkit = Toolkit.getDefaultToolkit();
	private static Image endImage = toolkit.getImage(RuleFlowRenderer.class
			.getResource("/icons/process_stop.gif"));
	private static Image startImage = toolkit.getImage(RuleFlowRenderer.class
			.getResource("/icons/process_start.gif"));
	private static Image workItemImage = toolkit
			.getImage(RuleFlowRenderer.class.getResource("/icons/dsl.png"));
	private static Image joinImage = toolkit.getImage(RuleFlowRenderer.class
			.getResource("/icons/join.gif"));
	private static Image eventImage = toolkit.getImage(RuleFlowRenderer.class
			.getResource("/icons/event.gif"));
	private static Image scriptImage = toolkit.getImage(RuleFlowRenderer.class
			.getResource("/icons/open.gif"));
	private static Image humanTaskImage = toolkit
			.getImage(RuleFlowRenderer.class
					.getResource("/icons/human_task.gif"));

	public void write(OutputStream output) throws IOException {
		BufferedImage aimg = new BufferedImage(getWidth(), getHeight(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = aimg.createGraphics();
		paint(g);
		g.dispose();
		ImageIO.write(aimg, "png", output);
	}

	enum NodeState {
		IN_PROGRESS, COMPLETED, INACTIVE, FAILED
	};

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public static void paintBall(Graphics2D g2, Color c) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		int diameter = 16;

		// Retains the previous state
		Paint oldPaint = g2.getPaint();

		// Fills the circle with solid blue color
		g2.setColor(c);
		g2.fillOval(0, 0, diameter - 1, diameter - 1);

		// Adds shadows at the top
		Paint p;
		p = new GradientPaint(0, 0, new Color(0.0f, 0.0f, 0.0f, 0.4f), 0,
				diameter, new Color(0.0f, 0.0f, 0.0f, 0.0f));
		g2.setPaint(p);
		g2.fillOval(0, 0, diameter - 1, diameter - 1);

		// Adds highlights at the bottom
		p = new GradientPaint(0, 0, new Color(1.0f, 1.0f, 1.0f, 0.0f), 0,
				diameter, new Color(1.0f, 1.0f, 1.0f, 0.0f));
		g2.setPaint(p);
		g2.fillOval(0, 0, diameter - 1, diameter - 1);

		// Creates dark edges for 3D effect
		p = new RadialGradientPaint(new Point2D.Double(diameter * .4,
				diameter * .45), diameter / 2.0f, new float[] { 0.0f, 0.95f },
				new Color[] {
						new Color(c.getRed(), c.getGreen(), c.getBlue(), 127),
						new Color(0.0f, 0.0f, 0.0f, 0.0f) });
		g2.setPaint(p);
		g2.fillOval(0, 0, diameter - 1, diameter - 1);

		// Adds oval inner highlight at the bottom
		p = new RadialGradientPaint(new Point2D.Double(diameter / 2.0,
				diameter * 1.5), diameter / 2.3f, new Point2D.Double(
				diameter / 2.0, diameter * 1.75 + 6),
				new float[] { 0.0f, 0.8f }, new Color[] {
						new Color(c.getRed(), c.getGreen(), c.getBlue(), 255),
						new Color(c.getRed(), c.getGreen(), c.getBlue(), 0) },
				RadialGradientPaint.CycleMethod.NO_CYCLE,
				RadialGradientPaint.ColorSpaceType.SRGB, AffineTransform
						.getScaleInstance(1.0, 0.5));
		g2.setPaint(p);
		g2.fillOval(0, 0, diameter - 1, diameter - 1);

		// Restores the previous state
		g2.setPaint(oldPaint);
	}

}
