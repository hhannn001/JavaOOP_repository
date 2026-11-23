package coderoom.features.gameplay.ui;

import coderoom.shared.ui.PixelTheme;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Four movable pieces and one or four target slots. The caller checks the submitted order. */
final class DragPuzzlePanel extends JPanel {
    private final String labels;
    private final int targetCount;
    private final BufferedImage artwork;
    private final String[] marks;
    private final int[] pieces = new int[8];
    private int selected = -1;
    private int focused;
    private Point pointer;

    DragPuzzlePanel(String labels, int targetCount) {
        this(labels, targetCount, null, null);
    }

    DragPuzzlePanel(String labels, int targetCount, BufferedImage artwork, String[] marks) {
        this.artwork     = artwork;
        this.marks       = marks;
        this.labels      = labels;
        this.targetCount = targetCount;
        setFocusable(true);
        getAccessibleContext().setAccessibleName("조작 퍼즐. 방향키로 이동하고 스페이스로 선택하거나 놓습니다.");
        setBackground(PixelTheme.DARK);
        setOpaque(artwork == null);
        reset();
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                if (isEnabled() && SwingUtilities.isLeftMouseButton(event)) {
                    requestFocusInWindow();
                    int index = indexAt(event.getPoint());
                    if (index >= 0 && pieces[index] >= 0) {
                        selected = index;
                        pointer  = event.getPoint();
                        focused  = index;
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        repaint();
                    }
                }
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (isEnabled() && pointer != null) {
                    pointer = event.getPoint();
                    repaint();
                }
            }

            @Override public void mouseReleased(MouseEvent event) {
                if (pointer != null) {
                    if (isEnabled()) { drop(indexAt(event.getPoint())); }
                    clearSelection();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (!isEnabled()) { return; }
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> focused = Math.max(focused - 1, 0);
                    case KeyEvent.VK_RIGHT -> focused = Math.min(focused + 1, 3 + targetCount);
                    case KeyEvent.VK_UP -> focused = focused < 4 ? focused : focused - 4;
                    case KeyEvent.VK_DOWN -> focused = focused < 4 ? 4 + Math.min(focused, targetCount - 1) : focused;
                    case KeyEvent.VK_SPACE -> {
                        if (selected < 0 && pieces[focused] >= 0) {
                            selected = focused;
                        } else if (selected >= 0) {
                            drop(focused);
                            clearSelection();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> clearSelection();
                    default -> { return; }
                }
                repaint();
            }
        });
    }

    void reset() {
        Arrays.fill(pieces, -1);
        for (int i = 0; i < 4; i++) {
            pieces[i] = i;
        }
        focused = 0;
        clearSelection();
    }

    String answer() {
        StringBuilder result = new StringBuilder();
        for (int i = 4; i < 4 + targetCount; i++) {
            if (pieces[i] < 0) { return ""; }
            result.append(labels.charAt(pieces[i]));
        }
        return result.toString();
    }

    private void drop(int target) {
        if (target < 0 || target == selected) { return; }
        int displaced    = pieces[target];
        pieces[target]   = pieces[selected];
        pieces[selected] = displaced;
    }

    private void clearSelection() {
        pointer  = null;
        selected = -1;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        repaint();
    }

    private Rectangle slot(int index) {
        int width   = Math.max(24, Math.min(80, (getWidth() - 48) / 4));
        int rowSize = index < 4 ? 4 : targetCount;
        int column  = index < 4 ? index : index - 4;
        int x       = (getWidth() - rowSize * (width + 8)) / 2 + column * (width + 8) + 4;
        int y       = getHeight() * (index < 4 ? 21 : 62) / 100;
        return new Rectangle(x, y, width, Math.max(22, Math.min(58, getHeight() / 4)));
    }

    private int indexAt(Point point) {
        for (int i = 0; i < 4 + targetCount; i++) {
            if (slot(i).contains(point)) { return i; }
        }
        return -1;
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.setFont(PixelTheme.font(16));
        graphics.setColor(PixelTheme.LIGHT);
        if (artwork == null) {
            graphics.drawString("옮길 사물", 16, 22);
            graphics.drawString("놓을 자리", 16, Math.max(24, getHeight() * 62 / 100 - 10));
        }
        graphics.setFont(PixelTheme.font(26));
        for (int i = 0; i < 4 + targetCount; i++) {
            Rectangle bounds = slot(i);
            boolean filled   = pieces[i] >= 0 && (i != selected || pointer == null);
            paintPiece(graphics, bounds, filled ? pieces[i] : -1);
            if (i == selected || hasFocus() && i == focused) {
                graphics.setColor(Color.WHITE);
                graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            }
        }
        if (pointer != null) {
            Rectangle drag = slot(selected);
            drag.setLocation(pointer.x - drag.width / 2, pointer.y - drag.height / 2);
            paintPiece(graphics, drag, pieces[selected]);
        }
    }

    private void paintBand(Graphics2D g, Rectangle bounds, int piece) {
        double scale = Math.min(bounds.width / (double) artwork.getWidth(),
                bounds.height / (double) artwork.getHeight());
        int width  = (int) (artwork.getWidth() * scale);
        int height = (int) (artwork.getHeight() * scale);
        int center = bounds.x + (bounds.width - width) / 2 + width * 36 / 100;
        int top    = bounds.y + (bounds.height - height) / 2 + height * 36 / 100;
        int length = Math.max(5, height / 3);
        int step   = Math.max(3, width / 12);
        int bars   = piece == 3 ? 1 : piece + 1;
        int band   = piece == 3 ? step * 2 : Math.max(1, step / 2);
        for (int i = 0; i < bars; i++) {
            int x = center + i * step - ((bars - 1) * step + band) / 2;
            g.setColor(new Color(51, 30, 16, 220));
            g.fillRoundRect(x, top, band, length, 2, 2);
            g.setColor(new Color(200, 159, 96, 225));
            g.drawLine(x, top + 1, x, top + length - 1);
        }
    }

    private void paintPiece(Graphics graphics, Rectangle bounds, int piece) {
        if (artwork != null) {
            if (piece < 0) {
                Graphics2D g = (Graphics2D) graphics.create();
                int y = bounds.y + bounds.height * 2 / 3;
                g.setColor(new Color(42, 23, 13, 215));
                g.fillOval(bounds.x + 2, y - 4, bounds.width - 4, bounds.height / 3);
                g.setColor(new Color(171, 114, 57));
                g.drawArc(bounds.x + 3, y - 5, bounds.width - 6, bounds.height / 3 + 3, 185, 170);
                g.dispose();
            } else {
                Graphics2D g = (Graphics2D) graphics.create();
                PropBoard.sprite(g, artwork, bounds.x, bounds.y, bounds.width, bounds.height);
                paintBand(g, bounds, piece);
                PropBoard.label(g, marks[piece], bounds.x + bounds.width / 2, bounds.y + bounds.height + 13, 12);
                g.dispose();
            }
            return;
        }
        graphics.setColor(piece < 0 ? new Color(66, 54, 43) : PixelTheme.PAPER);
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        if (piece >= 0) {
            String label = String.valueOf(labels.charAt(piece));
            graphics.setColor(PixelTheme.INK);
            graphics.drawString(label, bounds.x + (bounds.width - graphics.getFontMetrics().stringWidth(label)) / 2,
                    bounds.y + (bounds.height + graphics.getFontMetrics().getAscent()) / 2 - 3);
        }
    }
}
