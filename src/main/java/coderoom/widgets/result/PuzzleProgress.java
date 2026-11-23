package coderoom.widgets.result;

import coderoom.shared.ui.PixelTheme;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 개수를 12시 방향부터 시계 방향으로 채운 부채꼴로 표시한다. */
public final class PuzzleProgress extends JPanel {
    private static final Image RIM    = sprite("progress-rim");
    private static final Image EMPTY  = sprite("progress-empty");
    private static final Image FILLED = sprite("progress-filled");
    private static final Image WAX    = waxTexture();

    private static final int FILL_MILLIS = 280;

    private final boolean attempts;
    private int solved;
    private int total;
    private long fillStarted;
    private final Timer fillTimer = new Timer(16, event -> {
        if (fillFraction() >= 1) {
            ((Timer) event.getSource()).stop();
        }
        repaint();
    });

    public PuzzleProgress() {
        this(false);
    }

    public PuzzleProgress(boolean attempts) {
        this.attempts = attempts;
        setOpaque(false);
    }

    public void setProgress(int solvedCount, int puzzleCount) {
        int filled = Math.max(0, Math.min(solvedCount, puzzleCount));
        if (solved == filled && total == puzzleCount) {
            return;
        }
        boolean animate = total == puzzleCount && filled == solved + 1;
        solved      = filled;
        total       = puzzleCount;
        fillStarted = animate ? System.nanoTime() : 0;
        if (animate && isShowing()) {
            fillTimer.restart();
        } else {
            fillTimer.stop();
        }
        String label = (attempts ? "남은 오답 기회 " : "풀이 ") + solved + "/" + total;
        getAccessibleContext().setAccessibleName(label);
        repaint();
    }

    @Override public boolean contains(int x, int y) {
        return false;
    }

    @Override public void removeNotify() {
        fillTimer.stop();
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics graphics) {
        if (total <= 0) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        PixelTheme.pixels(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size       = Math.min(getWidth(), getHeight());
        int inset      = Math.max(2, size / 22);
        int diameter   = size - 2 * inset;
        double sweep   = 360.0 / total;
        double gap     = total == 1 ? 0 : Math.min(5, sweep / 5);
        double moving  = fillFraction();

        g.setColor(new Color(23, 18, 15, 235));
        g.fillOval(inset - 1, inset - 1, diameter + 2, diameter + 2);
        for (int i = 0; i < total; i++) {
            double start = 90 - i * sweep - gap / 2;
            double span  = sweep - gap;
            drawSector(g, EMPTY, inset, diameter, start, span);
            if (i < solved) {
                drawSector(g, attempts ? WAX : FILLED, inset, diameter, start, span * (i == solved - 1 ? moving : 1));
            }
        }
        g.drawImage(RIM, 0, 0, size, size, null);
        g.dispose();
    }

    private double fillFraction() {
        return fillStarted == 0 ? 1 : Math.min(1, (System.nanoTime() - fillStarted) / (FILL_MILLIS * 1_000_000.0));
    }

    private static void drawSector(Graphics2D graphics, Image texture, int inset, int diameter,
            double start, double sweep) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.clip(new Arc2D.Double(inset, inset, diameter, diameter, start, -sweep, Arc2D.PIE));
        g.drawImage(texture, inset, inset, diameter, diameter, null);
        g.dispose();
    }

    private static Image waxTexture() {
        int width             = FILLED.getWidth(null);
        int height            = FILLED.getHeight(null);
        BufferedImage texture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g          = texture.createGraphics();
        g.drawImage(FILLED, 0, 0, null);
        g.setComposite(AlphaComposite.SrcAtop.derive(0.94f));
        g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0), new Point2D.Float(width, height),
                new float[] {0f, 0.31f, 0.49f, 0.74f, 1f},
                new Color[] {new Color(173, 112, 136), new Color(149, 78, 106), new Color(197, 136, 160),
                    new Color(130, 64, 90), new Color(95, 45, 71)}));
        g.fillRect(0, 0, width, height);

        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(new Color(255, 229, 239, 53));
        Random speckles = new Random(57);
        for (int i = 0; i < 180; i++) {
            int x    = speckles.nextInt(width);
            int y    = speckles.nextInt(height);
            int size = 2 + speckles.nextInt(4);
            g.fillOval(x, y, size, size);
        }
        g.dispose();
        return texture;
    }

    private static Image sprite(String name) {
        return new ImageIcon(PuzzleProgress.class.getResource("/images/game-ui/hud/" + name + ".png")).getImage();
    }
}
