package hudson.drools.renderer;

import hudson.drools.NodeInstanceLog;
import hudson.drools.WorkItemAction;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleFlowRenderer {

	private static final Pattern BENDPOINT_PATTERN = Pattern.compile("\\[(?:(\\d+),(\\d+))(?:;(\\d+),(\\d+))*\\]");
	
    private Map<String, RendererNode> nodes = new HashMap<String, RendererNode>();
    private List<Connection> connections = new ArrayList<Connection>();
    private List<Connection> compositeConnections = new ArrayList<Connection>();

    private int width, height;

    // private List<NodeInstanceLog> logs;

    public RuleFlowRenderer(String xml) {
        try {
            readResource(new SAXReader().read(new StringReader(xml)));
        } catch (DocumentException e) {
            throw new IllegalArgumentException("Cannot parse workflow xml");
        }
    }

    public RuleFlowRenderer(String xml, List<NodeInstanceLog> logs) {
        this(xml);
        // this.logs = logs;

        for (NodeInstanceLog log : logs) {
            RendererNode node = nodes.get(log.getNodeId());
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

    static Job getJobUrl(String projectName) {
    	if (projectName == null) return null;
    	if (Hudson.getInstance() == null) return null;
        return (Job) Hudson.getInstance().getItem(projectName);
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
        it = root.element("connections").elementIterator();
        while (it.hasNext()) {
            Element el = (Element) it.next();
            String bendPointsVal = el.attributeValue("bendpoints");
            if (bendPointsVal != null) {
            	int[][] bendPoints = parseBendPoints(bendPointsVal, 0, 0);
            	if (bendPoints != null)
            	for (int i = 0; i< bendPoints.length; i++) {
            		minX = Math.min(minX, bendPoints[i][0]);
            		minY = Math.min(minY, bendPoints[i][1]);
            		maxX = Math.max(maxX, bendPoints[i][0]);
            		maxY = Math.max(maxY, bendPoints[i][1]);
            	}
            }
        }

        int offsetX = minX - 5;
        int offsetY = minY - 5;

        it = root.element("nodes").elementIterator();
        while (it.hasNext()) {
            Element el = (Element) it.next();
            RendererNode node = createNode(el, offsetX, offsetY);
            nodes.put(node.id, node);
        }

        width = maxX - minX + 10;
        height = maxY - minY + 10;

        it = root.element("connections").elementIterator();
        while (it.hasNext()) {
            Element el = (Element) it.next();
            String from = el.attributeValue("from");
            String to = el.attributeValue("to");
            String bendPointsVal = el.attributeValue("bendpoints");
            int[][] bendPoints = null;
            if (bendPointsVal != null) {
            	bendPoints = parseBendPoints(bendPointsVal, offsetX, offsetY);
            }
            connections.add(new Connection(nodes.get(from), nodes.get(to), bendPoints));
        }
    }

	private int[][] parseBendPoints(String bendPointsVal, int offsetX,
			int offsetY) {
		Matcher m = BENDPOINT_PATTERN.matcher(bendPointsVal);
		if (!m.matches()) {
			throw new IllegalArgumentException("badly formatted bendpoints: " + bendPointsVal);
		}

		bendPointsVal = bendPointsVal.substring(1, bendPointsVal.length() - 1);
		String[] ss = bendPointsVal.split("[,;]");
		int[][] result = new int[ss.length / 2][];
		for (int i = 0; i < result.length; i++) {
			result[i] = new int [] { 
					Integer.parseInt(ss[i*2]) - offsetX,
					Integer.parseInt(ss[i*2+1]) - offsetY};
		}

		return result;
	}
    
    
    

    private RendererNode createNode(Element el, int offsetX, int offsetY) {
        String type = el.getName();
        String name = el.attributeValue("name");
        String id = el.attributeValue("id");
        int x = Integer.parseInt(el.attributeValue("x")) - offsetX;
        int y = Integer.parseInt(el.attributeValue("y")) - offsetY;
        int width = el.attributeValue("width") != null ? Integer.parseInt(el
                .attributeValue("width")) : 80;
        int height = el.attributeValue("height") != null ? Integer.parseInt(el
                .attributeValue("height")) : 40;
        RendererNode node;
        if ("workItem".equals(type)) {
            String workName = el.element("work").attributeValue("name");
            if ("Script".equals(workName)) {
                node = new Script(type, name, id, x, y, width, height);
            } else if ("Build".equals(workName)) {
                Iterator<Element> eit = el.element("work").elementIterator();
                String project = null;
                while (eit.hasNext()) {
                    Element param = eit.next();
                    if ("Project".equals(param.attributeValue("name"))) {
                        project = param.elementText("value");
                    }
                }
                node = new Build(type, name, id, project, x, y, width, height);
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
                RendererNode child = createNode(e, -x, -y);
                nodes.put(node.id + ":2:" + child.id, child);
            }
            it = el.element("connections").elementIterator();
            while (it.hasNext()) {
                Element conn = (Element) it.next();
                String from = node.id + ":2:" + conn.attributeValue("from");
                String to = node.id + ":2:" + conn.attributeValue("to");
                String bendPointsVal = el.attributeValue("bendpoints");
                int[][] bendPoints = parseBendPoints(bendPointsVal, offsetX,
    					offsetY);
                compositeConnections.add(new Connection(nodes.get(from), nodes
                        .get(to), bendPoints));
            }
        } else {
            node = new RendererNode(type, name, id, x, y, width, height);
        }
        return node;
    }

    public Collection<RendererNode> getNodes() {
        return nodes.values();
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void paint(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        for (Connection connection : connections) {
            paintConnection(g2, connection);
        }

        for (RendererNode node : nodes.values()) {
            if (node instanceof ForEach)
                node.paint(g2);
        }

        for (Connection connection : compositeConnections) {
        	paintConnection(g2, connection);
        }

        for (RendererNode node : nodes.values()) {
            if (!(node instanceof ForEach))
                node.paint(g2);
        }
    }

	private void paintConnection(Graphics2D g2, Connection connection) {
		Rectangle2D.Double fromRect = connection.from.getRectangle();
		Rectangle2D.Double toRect = connection.to.getRectangle();
		int bendPoints[][] = connection.bendPoints;
		if (bendPoints == null) {
			GraphicsUtil.paintLine(g2, point(fromRect), point(toRect));
		} else {
			GraphicsUtil.paintLine(g2, point(fromRect), point(bendPoints[0]));
			
			for (int i = 0; i < bendPoints.length - 1; i++) {
				GraphicsUtil.paintLine(g2, point(bendPoints[i]), point(bendPoints[i+1]));
			}
			
			GraphicsUtil.paintLine(g2, point(bendPoints[bendPoints.length - 1]), point(toRect) );
		}
	}
    
    private Point2D.Double point(int[] xy) {
    	return new Point2D.Double(xy[0], xy[1]);
    }
    private Point2D.Double point(Rectangle2D.Double rect) {
    	return new Point2D.Double(rect.getCenterX(), rect.getCenterY());
    }

    public String getNodeName(String id) {
        RendererNode node = nodes.get(id);
        return node != null ? node.name : null;
    }

    public void write(OutputStream output) throws IOException {
        BufferedImage aimg = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = aimg.createGraphics();
        paint(g);
        g.dispose();
        ImageIO.write(aimg, "png", output);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void writeSVG(ServletOutputStream output) throws IOException {
        output
                .print("<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' width='"
                        + getWidth() + "' height='" + getHeight() + "'>");
        output.println("<g>");

        for (Connection connection : connections) {
            Rectangle2D.Double fromRect = connection.from.getRectangle();
            Rectangle2D.Double toRect = connection.to.getRectangle();

            output.println("<line x1='" + fromRect.getCenterX() + "' y1='"
                    + fromRect.getCenterY() + "' x2='" + toRect.getCenterX()
                    + "' y2='" + toRect.getCenterY()
                    + "' style='stroke:rgb(0,0,0);stoke-width:1'/>");

        }

        for (RendererNode node : nodes.values()) {
            // if (node instanceof ForEach) node.paint(g2);
        }

        for (Connection connection : compositeConnections) {
            Rectangle2D.Double fromRect = connection.from.getRectangle();
            Rectangle2D.Double toRect = connection.to.getRectangle();
            output.println("<line x1='" + fromRect.getCenterX() + "' y1='"
                    + fromRect.getCenterY() + "' x2='" + toRect.getCenterX()
                    + "' y2='" + toRect.getCenterY() + "'");
        }

        for (RendererNode node : nodes.values()) {
            // if (!(node instanceof ForEach)) node.paint(g2);
            output
                    .println("<rect x='"
                            + node.x
                            + "' y='"
                            + node.y
                            + "' width='"
                            + node.width
                            + "' height='"
                            + node.height
                            + "' style='fill:rgb(255,255,255);stroke-width:1;stroke:rgb(0,0,0)'/>");
            output.println("<text x='" + (node.x + 16) + "' y='"
                    + (node.y + (node.height / 2) + 4) + "' font-size='11'>"
                    + node.name + "</text>");
            output
                    .println("<image x='"
                            + node.x
                            + "' y='"
                            + (node.y + (node.height / 2) - 8)
                            + "' width='16' height='16' xlink:href='/plugin/drools/icons/event.gif'/>");
        }

        output.println("</g>");
        output.println("</svg>");

    }
    
    public static void main(String[] args) throws Exception {
		URL url = RuleFlowRenderer.class.getResource("/hudson/drools/SimpleProjectTest-1.rf");
		String xml = IOUtils.toString(url.openStream());
		final RuleFlowRenderer r = new RuleFlowRenderer(xml);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				r.paint((Graphics2D) g);
			}
			
			@Override
			public Dimension getMinimumSize() {
				return new Dimension(r.getWidth(), r.getHeight());
			}
		};
		frame.setContentPane(panel);
		
		frame.setSize(650, 480);
		frame.setVisible(true);
		
		
	}

}
