package coderoom.widgets.clue;

import coderoom.entities.game.ItemId;
import coderoom.features.gameplay.ItemView;
import coderoom.shared.ui.PixelTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;
import java.util.EnumSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * 담당: F
 * 역할: 선택한 메모의 코드와 힌트를 펼친 종이 형태로 표시한다.
 * 게임 스냅샷을 표시하며 메모별 힌트 공개 상태를 유지한다. DB 조회와 정답 판정은 하지 않는다.
 */
public class CluePanel extends JPanel {
    private final JLabel title     = new JLabel();
    private final JTextArea code   = new JTextArea();
    private final HintButton hint  = new HintButton();
    private final EnumSet<ItemId> revealed = EnumSet.noneOf(ItemId.class);
    private ItemView item;
    private Runnable onClose;

    public CluePanel() {
        setLayout(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 26, 28, 26));
        setPreferredSize(new Dimension(370, 560));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        title.setFont(PixelTheme.font(24));
        title.setForeground(PixelTheme.INK);
        JButton close = new JButton("접어두기  X");
        close.setFont(PixelTheme.font(12));
        close.setForeground(PixelTheme.INK);
        close.setContentAreaFilled(false);
        close.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        close.addActionListener(event -> closeClue());
        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        code.setEditable(false);
        code.setFont(PixelTheme.font(14));
        code.setForeground(PixelTheme.INK);
        code.setOpaque(false);
        code.setTabSize(2);
        JScrollPane scroll = new JScrollPane(code);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        hint.setPreferredSize(new Dimension(280, 136));
        hint.setName("revealHint");
        hint.addActionListener(event -> {
            if (item != null && !item.hint().isBlank()) {
                revealed.add(item.id());
                hint.setHint(item.hint(), true);
            }
        });
        add(hint, BorderLayout.SOUTH);
        setVisible(false);
    }

    public void setClueData(ItemView value) {
        item = value;
        title.setText(value.title());
        code.setText(value.content());
        code.setCaretPosition(0);
        hint.setVisible(!value.hint().isBlank());
        hint.setHint(value.hint(), revealed.contains(value.id()));
        revalidate();
        repaint();
    }

    public boolean isHintRevealed(ItemId id) { return revealed.contains(id); }
    public void openClue() { setVisible(true); revalidate(); repaint(); }
    public void closeClue() {
        setVisible(false);
        if (onClose != null) { onClose.run(); }
    }
    public void setOnClose(Runnable callback) { onClose = callback; }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        PixelTheme.asset(g, "paper", 0, 0, getWidth(), getHeight());
        g.dispose();
    }

    private static final class HintButton extends JButton {
        private String content = "";
        private boolean open;
        private BufferedImage blurred;
        private int cachedWidth;

        private HintButton() {
            setContentAreaFilled(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            setBorderPainted(false);
            setFocusPainted(false);
        }

        private void setHint(String value, boolean shown) {
            content = value;
            open    = shown;
            blurred = null;
            getAccessibleContext().setAccessibleName(shown ? "힌트: " + value : "클릭하여 작은 힌트 공개");
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(new Color(127, 91, 45));
            g.drawLine(0, 0, getWidth(), 0);
            g.setFont(PixelTheme.font(16));
            g.setColor(PixelTheme.INK);
            g.drawString("작은 힌트", 2, 25);
            int width = Math.max(1, getWidth());
            if (blurred == null || cachedWidth != width) {
                BufferedImage text = new BufferedImage(width, 84, BufferedImage.TYPE_INT_ARGB);
                Graphics2D ink     = text.createGraphics();
                ink.setFont(PixelTheme.font(14));
                ink.setColor(PixelTheme.INK);
                int x = 2;
                int y = 18;
                for (char character : content.toCharArray()) {
                    int advance = ink.getFontMetrics().charWidth(character);
                    if (x + advance > width - 4) { x = 2; y += 20; }
                    ink.drawString(String.valueOf(character), x, y);
                    x += advance;
                }
                ink.dispose();
                if (open) {
                    blurred = text;
                } else {
                    float[] kernel = new float[81];
                    Arrays.fill(kernel, 1f / 81f);
                    blurred = new ConvolveOp(new Kernel(9, 9, kernel), ConvolveOp.EDGE_NO_OP, null).filter(text, null);
                }
                cachedWidth = width;
            }
            g.drawImage(blurred, 0, 36, null);
            if (hasFocus()) {
                g.drawLine(2, getHeight() - 1, getWidth() - 3, getHeight() - 1);
            }
            g.dispose();
        }
    }
}
