package coderoom.features.gameplay.ui;

import coderoom.shared.audio.GameAudio;
import coderoom.shared.ui.BridgeArt;
import coderoom.shared.ui.HandCursor;
import coderoom.shared.ui.PixelTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Two sliding platforms and the wooden deck that closes their crossing. */
final class RailPlatformPanel extends JPanel {
    private static final double HORIZONTAL_START = .54;
    private static final double HORIZONTAL_TRAVEL = .21;
    private static final double VERTICAL_START = .12;
    private static final double VERTICAL_TRAVEL = .48;
    private double across;
    private double down;
    private int active = -1;
    private int focused;
    private int grabOffset;
    private double initialPosition;
    private final int[] railOffsets  = new int[2];
    private boolean centerFitted;
    private Point fittingPointer;
    private Point fittingGrab;
    private Runnable onPlacementChanged = () -> { };
    private final BufferedImage[] boards;

    RailPlatformPanel(BufferedImage horizontal, BufferedImage vertical) {
        boards = new BufferedImage[] {horizontal, vertical};
        setOpaque(false);
        setName("railPlatform");
        setFocusable(true);
        setBackground(new Color(25, 48, 55));
        getAccessibleContext().setAccessibleName("스페이스로 부품을 전환합니다. 방향키로 발판을 옮기고, "
                + "엔터로 중앙 발판을 끼우거나 뺍니다.");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(event)) { return; }
                requestFocusInWindow();
                active = partAt(event.getPoint());
                if (active < 0) { return; }
                focused = active;
                if (active < 2) {
                    Rectangle bounds = platform(active);
                    grabOffset       = active == 0 ? event.getX() - bounds.x : event.getY() - bounds.y;
                    initialPosition  = active == 0 ? across : down;
                } else {
                    fittingPointer = boardPoint(event.getPoint());
                    Rectangle bounds = centerBounds();
                    fittingGrab = new Point(fittingPointer.x - bounds.x, fittingPointer.y - bounds.y);
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
                } else if (active >= 2) {
                    fittingPointer = boardPoint(event.getPoint());
                }
                repaint();
            }

            @Override public void mouseReleased(MouseEvent event) {
                if (active == 0) { across = Math.round(across); }
                if (active == 1) { down = Math.round(down); }
                boolean moved = active == 0 && across != initialPosition || active == 1 && down != initialPosition;
                if (active >= 2) {
                    Point point = boardPoint(event.getPoint());
                    if (BridgeArt.centerShape(BridgeArt.centerBounds(true)).contains(point) && platformsInPlace()) {
                        moved        = !centerFitted;
                        centerFitted = true;
                    } else if (BridgeArt.centerShape(BridgeArt.centerBounds(false)).contains(point)) {
                        moved        = centerFitted;
                        centerFitted = false;
                    }
                    fittingPointer = null;
                }
                active = -1;
                if (moved) { placementChanged(); }
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        HandCursor.install(this, point -> partAt(point) >= 0);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (!isEnabled()) { return; }
                double previousAcross = across;
                double previousDown   = down;
                int key = event.getKeyCode();
                if (key == KeyEvent.VK_SPACE) { focused = (focused + (event.isShiftDown() ? 2 : 1)) % 3; }
                else if (focused >= 2 && key == KeyEvent.VK_ENTER && platformsInPlace()) {
                    centerFitted = !centerFitted;
                    placementChanged();
                } else if (!centerFitted && focused == 0) {
                    if (key == KeyEvent.VK_RIGHT) { across = limitAcross(across + 1); }
                    if (key == KeyEvent.VK_LEFT) { across = limitAcross(across - 1); }
                } else if (!centerFitted && focused == 1 && across == -1) {
                    if (key == KeyEvent.VK_DOWN) { down = 1; }
                    if (key == KeyEvent.VK_UP) { down = 0; }
                }
                if (across != previousAcross || down != previousDown) { placementChanged(); }
                repaint();
            }
        });
    }

    void onPlacementChanged(Runnable callback) { onPlacementChanged = callback; }

    private void placementChanged() {
        GameAudio.play(GameAudio.Cue.SNAP);
        onPlacementChanged.run();
    }

    boolean platformsInPlace() { return across == 1 && down == 1; }

    boolean centerInPlace() { return centerFitted; }

    private Rectangle centerBounds() { return BridgeArt.centerBounds(centerFitted); }

    private Point boardPoint(Point pointer) {
        int x = pointer.x * PropBoard.WIDTH / Math.max(1, getWidth());
        int y = pointer.y * PropBoard.HEIGHT / Math.max(1, getHeight());
        return new Point(x, y);
    }

    private int partAt(Point pointer) {
        if (BridgeArt.centerShape(centerBounds()).contains(boardPoint(pointer))) { return 2; }
        if (centerFitted) { return -1; }
        if (platform(0).contains(pointer)) { return 0; }
        return platform(1).contains(pointer) ? 1 : -1;
    }

    private double limitAcross(double progress) {
        return Math.max(-1, Math.min(down == 1 ? 1 : 0, progress));
    }

    private static double unit(double value) { return Math.max(0, Math.min(1, value)); }

    private Rectangle platform(int index) {
        Rectangle bounds = BridgeArt.platformBounds(index, placement());
        return new Rectangle(bounds.x * getWidth() / BridgeArt.WIDTH, bounds.y * getHeight() / BridgeArt.HEIGHT,
                bounds.width * getWidth() / BridgeArt.WIDTH, bounds.height * getHeight() / BridgeArt.HEIGHT);
    }

    BridgeArt.Placement placement() {
        return new BridgeArt.Placement(across, down, centerFitted, railOffsets[0], railOffsets[1]);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D board = (Graphics2D) g.create();
        try {
            board.scale(getWidth() / (double) PropBoard.WIDTH, getHeight() / (double) PropBoard.HEIGHT);
            board.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            board.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            paintFixedRails(board);
            for (int i = 0; i < 2; i++) { paintPlatform(board, i); }
            if (active == 2 && fittingPointer != null) {
                paintCenterDeck(board, new Rectangle(fittingPointer.x - fittingGrab.x,
                        fittingPointer.y - fittingGrab.y, centerBounds().width, centerBounds().height));
            } else {
                paintCenterDeck(board, centerBounds());
            }
        } finally {
            board.dispose();
        }
    }

    private void paintFixedRails(Graphics2D g) {
        g.setColor(new Color(48, 44, 35));
        g.fillRoundRect(95, 290, 235, 52, 10, 10);
        g.setColor(new Color(99, 88, 66));
        g.drawRoundRect(95, 290, 235, 52, 10, 10);
        railStop(g, 318, 260, 13, 112);
        railStop(g, 330, 260, 165, 10);
        railStop(g, 330, 362, 165, 10);
        railStop(g, 743, 258, 174, 12);
        railStop(g, 743, 362, 174, 12);
        railStop(g, 905, 258, 12, 116);
        railStop(g, 613, 589, 124, 14);
        railStop(g, 613, 482, 13, 121);
        railStop(g, 725, 482, 13, 121);
        BridgeArt.horizontalRails(g, 331, 899, 0);
        BridgeArt.verticalRails(g, 77, 590, 0);
        Shape socket = BridgeArt.centerShape(BridgeArt.centerBounds(true));
        g.setColor(new Color(31, 27, 23));
        g.fill(socket);
        g.setColor(platformsInPlace() ? new Color(222, 181, 115) : new Color(105, 88, 65));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1,
                new float[] {5, 4}, 0));
        g.draw(socket);
        if (!centerFitted) {
            Rectangle tray = BridgeArt.centerBounds(false);
            g.setColor(PixelTheme.LIGHT);
            g.setFont(PixelTheme.font(18));
            g.drawString("중앙 발판", tray.x + 23, tray.y + tray.height + 22);
        }
    }

    private void paintPlatform(Graphics2D g, int index) {
        BridgeArt.Placement state = placement();
        BridgeArt.paintPlatform(g, index, state, boards[index]);
        if ((hasFocus() && focused == index) || active == index) {
            Rectangle bounds = BridgeArt.platformBounds(index, state);
            g.setColor(PixelTheme.LIGHT);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        }
    }

    private void paintCenterDeck(Graphics2D g, Rectangle bounds) {
        BridgeArt.paintCenterDeck(g, bounds, boards[0]);
        g.setStroke(new BasicStroke(2));
        g.setColor(active == 2 || hasFocus() && focused == 2 ? PixelTheme.LIGHT : new Color(105, 70, 40));
        g.draw(BridgeArt.centerShape(bounds));
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

    void adjustRail(int index) {
        railOffsets[index] = railOffsets[index] == 1 ? -1 : railOffsets[index] + 1;
        repaint();
    }

    String answer() {
        if (active >= 0 || !platformsInPlace() || !centerFitted) { return ""; }
        if (railOffsets[0] == BridgeArt.HORIZONTAL_ALIGNMENT && railOffsets[1] == BridgeArt.VERTICAL_ALIGNMENT) { return "AB"; }
        return "AB" + (char) ('B' + railOffsets[0]) + (char) ('B' + railOffsets[1]);
    }

    void reset() {
        across = 0;
        down   = 0;
        active = -1;
        focused = 0;
        fittingPointer = null;
        centerFitted = false;
        java.util.Arrays.fill(railOffsets, 0);
        repaint();
    }
}
