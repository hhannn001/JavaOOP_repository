package coderoom.widgets.clue;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import coderoom.features.gameplay.ItemView;
import coderoom.shared.ui.PixelTheme;

/**
 * 담당: F
 * 역할: 선택한 메모의 설명과 코드를 펼친 종이 형태로 표시한다.
 * 게임 스냅샷을 표시한다. DB 조회와 정답 판정은 하지 않는다.
 */
public final class CluePanel extends JPanel {
    private final JLabel title       = new JLabel();
    private final ClueReading code    = new ClueReading();
    private final JPanel codePage     = new JPanel(new BorderLayout());
    private final JScrollPane scroll  = new JScrollPane();
    private final BalanceDiagram balance = new BalanceDiagram();
    private final CardLayout content  = new CardLayout();
    private final JPanel pages        = new JPanel(content);
    private final JButton switchView  = new JButton();
    private final JPanel header = new JPanel(new BorderLayout(8, 0));
    private boolean codeVisible;
    private int measuredWidth = -1;
    private int measuredLimit;
    private int measuredBodyHeight;
    private Runnable onClose;

    public CluePanel() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(17, 22, 19, 22));
        setPreferredSize(new Dimension(500, 650));
        header.setOpaque(false);
        title.setFont(PixelTheme.font(24).deriveFont(Font.BOLD));
        title.setBorder(BorderFactory.createEmptyBorder(8, 6, 0, 0));
        title.setForeground(PixelTheme.INK);
        JButton close = new JButton("닫기");
        close.setFont(PixelTheme.font(16));
        close.setForeground(PixelTheme.INK);
        close.setContentAreaFilled(false);
        close.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 10));
        close.addActionListener(event -> closeClue());
        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        pages.setOpaque(false);
        codePage.setOpaque(false);
        codePage.add(code);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        pages.add(codePage, "code");
        pages.add(balance, "balance");
        JPanel reading = new JPanel(new BorderLayout(0, 8));
        reading.setOpaque(false);
        reading.add(pages, BorderLayout.CENTER);
        switchView.setName("clueViewToggle");
        switchView.setFont(PixelTheme.font(14));
        switchView.setForeground(PixelTheme.INK);
        switchView.setContentAreaFilled(false);
        switchView.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(155, 113, 63)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        switchView.addActionListener(event -> showReading(!codeVisible));
        reading.add(switchView, BorderLayout.SOUTH);
        add(reading, BorderLayout.CENTER);
        setVisible(false);
    }

    public void setClueData(ItemView value) {
        title.setText(value.title());
        measuredWidth = -1;
        code.showNote(value.content());
        balance.setLoad(value.balanceLoad());
        switchView.setVisible(value.diagram() == ItemView.Diagram.BALANCE);
        showReading(value.diagram() == ItemView.Diagram.NONE);
        revalidate();
        repaint();
    }

    private void showReading(boolean showCode) {
        codeVisible = showCode;
        content.show(pages, showCode ? "code" : "balance");
        switchView.setText(showCode ? "그림 다시 보기" : "Java 규칙 보기");
        if (getParent() != null) { getParent().revalidate(); }
    }

    public int paperTopInset(int height) {
        // paper.png 높이 1536px 중 약 34px가 위쪽 투명 여백이다.
        return Math.round(height * (34f / 1536));
    }

    public int readingHeight(int width, int limit) {
        if (!codeVisible) { return getPreferredSize().height; }
        var insets  = getInsets();
        int footer  = switchView.isVisible() ? switchView.getPreferredSize().height + 8 : 0;
        int frame   = insets.top + insets.bottom + header.getPreferredSize().height + 12 + footer;
        if (width != measuredWidth || limit != measuredLimit) {
            int bodyWidth = width - insets.left - insets.right;
            int bodyLimit = limit - frame;
            int bodyHeight = code.fit(bodyWidth, bodyLimit);
            boolean overflow = bodyHeight > bodyLimit;
            if (overflow) {
                bodyHeight = code.fit(bodyWidth - scroll.getVerticalScrollBar().getPreferredSize().width, bodyLimit);
            }
            setScrolling(overflow);
            measuredBodyHeight = bodyHeight;
            measuredWidth      = width;
            measuredLimit      = limit;
        }
        return frame + measuredBodyHeight;
    }

    private void setScrolling(boolean overflow) {
        if (overflow == (scroll.getParent() == codePage)) { return; }
        codePage.removeAll();
        if (overflow) {
            scroll.setViewportView(code);
            codePage.add(scroll);
        } else {
            scroll.setViewportView(null);
            codePage.add(code);
        }
    }

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
}
