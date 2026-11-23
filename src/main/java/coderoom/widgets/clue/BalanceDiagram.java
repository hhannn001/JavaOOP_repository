package coderoom.widgets.clue;

import coderoom.features.gameplay.PuzzleVariant;
import coderoom.shared.ui.PixelTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** A schematic of the fixed box, four hooks, and two loose weights. */
final class BalanceDiagram extends JPanel {
    private static final Color BRASS = new Color(119, 74, 35);
    private static final int WIDTH  = 320;
    private static final int HEIGHT = 224;

    private int load;

    BalanceDiagram() {
        setName("balanceDiagram");
        setOpaque(false);
        setLoad(PuzzleVariant.STANDARD.balanceLoad());
    }

    void setLoad(int load) {
        this.load = load;
        getAccessibleContext().setAccessibleName("씨앗 상자 무게 " + load + ", 중심에서 왼쪽 한 칸. "
                + "오른쪽 네 고리 중 서로 다른 곳에 무게 2와 3인 추를 하나씩 건다. "
                + "양쪽의 무게 곱하기 거리의 합이 같으면 균형을 이룬다.");
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
            g.translate((getWidth() - WIDTH * scale) / 2, (getHeight() - HEIGHT * scale) / 2);
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(PixelTheme.INK);
            center(g, "양쪽의 무게 × 거리가 같아야 해요", 160, 23, 16, true);
            g.setColor(BRASS);
            g.fillRoundRect(21, 100, 278, 8, 5, 5);
            g.fillPolygon(new int[] {101, 85, 117}, new int[] {108, 138, 138}, 3);
            weight(g, 34, 64, 44, Integer.toString(load));

            g.setColor(PixelTheme.INK);
            center(g, "씨앗 상자", 56, 56, 14, true);
            center(g, "중심", 101, 157, 13, false);
            for (int i = 1; i <= 4; i++) {
                int socket = 101 + i * 45;
                g.setColor(BRASS);
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        1, new float[] {4, 3}, 0));
                g.drawRoundRect(socket - 16, 68, 32, 27, 5, 5);
                g.setColor(PixelTheme.INK);
                center(g, i + "칸", socket, 126, 13, false);
            }

            g.setColor(PixelTheme.INK);
            center(g, "두 추를 서로 다른 고리에", 160, 178, 14, true);
            weight(g, 111, 187, 40, "2");
            weight(g, 170, 187, 40, "3");
        } finally {
            g.dispose();
        }
    }

    private static void weight(Graphics2D g, int x, int y, int width, String number) {
        g.setColor(BRASS);
        g.fillRoundRect(x, y, width, 29, 5, 5);
        g.setColor(PixelTheme.LIGHT);
        center(g, number, x + width / 2, y + 21, 19, true);
    }

    private static void center(Graphics2D g, String text, int x, int y, int size, boolean bold) {
        g.setFont(PixelTheme.font(size).deriveFont(bold ? Font.BOLD : Font.PLAIN));
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
