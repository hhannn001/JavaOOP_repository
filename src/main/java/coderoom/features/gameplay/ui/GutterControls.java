package coderoom.features.gameplay.ui;

import coderoom.features.gameplay.PuzzleVariant;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.Timer;

/** The flow test follows the same channel geometry the player sees. */
final class GutterControls {
    private static final int[] JOINTS = {190, 396, 603, 810};
    private static final int HIGH  = 364;
    private static final int LEVEL = 409;
    private static final int OUTLET = 461;
    private final int supplyHeight;
    private final int drainHeight;
    private final PropBoard board;
    private final Runnable complete;
    private final int[] turns = {1, 2, 1};
    private final Timer flow;
    private double progress;
    private int leak = -1;
    private boolean tested;

    GutterControls(PropBoard board, PuzzleVariant variant, Runnable complete) {
        supplyHeight  = variant.highGutterInlet() ? HIGH : LEVEL;
        drainHeight   = variant.lowGutterOutlet() ? OUTLET : LEVEL;
        this.board    = board;
        this.complete = complete;
        flow         = new Timer(24, event -> advance());
        for (int index = 0; index < turns.length; index++) {
            final int section = index;
            var knob = new PropBoard.Control("gutter" + index, "홈통 " + (index + 1));
            knob.hotspot();
            int left = JOINTS[index];
            board.place(knob, left + 8, 349, JOINTS[index + 1] - left - 16, 112);
            knob.addActionListener(event -> {
                turns[section] = (turns[section] + 1) % 3;
                clearTest();
            });
        }
        var valve = new PropBoard.Control("flowTest", "물 흘려보기");
        valve.sprite(PropBoard.sprite("handwheel"));
        board.place(valve, 454, 511, 92, 92);
        valve.addActionListener(event -> {
            clearTest();
            leak = firstBreak();
            flow.start();
        });
        board.effects(this::paint);
    }

    private int firstBreak() {
        int height = supplyHeight;
        for (int i = 0; i < turns.length; i++) {
            if (height != inlet(i)) { return i; }
            height = outlet(i);
        }
        return height == drainHeight ? -1 : turns.length;
    }

    private int inlet(int index) { return turns[index] == 0 ? HIGH : LEVEL; }
    private int outlet(int index) { return turns[index] == 2 ? OUTLET : LEVEL; }

    private void advance() {
        progress += .135;
        double end = leak < 0 ? 3.6 : leak + .55;
        if (progress >= end) {
            flow.stop();
            if (leak < 0) {
                tested = true;
                complete.run();
            }
        }
        board.repaint();
    }

    private void paint(Graphics2D g) {
        int[] sourceX = {127, 127, 154, JOINTS[0]};
        int[] sourceY = {144, 320, supplyHeight, supplyHeight};
        int[] drainX  = {JOINTS[3], 886, 922};
        int[] drainY  = {drainHeight, drainHeight, 443};
        channel(g, sourceX, sourceY, Math.min(1, progress * 2));
        channel(g, drainX, drainY, leak < 0 ? Math.min(1, (progress - 3) * 1.7) : 0);
        for (int i = 0; i < turns.length; i++) {
            int left  = JOINTS[i];
            int right = JOINTS[i + 1];
            double water = leak < 0 || i < leak ? Math.min(1, progress - i) : 0;
            channel(g, new int[] {left + 3, left + 42, right - 45, right - 3},
                    new int[] {inlet(i), inlet(i), outlet(i), outlet(i)}, water);
        }
        if (leak >= 0 && progress >= leak + .30) {
            int x = JOINTS[leak];
            int y = leak == 0 ? supplyHeight : outlet(leak - 1);
            g.setColor(new Color(115, 210, 228));
            g.fillOval(x - 6, y + 17, 11, 23);
            g.fillOval(x + 2, y + 45, 8, 15);
        }
    }

    private static void channel(Graphics2D g, int[] x, int[] y, double water) {
        stroke(g, 29, new Color(55, 30, 19));
        g.drawPolyline(x, y, x.length);
        stroke(g, 23, new Color(193, 131, 73));
        g.drawPolyline(x, y, x.length);
        stroke(g, 14, new Color(83, 48, 29));
        g.drawPolyline(x, y, x.length);
        if (water <= 0) { return; }

        var trail = new Path2D.Double();
        trail.moveTo(x[0], y[0]);
        double remaining = water * (x.length - 1);
        for (int i = 1; i < x.length && remaining > 0; i++, remaining--) {
            double fraction = Math.min(1, remaining);
            trail.lineTo(x[i - 1] + (x[i] - x[i - 1]) * fraction, y[i - 1] + (y[i] - y[i - 1]) * fraction);
        }
        stroke(g, 8, new Color(110, 202, 226));
        g.draw(trail);
    }

    private static void stroke(Graphics2D g, int width, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    String answer() {
        if (!tested) { return ""; }
        var answer = new StringBuilder();
        for (int turn : turns) { answer.append((char) ('A' + turn)); }
        return answer.toString();
    }

    void reset() {
        int[] start = {1, 2, 1};
        System.arraycopy(start, 0, turns, 0, turns.length);
        clearTest();
    }

    void stop() { flow.stop(); }

    private void clearTest() {
        flow.stop();
        progress = 0;
        tested   = false;
        leak     = -1;
        board.repaint();
    }
}
