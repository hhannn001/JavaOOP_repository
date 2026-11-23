package coderoom.features.gameplay.ui;

import coderoom.shared.audio.GameAudio;
import coderoom.shared.ui.PixelTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** The two mechanical locks on the study map. */
final class StudyLockPanel extends PropBoard {
    private static final String ART = "/images/game-ui/study/";
    private final DigitControl[] digits;
    private final Control latch;
    private final int latchX;
    private final int latchY;
    private final Timer rattle;
    private int pulse;

    StudyLockPanel(boolean drawer, Runnable submit) {
        super(ART + (drawer ? "drawer.jpg" : "wall.jpg"));
        BufferedImage wheel = read(ART + (drawer ? "wheel.png" : "dial.png"));
        String action = drawer ? "서랍 열기  →" : "다이얼 확인  →";
        if (drawer) {
            digits = new DigitControl[] {new DigitControl(wheel, 4, "서랍 네 자리 암호", submit)};
            place(digits[0], 281, 275, 438, 84);
        } else {
            digits = new DigitControl[9];
            for (int i = 0; i < digits.length; i++) {
                int row    = i / 3;
                int column = i % 3;
                String name = "다이얼 " + (row + 1) + "행 " + (column + 1) + "열";
                digits[i] = new DigitControl(wheel, 1, name, submit);
                place(digits[i], 283 + column * 156, 106 + row * 125, 121, 121);
            }
        }
        latchX = drawer ? 438 : 455;
        latchY = drawer ? 441 : 478;
        latch  = new Control("studyLockLatch", action);
        latch.sprite(read(ART + (drawer ? "pull.png" : "latch.png")));
        latch.addActionListener(event -> submit.run());
        place(latch, latchX, latchY, drawer ? 124 : 92, drawer ? 126 : 68);
        rattle = new Timer(32, event -> {
            pulse--;
            int offset = (pulse % 2 == 0 ? -1 : 1) * pulse;
            move(latch, latchX + offset, latchY);
            if (pulse == 0) { ((Timer) event.getSource()).stop(); }
        });
    }

    JTextField firstDigit() { return digits[0]; }

    String answer() {
        var answer = new StringBuilder();
        for (DigitControl dial : digits) { answer.append(dial.getText()); }
        return answer.toString();
    }

    void reset() {
        rattle.stop();
        move(latch, latchX, latchY);
        for (DigitControl dial : digits) { dial.setText("0".repeat(dial.positions)); }
    }

    void reject() {
        pulse = 8;
        move(latch, latchX - pulse, latchY);
        rattle.restart();
    }

    private static final class DigitControl extends JTextField {
        private final BufferedImage wheel;
        private final int positions;
        private int activePosition;
        private int dragOrigin;
        private int dragSteps;
        private boolean hovered;

        DigitControl(BufferedImage wheel, int positions, String description, Runnable submit) {
            super("0".repeat(positions), positions);
            this.wheel     = wheel;
            this.positions = positions;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(description);
            setToolTipText("눌러 돌리거나 끌어서 돌리기 · 숫자 입력 가능");
            ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attrs)
                        throws BadLocationException {
                    replace(bypass, offset, 0, text, attrs);
                }

                @Override public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    var document = bypass.getDocument();
                    String old   = document.getText(0, document.getLength());
                    String value = old.substring(0, offset) + (text == null ? "" : text) + old.substring(offset + length);
                    if (value.length() <= positions && value.chars().allMatch(digit -> digit >= '0' && digit <= '9')) {
                        super.replace(bypass, offset, length, text, attrs);
                    }
                }
            });
            addActionListener(event -> submit.run());
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) {
                    activePosition = positionAt(event.getX());
                    dragOrigin     = event.getY();
                    dragSteps      = 0;
                }

                @Override public void mouseClicked(MouseEvent event) {
                    if (dragSteps == 0 && (SwingUtilities.isLeftMouseButton(event) || SwingUtilities.isRightMouseButton(event))) {
                        rotate(positionAt(event.getX()), SwingUtilities.isRightMouseButton(event) ? -1 : 1);
                    }
                    dragSteps = 0;
                }

                @Override public void mouseDragged(MouseEvent event) {
                    if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) { return; }
                    int steps = (dragOrigin - event.getY()) / Math.max(7, getHeight() / 7);
                    if (steps != dragSteps) { rotate(activePosition, steps - dragSteps); }
                    dragSteps = steps;
                }

                @Override public void mouseEntered(MouseEvent event) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent event) { hovered = false; repaint(); }
                @Override public void mouseMoved(MouseEvent event) { activePosition = positionAt(event.getX()); repaint(); }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private int positionAt(int x) {
            return Math.max(0, Math.min(positions - 1, x * positions / Math.max(1, getWidth())));
        }

        private void rotate(int index, int delta) {
            char[] value = (getText() + "0".repeat(positions)).substring(0, positions).toCharArray();
            value[index] = (char) ('0' + Math.floorMod(value[index] - '0' + delta, 10));
            GameAudio.play(GameAudio.Cue.METAL);
            setText(new String(value));
            select(index, index + 1);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cell = getWidth() / positions;
                int size = Math.min(cell, getHeight()) - 4;
                if (size <= 0) { return; }
                String value = (getText() + "0".repeat(positions)).substring(0, positions);
                for (int i = 0; i < positions; i++) {
                    int x = i * cell + cell / 2;
                    int y = getHeight() / 2;
                    char digit = value.charAt(i);
                    var pose = g.getTransform();
                    if (positions == 1) { g.rotate(Math.toRadians((digit - '0') * 36), x, y); }
                    sprite(g, wheel, x - size / 2, y - size / 2, size, size);
                    g.setTransform(pose);
                    if (i == activePosition && (hovered || hasFocus())) {
                        g.setColor(new Color(255, 222, 143, 180));
                        g.setStroke(new BasicStroke(1.5f));
                        if (positions == 1) { g.drawOval(x - size / 2, y - size / 2, size, size); }
                        else { g.drawRoundRect(x - size / 2, y - size / 2, size, size, 6, 6); }
                    }
                    g.setFont(PixelTheme.font(size * .55f).deriveFont(java.awt.Font.BOLD));
                    int left = x - g.getFontMetrics().charWidth(digit) / 2;
                    int base = y + (g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
                    g.setColor(new Color(27, 17, 10));
                    g.drawString(String.valueOf(digit), left + 1, base + 1);
                    g.setColor(PixelTheme.LIGHT);
                    g.drawString(String.valueOf(digit), left, base);
                }
            } finally {
                g.dispose();
            }
        }
    }
}
