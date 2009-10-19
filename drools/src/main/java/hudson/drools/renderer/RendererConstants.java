package hudson.drools.renderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;

public class RendererConstants {

    private static Toolkit toolkit = Toolkit.getDefaultToolkit();

    public static final Color BUILD_CANCELED_COLOR = new Color(200, 200, 200);
    public static final Color BUILD_FAILED_COLOR = new Color(220, 80, 80);
    public static final Color BUILD_SUCCESS_COLOR = new Color(110, 110, 255);
    public static final Color BUILD_UNSTABLE_COLOR = new Color(246, 248, 64);
    public static final Image endImage = toolkit.getImage(RendererConstants.class
            .getResource("/icons/process_stop.gif"));
    public static final Image eventImage = toolkit.getImage(RendererConstants.class
            .getResource("/icons/event.gif"));
    public static final Font FONT = new Font("Arial", Font.PLAIN, 11);
    public static final Image humanTaskImage = toolkit
            .getImage(RendererConstants.class
                    .getResource("/icons/human_task.gif"));
    public static final Image joinImage = toolkit.getImage(RendererConstants.class
            .getResource("/icons/join.gif"));
    public static Color LINE_COLOR = Color.BLACK;
    public static final Color NODE_COMPLETE_COLOR = new Color(132, 217, 50);
    public static final Color NODE_IN_PROGRESS_COLOR = new Color(193, 247, 160);
    public static final Image scriptImage = toolkit.getImage(RendererConstants.class
            .getResource("/icons/open.gif"));
    public static final Image startImage = toolkit.getImage(RendererConstants.class
            .getResource("/icons/process_start.gif"));
    public static final Color TEXT_COLOR = Color.BLACK;
    public static final Image workItemImage = toolkit
            .getImage(RendererConstants.class.getResource("/icons/dsl.png"));

}
