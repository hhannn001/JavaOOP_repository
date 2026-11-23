package coderoom.widgets.room;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

/** Converts the transparent pixels of a sprite to a reusable silhouette in room coordinates. */
final class SpriteOutline {
    private SpriteOutline() { }

    static Shape fromAlpha(BufferedImage image, Rectangle target) {
        Path2D runs = new Path2D.Double();
        int width   = image.getWidth();
        int height  = image.getHeight();
        for (int y = 0; y < height; y++) {
            int start = -1;
            for (int x = 0; x <= width; x++) {
                boolean visible = x < width && image.getRGB(x, y) >>> 24 >= 128;
                if (visible && start < 0) {
                    start = x;
                } else if (!visible && start >= 0) {
                    runs.append(new Rectangle(start, y, x - start, 1), false);
                    start = -1;
                }
            }
        }
        AffineTransform placement = AffineTransform.getTranslateInstance(target.x, target.y);
        placement.scale(target.width / (double) width, target.height / (double) height);
        return placement.createTransformedShape(new Area(runs));
    }

    /** Fill enclosed holes for the visible border while keeping separate objects separate. */
    static Shape exterior(Shape shape) {
        Area silhouette = new Area(shape);
        Area outer      = new Area(silhouette);
        Path2D contour  = new Path2D.Double();
        double[] points = new double[6];
        for (PathIterator path = silhouette.getPathIterator(null); !path.isDone(); path.next()) {
            switch (path.currentSegment(points)) {
                case PathIterator.SEG_MOVETO -> contour.moveTo(points[0], points[1]);
                case PathIterator.SEG_LINETO -> contour.lineTo(points[0], points[1]);
                case PathIterator.SEG_QUADTO -> contour.quadTo(points[0], points[1], points[2], points[3]);
                case PathIterator.SEG_CUBICTO -> contour.curveTo(
                        points[0], points[1], points[2], points[3], points[4], points[5]);
                case PathIterator.SEG_CLOSE -> {
                    contour.closePath();
                    outer.add(new Area(contour));
                    contour.reset();
                }
                default -> throw new IllegalStateException("Unknown outline segment");
            }
        }
        return outer;
    }

    static Polygon polygon(int... xy) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < xy.length; i += 2) {
            polygon.addPoint(xy[i], xy[i + 1]);
        }
        return polygon;
    }
}
