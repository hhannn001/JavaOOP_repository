package coderoom.app;

import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** 창틀·건물은 고정하고 유리 너머 잎사귀 에셋만 반복한다. */
final class WindowAmbient {
    static final int FRAME_MS    = 40;
    private static final int FALL_TICKS = 100;
    private final BufferedImage leaves = SceneObjectButton.read("window-leaves.png");
    private final java.util.Random random = new java.util.Random();
    private int tick;
    private int fallTick   = -1;
    private int quietTicks = 250;
    private boolean secondLeaf;

    void advance() {
        tick++;
        if (fallTick >= 0) {
            if (++fallTick >= FALL_TICKS) {
                fallTick   = -1;
                quietTicks = 250 + random.nextInt(201);
            }
        } else if (--quietTicks <= 0) {
            fallTick   = 0;
            secondLeaf = random.nextBoolean();
        }
    }

    boolean isFalling() { return fallTick >= 0; }
    int frameIndex() { return new int[]{3, 4, 5, 4}[(tick / 16) % 4]; }

    void paint(Graphics2D graphics, int x, int y, int width, int height) {
        Graphics2D g   = (Graphics2D) graphics.create();
        Area glass    = new Area();
        double[] cols  = {0.165, 0.39, 0.64};
        double[] rows  = {0.29, 0.50, 0.72};
        double[] sizes = {0.18, 0.19, 0.20};
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < cols.length; column++) {
                double paneWidth = column == 0 ? 0.20 : 0.215;
                glass.add(new Area(new Rectangle2D.Double(x + width * cols[column], y + height * rows[row],
                        width * paneWidth, height * sizes[row])));
            }
        }
        g.clip(glass);
        int cellWidth  = leaves.getWidth() / 3;
        int cellHeight = leaves.getHeight() / 2;
        int frame      = frameIndex();
        int sourceX    = frame % 3 * cellWidth;
        int sourceY    = frame / 3 * cellHeight;
        int left       = x + (int) (width * 0.16);
        int top        = y + (int) (height * 0.28);
        int right      = x + (int) (width * 0.88);
        int bottom     = y + (int) (height * 0.94);
        // The lower atlas rows contain only attached foliage above y=220.
        // Keep detached leaves out of the idle loop, then reuse a single leaf sprite.
        int foliageBottom = top + (bottom - top) * 220 / cellHeight;
        g.drawImage(leaves, left, top, right, foliageBottom,
                sourceX, sourceY, sourceX + cellWidth, sourceY + 220, null);
        if (isFalling()) {
            paintLeaf(g, left, top, right - left, bottom - top, fallTick / 80.0, 0);
            if (secondLeaf && fallTick >= 20) {
                paintLeaf(g, left, top, right - left, bottom - top, (fallTick - 20) / 80.0, 1);
            }
        }
        g.dispose();
    }
    private void paintLeaf(Graphics2D g, int left, int top, int width, int height, double progress, int variant) {
        if (progress > 1) return;
        double drift = Math.sin(progress * Math.PI * 3 + variant) * 0.035;
        int x        = left + (int) (width * (0.30 + variant * 0.22 + drift + progress * 0.04));
        int y        = top + (int) (height * (0.22 + progress * 0.85));
        int leafW    = Math.max(5, width * 20 / 512);
        int leafH    = Math.max(7, height * 26 / 512);
        g.drawImage(leaves, x, y, x + leafW, y + leafH, 174, 42, 194, 68, null);
    }
}
