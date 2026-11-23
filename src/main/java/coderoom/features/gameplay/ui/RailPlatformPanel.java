package coderoom.features.gameplay.ui;

import coderoom.shared.audio.GameAudio;
import coderoom.shared.ui.PixelTheme;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Two platforms share a crossing and the horizontal one can wait in a siding. */
final class RailPlatformPanel extends JPanel {
    private static final double HORIZONTAL_START = .54;
    private static final double HORIZONTAL_TRAVEL = .21;
    private static final double VERTICAL_START = .12;
    private static final double VERTICAL_TRAVEL = .48;
    private static final int RAIL_STEP = 14;
    private static final int HORIZONTAL_ALIGNMENT = 1;
    private static final int VERTICAL_ALIGNMENT = -1;

    private double across;
    private double down;
    private int active = -1;
    private int focused;
    private int grabOffset;
    private double initialPosition;
    private final int[] railOffsets = new int[2];
    private final BufferedImage[] boards;

    RailPlatformPanel(BufferedImage horizontal, BufferedImage vertical) {
        boards = new BufferedImage[] {horizontal, vertical};
        setOpaque(false);
        setName("railPlatform");
        setFocusable(true);
        setBackground(new Color(25, 48, 55));
        getAccessibleContext().setAccessibleName("스페이스로 발판을 전환하고 방향키로 움직입니다. 레일 조절 장치로 이음 위치를 바꿉니다.");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) {
                boolean onPlatform = platform(0).contains(event.getPoint()) || platform(1).contains(event.getPoint());
                setCursor(Cursor.getPredefinedCursor(onPlatform ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override public void mousePressed(MouseEvent event) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(event)) { return; }
                requestFocusInWindow();
                if (platform(0).contains(event.getPoint())) {
                    active = 0;
                    grabOffset = event.getX() - platform(0).x;
                } else if (platform(1).contains(event.getPoint())) {
                    active = 1;
                    grabOffset = event.getY() - platform(1).y;
                }
                if (active >= 0) {
                    focused         = active;
                    initialPosition = active == 0 ? across : down;
                }
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (!isEnabled()) { return; }
                if (active == 0) {
                    double travel = (event.getX() - grabOffset - getWidth() * HORIZONTAL_START)
                            / Math.max(1, getWidth() * HORIZONTAL_TRAVEL);
                    across = limitAcross(travel);
                } else if (active == 1 && across == -1) {
                    down = unit((event.getY() - grabOffset - getHeight() * VERTICAL_START) / Math.max(1, getHeight() * VERTICAL_TRAVEL));
                }
                repaint();
            }

            @Override public void mouseReleased(MouseEvent event) {
                if (active == 0) { across = Math.round(across); }
                if (active == 1) { down = Math.round(down); }
                if (active == 0 && across != initialPosition || active == 1 && down != initialPosition) {
                    GameAudio.play(GameAudio.Cue.SNAP);
                }
                active = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (!isEnabled()) { return; }
                double previousAcross = across;
                double previousDown   = down;
                if (event.getKeyCode() == KeyEvent.VK_SPACE) { focused = 1 - focused; }
                else if (focused == 0) {
                    if (event.getKeyCode() == KeyEvent.VK_RIGHT) { across = limitAcross(across + 1); }
                    if (event.getKeyCode() == KeyEvent.VK_LEFT) { across = limitAcross(across - 1); }
                } else if (focused == 1 && across == -1) {
                    if (event.getKeyCode() == KeyEvent.VK_DOWN) { down = 1; }
                    if (event.getKeyCode() == KeyEvent.VK_UP) { down = 0; }
                }
                if (across != previousAcross || down != previousDown) { GameAudio.play(GameAudio.Cue.SNAP); }
                repaint();
            }
        });
    }

    private double limitAcross(double progress) {
        return Math.max(-1, Math.min(down == 1 ? 1 : 0, progress));
    }

    private static double unit(double value) { return Math.max(0, Math.min(1, value)); }

    private Rectangle platform(int index) {
        int w = getWidth();
        int h = getHeight();
        return index == 0
                ? new Rectangle((int) (w * (HORIZONTAL_START + across * HORIZONTAL_TRAVEL)),
                        (int) (h * .399), (int) (w * .16), (int) (h * .15))
                : new Rectangle((int) (w * .625), (int) (h * (VERTICAL_START + down * VERTICAL_TRAVEL)),
                        (int) (w * .10), (int) (h * .28));
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D guides = (Graphics2D) g.create();
        try {
            guides.scale(getWidth() / (double) PropBoard.WIDTH, getHeight() / (double) PropBoard.HEIGHT);
            int sidingX = (int) (PropBoard.WIDTH * (HORIZONTAL_START - HORIZONTAL_TRAVEL));
            int dockX   = (int) (PropBoard.WIDTH * (HORIZONTAL_START + HORIZONTAL_TRAVEL));
            int deckY   = (int) (PropBoard.HEIGHT * .399);
            // The iron guard covers the unused extension of the background track.
            guides.setColor(new Color(48, 44, 35));
            guides.fillRoundRect(95, 289, sidingX - 93, 53, 10, 10);
            guides.setColor(new Color(99, 88, 66));
            guides.drawRoundRect(95, 289, sidingX - 93, 53, 10, 10);
            railStop(guides, sidingX - 12, deckY - 6, 13, 112);
            railStop(guides, sidingX, deckY - 6, 165, 10);
            railStop(guides, sidingX, deckY + 96, 165, 10);
            railStop(guides, dockX - 7, deckY - 8, 174, 12);
            railStop(guides, dockX - 7, deckY + 96, 174, 12);
            railStop(guides, dockX + 155, deckY - 8, 12, 116);
            railStop(guides, 613, 589, 124, 14);
            railStop(guides, 613, 482, 13, 121);
            railStop(guides, 725, 482, 13, 121);
            if (down < 1) { railStop(guides, 712, 275, 14, 105); }
            int verticalDockY = (int) (PropBoard.HEIGHT * (VERTICAL_START + VERTICAL_TRAVEL));
            track(guides, dockX - 20, 100, deckY + 38, deckY + 63, 20);
            track(guides, dockX + 160, 100, deckY + 38, deckY + 63, 18);
            Graphics2D verticalJoint = (Graphics2D) guides.create();
            try {
                verticalJoint.translate(725, verticalDockY);
                verticalJoint.rotate(Math.PI / 2);
                track(verticalJoint, -20, 100, 38, 66, 20);
                track(verticalJoint, 187, 100, 38, 66, 15);
            } finally {
                verticalJoint.dispose();
            }
        } finally {
            guides.dispose();
        }
        for (int i = 0; i < 2; i++) {
            Rectangle p = platform(i);
            g.drawImage(boards[i], p.x, p.y, p.width, p.height, null);
            Graphics2D deck = (Graphics2D) g.create();
            try {
                deck.translate(p.x, p.y);
                if (i == 1) {
                    deck.translate(p.width, 0);
                    deck.rotate(Math.PI / 2);
                }
                deck.scale((i == 0 ? p.width : p.height) / 160.0, (i == 0 ? p.height : p.width) / 100.0);
                int adjustment = railOffsets[i] * RAIL_STEP;
                int firstRail  = (i == 0 ? 24 : 52) + adjustment;
                int secondRail = (i == 0 ? 49 : 80) + adjustment;
                track(deck, 0, 100, firstRail, secondRail, 160);
            } finally {
                deck.dispose();
            }
            if ((hasFocus() && focused == i) || active == i) {
                g.setColor(PixelTheme.LIGHT);
                g.drawRoundRect(p.x, p.y, p.width, p.height, 8, 8);
            }
        }
    }

    private static void railStop(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(51, 29, 18));
        g.fillRoundRect(x + 2, y + 3, width, height, 4, 4);
        g.setColor(new Color(155, 103, 49));
        g.fillRoundRect(x, y, width, height, 4, 4);
        g.setColor(new Color(225, 170, 97));
        g.drawLine(x + 2, y + 2, x + width - 3, y + 2);
    }

    static void track(Graphics2D g, int x, int height, int top, int bottom, int width) {
        g.setColor(new Color(39, 27, 21));
        g.fillRect(x, top, width, height / 14 + 2);
        g.fillRect(x, bottom, width, height / 14 + 2);
        g.setColor(new Color(212, 164, 100));
        g.fillRect(x, top, width, Math.max(2, height / 22));
        g.fillRect(x, bottom, width, Math.max(2, height / 22));
    }

    void adjustRail(int index, PropBoard.Control adjuster) {
        railOffsets[index] = railOffsets[index] == 1 ? -1 : railOffsets[index] + 1;
        describeRail(index, adjuster);
        repaint();
    }

    void describeRail(int index, PropBoard.Control adjuster) {
        adjuster.describe(adjuster.getText() + " · " + (railOffsets[index] + 2) + "단");
    }

    String answer() {
        return active < 0 && across == 1 && down == 1
                && railOffsets[0] == HORIZONTAL_ALIGNMENT && railOffsets[1] == VERTICAL_ALIGNMENT ? "AB" : "";
    }

    void reset() {
        across = 0;
        down   = 0;
        active = -1;
        java.util.Arrays.fill(railOffsets, 0);
        repaint();
    }
}
