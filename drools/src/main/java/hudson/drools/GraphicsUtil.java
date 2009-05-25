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
package hudson.drools;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class GraphicsUtil {

	public static boolean getLineRectangleIntersection(Rectangle2D.Double rect,
			Line2D.Double line, Point2D.Double intersection) {

		Line2D.Double top = new Line2D.Double(rect.x, rect.y, rect.x
				+ rect.width, rect.y);
		Line2D.Double bottom = new Line2D.Double(rect.x, rect.y + rect.height,
				rect.x + rect.width, rect.y + rect.height);
		Line2D.Double left = new Line2D.Double(rect.x, rect.y, rect.x, rect.y
				+ rect.height);
		Line2D.Double right = new Line2D.Double(rect.x + rect.width, rect.y,
				rect.x + rect.width, rect.y + rect.height);

		return getLineLineIntersection(line, top, intersection)
				|| getLineLineIntersection(line, bottom, intersection)
				|| getLineLineIntersection(line, left, intersection)
				|| getLineLineIntersection(line, right, intersection);
	}

	public static boolean getLineLineIntersection(Line2D.Double l1,
			Line2D.Double l2, Point2D.Double intersection) {
		if (!l1.intersectsLine(l2))
			return false;

		double x1 = l1.getX1(), y1 = l1.getY1(), x2 = l1.getX2(), y2 = l1
				.getY2(), x3 = l2.getX1(), y3 = l2.getY1(), x4 = l2.getX2(), y4 = l2
				.getY2();

		intersection.x = det(det(x1, y1, x2, y2), x1 - x2, det(x3, y3, x4, y4),
				x3 - x4)
				/ det(x1 - x2, y1 - y2, x3 - x4, y3 - y4);
		intersection.y = det(det(x1, y1, x2, y2), y1 - y2, det(x3, y3, x4, y4),
				y3 - y4)
				/ det(x1 - x2, y1 - y2, x3 - x4, y3 - y4);

		return true;
	}

	public static double det(double a, double b, double c, double d) {
		return a * d - b * c;
	}

}
