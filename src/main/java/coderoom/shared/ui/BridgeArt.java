package coderoom.shared.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Canonical bridge geometry and artwork shared by the puzzle and its room projection. */
public final class BridgeArt {
    public static final int WIDTH  = 1000;
    public static final int HEIGHT = 667;
    private static final int RAIL_STEP = 14;
    public static final int HORIZONTAL_ALIGNMENT = 1;
    public static final int VERTICAL_ALIGNMENT = -1;
    private static final int HORIZONTAL_RAIL_A   = 304;
    private static final int HORIZONTAL_RAIL_B   = 329;
    private static final int VERTICAL_RAIL_A     = 662;
    private static final int VERTICAL_RAIL_B     = 688;
    private static final Rectangle CENTER_SOCKET = new Rectangle(625, 266, 125, 134);
    private static final Rectangle CENTER_TRAY   = new Rectangle(174, 96, 125, 134);

    private BridgeArt() { }

    public record Placement(double across, double down, boolean centerFitted, int horizontalOffset, int verticalOffset) {
        public static final Placement INITIAL = new Placement(0, 0, false, 0, 0);
        public static final Placement SOLVED  = new Placement(1, 1, true, HORIZONTAL_ALIGNMENT, VERTICAL_ALIGNMENT);
    }

    public static double verticalTrackCenter() { return (VERTICAL_RAIL_A + VERTICAL_RAIL_B) / 2.0; }

    public static double horizontalTrackCenter() { return (HORIZONTAL_RAIL_A + HORIZONTAL_RAIL_B) / 2.0; }

    public static Rectangle centerBounds(boolean fitted) {
        return new Rectangle(fitted ? CENTER_SOCKET : CENTER_TRAY);
    }

    public static Rectangle platformBounds(int index, Placement placement) {
        return index == 0
                ? new Rectangle((int) Math.round(540 + placement.across() * 210), 266, 160, 100)
                : new Rectangle(625, (int) Math.round(80 + placement.down() * 320), 100, 187);
    }

    public static void paintPlatform(Graphics2D g, int index, Placement placement, BufferedImage sprite) {
        Rectangle bounds = platformBounds(index, placement);
        g.drawImage(sprite, bounds.x, bounds.y, bounds.width, bounds.height, null);
        if (index == 0) {
            horizontalRails(g, bounds.x, bounds.x + bounds.width, (placement.horizontalOffset() - HORIZONTAL_ALIGNMENT) * RAIL_STEP);
        } else {
            verticalRails(g, bounds.y, bounds.y + bounds.height, (VERTICAL_ALIGNMENT - placement.verticalOffset()) * RAIL_STEP);
        }
    }

    /** The short lower tongue fits the narrower vertical deck. */
    public static Shape centerShape(Rectangle bounds) {
        int x = bounds.x;
        int y = bounds.y;
        var outline = new Path2D.Double();
        outline.moveTo(x, y);
        outline.lineTo(x + bounds.width, y);
        outline.lineTo(x + bounds.width, y + 100);
        outline.lineTo(x + 100, y + 100);
        outline.lineTo(x + 100, y + bounds.height);
        outline.lineTo(x, y + bounds.height);
        outline.closePath();
        return outline;
    }

    public static void horizontalRails(Graphics2D g, int start, int end, int offset) {
        straightRail(g, start, HORIZONTAL_RAIL_A + offset, end, HORIZONTAL_RAIL_A + offset);
        straightRail(g, start, HORIZONTAL_RAIL_B + offset, end, HORIZONTAL_RAIL_B + offset);
    }

    public static void verticalRails(Graphics2D g, int start, int end, int offset) {
        straightRail(g, VERTICAL_RAIL_A + offset, start, VERTICAL_RAIL_A + offset, end);
        straightRail(g, VERTICAL_RAIL_B + offset, start, VERTICAL_RAIL_B + offset, end);
    }

    public static void paintCenterDeck(Graphics2D g, Rectangle bounds, BufferedImage woodSprite) {
        Graphics2D wood = (Graphics2D) g.create();
        try {
            wood.clip(centerShape(bounds));
            wood.drawImage(woodSprite, bounds.x, bounds.y, bounds.width, bounds.height, null);
            int dx = bounds.x - CENTER_SOCKET.x;
            int dy = bounds.y - CENTER_SOCKET.y;
            horizontalRails(wood, bounds.x, bounds.x + bounds.width, dy);
            verticalRails(wood, bounds.y, bounds.y + bounds.height, dx);
        } finally {
            wood.dispose();
        }
    }

    private static void straightRail(Graphics2D g, int x1, int y1, int x2, int y2) {
        boolean horizontal = y1 == y2;
        int x = x1 - (horizontal ? 0 : 4);
        int y = y1 - (horizontal ? 4 : 0);
        g.setPaint(new GradientPaint(x, y, new Color(230, 199, 147),
                x + (horizontal ? 0 : 8), y + (horizontal ? 8 : 0), new Color(108, 77, 44)));
        metalRail(g, new Line2D.Float(x1, y1, x2, y2));
    }

    private static void metalRail(Graphics2D g, Shape rail) {
        var face = g.getPaint();
        g.setColor(new Color(43, 30, 22));
        g.setStroke(new BasicStroke(12, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(rail);
        g.setPaint(face);
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(rail);
    }

}
