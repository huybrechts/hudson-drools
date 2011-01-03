/* 
 * Copyright 2008 Tom Huybrechts and hudson.dev.java.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.  
 * 
 */
package hudson.drools.renderer;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class GraphicsUtil {

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

	public static void paintLine(Graphics2D g2, Point2D.Double from,
	        Point2D.Double to) {
	    Line2D.Double line = new Line2D.Double(from, to);
	    g2.setColor(Color.BLACK);
	    g2.draw(line);
	}

}
