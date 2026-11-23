package coderoom.widgets.room;

import coderoom.shared.ui.BridgeArt;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/** Projects the same three bridge pieces from the device view onto the room floor. */
final class BridgeProjection {
    private static final int TOP_Y = 608;
    private static final int FLOOR_HEIGHT = 282;
    private static final int ORIGIN_X = 425;
    private static final int IMAGE_WIDTH = 564;
    private static final int COMPLETED_X = 620;
    private static final int COMPLETED_Y = 261;
    static final double CART_TURN = .65;

    private final BufferedImage horizontal;
    private final BufferedImage vertical;
    private final BufferedImage completed;
    private BridgeArt.Placement placement;
    private BufferedImage projected;
    private Shape outline;

    BridgeProjection(BufferedImage horizontal, BufferedImage vertical, BufferedImage completed) {
        this.horizontal = horizontal;
        this.vertical   = vertical;
        this.completed  = completed;
        update(BridgeArt.Placement.INITIAL);
    }

    boolean update(BridgeArt.Placement next) {
        if (next.equals(placement)) { return false; }
        placement = next;
        projected = null;
        outline   = null;
        return true;
    }

    Shape outline() {
        if (outline == null) {
            var footprint = new Area(projectShape(BridgeArt.platformBounds(0, placement)));
            footprint.add(new Area(projectShape(BridgeArt.platformBounds(1, placement))));
            footprint.add(new Area(projectShape(BridgeArt.centerShape(BridgeArt.centerBounds(placement.centerFitted())))));
            outline = footprint;
        }
        return outline;
    }

    void paint(Graphics2D graphics) {
        if (projected == null) { projected = render(); }
        graphics.drawImage(projected, ORIGIN_X, TOP_Y, null);
    }

    private BufferedImage render() {
        var topView = new BufferedImage(BridgeArt.WIDTH, BridgeArt.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tracks = topView.createGraphics();
        tracks.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tracks.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        var bed = new Area(new Rectangle(0, 0, BridgeArt.WIDTH, BridgeArt.HEIGHT));
        bed.subtract(new Area(BridgeArt.centerShape(BridgeArt.centerBounds(true))));
        tracks.setClip(bed);
        tracks.setColor(new Color(62, 44, 30));
        for (int x = 337; x < 902; x += 44) { tracks.fillRect(x, 289, 9, 54); }
        for (int y = 82; y < 595; y += 43) { tracks.fillRect(648, y, 54, 9); }
        BridgeArt.horizontalRails(tracks, 330, 910, 0);
        BridgeArt.verticalRails(tracks, 77, BridgeArt.HEIGHT, 0);
        tracks.setClip(null);
        if (BridgeArt.Placement.SOLVED.equals(placement)) {
            tracks.drawImage(completed, COMPLETED_X, COMPLETED_Y, null);
        } else {
            paintLooseDeck(tracks);
        }
        tracks.setComposite(AlphaComposite.SrcAtop.derive(.24f));
        tracks.setColor(new Color(39, 28, 24));
        tracks.fillRect(0, 0, topView.getWidth(), topView.getHeight());
        tracks.dispose();

        var room = new BufferedImage(IMAGE_WIDTH, FLOOR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D surface = room.createGraphics();
        surface.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int y = 0; y < FLOOR_HEIGHT; y++) {
            double depth = (y + 0.5) / FLOOR_HEIGHT;
            int sourceY = (int) (depth * BridgeArt.HEIGHT);
            int left    = (int) Math.round(projectX(0, depth)) - ORIGIN_X;
            int right   = (int) Math.round(projectX(BridgeArt.WIDTH, depth)) - ORIGIN_X;
            surface.drawImage(topView, left, y, right, y + 1,
                    0, sourceY, BridgeArt.WIDTH, sourceY + 1, null);
        }
        surface.dispose();
        return room;
    }

    private void paintLooseDeck(Graphics2D tracks) {
        if (!placement.centerFitted()) {
            Shape socket = BridgeArt.centerShape(BridgeArt.centerBounds(true));
            tracks.setColor(new Color(29, 24, 18, 110));
            tracks.fill(socket);
            tracks.setColor(new Color(165, 129, 83, 155));
            tracks.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    1, new float[] {6, 5}, 0));
            tracks.draw(socket);
        }
        BridgeArt.paintPlatform(tracks, 0, placement, horizontal);
        BridgeArt.paintPlatform(tracks, 1, placement, vertical);
        Rectangle center = BridgeArt.centerBounds(placement.centerFitted());
        BridgeArt.paintCenterDeck(tracks, center, horizontal);
        tracks.setColor(new Color(99, 64, 36));
        tracks.setStroke(new BasicStroke(2));
        tracks.draw(BridgeArt.centerShape(center));
    }

    private static Shape projectShape(Shape shape) {
        var projected = new Path2D.Double();
        double[] point = new double[6];
        for (PathIterator path = shape.getPathIterator(null); !path.isDone(); path.next()) {
            int segment = path.currentSegment(point);
            double depth = point[1] / BridgeArt.HEIGHT;
            double x     = projectX(point[0], depth);
            double y     = TOP_Y + FLOOR_HEIGHT * depth;
            switch (segment) {
                case PathIterator.SEG_MOVETO -> projected.moveTo(x, y);
                case PathIterator.SEG_LINETO -> projected.lineTo(x, y);
                case PathIterator.SEG_CLOSE -> projected.closePath();
                default -> throw new IllegalArgumentException("Bridge projection requires polygonal pieces");
            }
        }
        return projected;
    }

    static Point2D trolleyTrackPoint(double progress) {
        double x = BridgeArt.verticalTrackCenter();
        double y = BridgeArt.horizontalTrackCenter();
        if (progress <= CART_TURN) {
            y = BridgeArt.HEIGHT + (y - BridgeArt.HEIGHT) * progress / CART_TURN;
        } else {
            x += (882 - x) * (progress - CART_TURN) / (1 - CART_TURN);
        }
        double depth = y / BridgeArt.HEIGHT;
        return new Point2D.Double(projectX(x, depth), TOP_Y + FLOOR_HEIGHT * depth);
    }

    private static double projectX(double x, double depth) {
        return 542 - 180 * depth + x / BridgeArt.WIDTH * (380 + 270 * depth);
    }
}
