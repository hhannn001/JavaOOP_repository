package coderoom.widgets.room;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Looped scenery in the room's image coordinates, independent of puzzle input. */
final class RoomAmbience {
    static final int FRAME_MS = 40;
    private static final Area STUDY_GLASS = panes(new int[][] {{142, 209}, {220, 288}, {300, 363}},
            new int[][] {{79, 190}, {202, 307}, {320, 381}});
    private static final Area GREENHOUSE_GLASS = panes(
            new int[][] {{406, 476}, {483, 552}, {561, 635}, {659, 711}, {720, 785},
                         {795, 866}, {875, 929}, {953, 983}, {990, 1042}, {1051, 1088}},
            new int[][] {{213, 298}, {310, 439}});
    private static final BufferedImage STEAM = softLight(new Color(215, 224, 220, 168));
    private static final BufferedImage AMBER = softLight(new Color(255, 178, 67, 117));
    private static final Color BLACKOUT = new Color(8, 12, 23, 175);
    private static final Drop[] DROPS = drops();

    private record Drop(double x, double y, double speed, int length, int alpha) { }

    private RoomAmbience() { }

    static void study(Graphics2D graphics, double seconds) {
        rain(graphics, STUDY_GLASS, new Rectangle(142, 79, 223, 303), seconds, 33);
    }

    static void greenhouse(Graphics2D graphics, double seconds) {
        Graphics2D glass = (Graphics2D) graphics.create();
        glass.clip(GREENHOUSE_GLASS);
        for (int i = 0; i < 17; i++) {
            int trunk = 434 + i * 38;
            int foot  = 405 + i % 5 * 6;
            int height = 64 + i * 31 % 98;
            double drift = 5.5 * Math.sin(seconds * 1.25 + i * .49)
                    + 1.7 * Math.sin(seconds * 2.3 + i);
            fir(glass, trunk, foot, height, drift);
        }
        branch(glass, seconds);
        glass.dispose();

        rain(graphics, GREENHOUSE_GLASS, new Rectangle(405, 211, 685, 232), seconds, DROPS.length);
    }

    static void basinRipples(Graphics2D water, double seconds, int phase) {
        water.setStroke(new BasicStroke(1.25f));
        for (int i = 0; i < 14 + phase; i++) {
            double age    = cycle(seconds * .43 + i * .381);
            int x         = 550 + i * 67 % 500;
            int y         = 630 + i * 51 % 246;
            double spread = 7 + age * (16 + (y - 630) * .12);
            int alpha     = (int) (90 * Math.sin(Math.PI * age));
            water.setColor(new Color(157, 208, 228, alpha));
            water.draw(new Ellipse2D.Double(x - spread, y - spread * .23, spread * 2, spread * .46));
        }
    }

    static void engine(Graphics2D graphics, double seconds, boolean sealed, int pressurePhase) {
        Graphics2D g = (Graphics2D) graphics.create();
        if (!sealed) {
            int count = 6 + pressurePhase * 3;
            for (int i = 0; i < count; i++) {
                double age = cycle(seconds / 2.7 + i / (double) count);
                int width  = (int) (32 + age * 168);
                int height = (int) (27 + age * 103);
                double x = 1373 - 59 * age + Math.sin(seconds * 1.8 + i) * (4 + age * 19);
                double y = 746 - age * (105 + pressurePhase * 15);
                drawGlow(g, STEAM, x, y, width, height, .63 * Math.sin(Math.PI * age));
            }
        }
        g.dispose();
    }

    static void engineLighting(Graphics2D graphics, Rectangle bounds, double seconds, boolean powered) {
        double phase = seconds % 7;
        double light = powered ? 1 : .82 * pulse(phase - 2, .14) + pulse(phase - 2.42, .19);
        Graphics2D g = (Graphics2D) graphics.create();
        if (light < 1) {
            g.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) (1 - light)));
            g.setColor(BLACKOUT);
            g.fill(bounds);
        }
        if (light > 0) {
            drawGlow(g, AMBER, 811, 184, 410, 300, .28 * light);
            drawGlow(g, AMBER, 1380, 168, 235, 170, .25 * light);
        }
        g.dispose();
    }

    private static double pulse(double offset, double radius) {
        double distance = Math.abs(offset) / radius;
        return distance >= 1 ? 0 : .5 + .5 * Math.cos(Math.PI * distance);
    }

    private static void branch(Graphics2D g, double seconds) {
        double bend = Math.sin(seconds * 1.28) * 11 + Math.sin(seconds * 2.03) * 3;
        g.setColor(new Color(12, 29, 38, 108));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        var stem = new Path2D.Double();
        stem.moveTo(1094, 223);
        stem.curveTo(1063, 226, 1027, 237 + .5 * bend, 982, 255 + bend);
        g.draw(stem);
        for (int i = 0; i < 7; i++) {
            int x = 1084 - i * 14;
            double y = 224 + i * 4 + bend * i / 7;
            int direction = i % 2 == 0 ? 1 : -1;
            var leaf = new Path2D.Double();
            leaf.moveTo(x, y);
            leaf.quadTo(x - 14, y + direction * 4, x - 23, y + direction * (9 + i % 3 * 4));
            leaf.quadTo(x - 9, y + direction * 13, x, y);
            g.fill(leaf);
        }
    }

    private static void fir(Graphics2D g, int x, int bottom, int height, double drift) {
        Path2D crown = new Path2D.Double();
        crown.moveTo(x + drift, bottom - height);
        for (int side : new int[] {-1, 1}) {
            for (int n = 0; n < 7; n++) {
                int tier = side == -1 ? n : 6 - n;
                double fraction = (tier + 1) / 8.0;
                double y = bottom - height * (1 - fraction);
                double bend = drift * (1 - fraction);
                double outer = (7 + tier * 2.1) * side;
                double inner = (4 + tier * 1.35) * side;
                if (side == 1) { crown.lineTo(x + bend + inner, y + 4); }
                crown.lineTo(x + bend + outer, y + 10);
                if (side == -1) { crown.lineTo(x + bend + inner, y + 4); }
            }
        }
        crown.closePath();
        g.setColor(new Color(15, 32, 43, 74));
        g.fill(crown);
    }

    private static void rain(Graphics2D graphics, Area mask, Rectangle bounds, double seconds, int count) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.clip(mask);
        g.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < count; i++) {
            Drop drop = DROPS[i];
            double fall = cycle(drop.y + seconds * drop.speed);
            int x = bounds.x + (int) (bounds.width * drop.x) - (int) (fall * 12);
            int y = bounds.y + (int) ((bounds.height + 24) * fall);
            g.setColor(new Color(156, 205, 235, drop.alpha));
            g.drawLine(x, y, x - 2 - drop.length / 9, y + drop.length);
        }
        g.dispose();
    }

    private static Area panes(int[][] columns, int[][] rows) {
        Area area = new Area();
        for (int[] x : columns) {
            for (int[] y : rows) { area.add(new Area(new Rectangle(x[0], y[0], x[1] - x[0], y[1] - y[0]))); }
        }
        return area;
    }

    private static Drop[] drops() {
        Random random = new Random(163);
        Drop[] result = new Drop[111];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Drop(random.nextDouble(), random.nextDouble(), .32 + random.nextDouble() * .65,
                    9 + random.nextInt(20), 38 + random.nextInt(78));
        }
        return result;
    }

    private static BufferedImage softLight(Color tint) {
        int size = 96;
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setPaint(new RadialGradientPaint(size / 2f, size / 2f, size / 2f,
                new float[] {0, .34f, 1}, new Color[] {tint,
                    new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), tint.getAlpha() / 2),
                    new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0)}));
        g.fillRect(0, 0, size, size);
        g.dispose();
        return image;
    }

    private static void drawGlow(Graphics2D g, BufferedImage sprite, double x, double y, int width, int height,
                                 double visibility) {
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) visibility));
        g.drawImage(sprite, (int) x - width / 2, (int) y - height / 2, width, height, null);
    }

    private static double cycle(double value) { return value - Math.floor(value); }
}
