package coderoom.app;

import coderoom.features.dashboard.DashboardRecords;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** 로비의 책장과 달력을 확장한 픽셀 기록 화면. */
final class DashboardRecordSurface extends JPanel {
    private static final Color INK   = new Color(239, 216, 174);
    private static final Color WOOD  = new Color(113, 72, 43);
    private static final Color PAPER = new Color(226, 208, 169);
    private final BufferedImage artwork;

    DashboardRecordSurface(boolean calendar) {
        artwork = SceneObjectButton.read(calendar ? "calendar-progress.png" : "bookshelf-records.png");
    }

    @Override public Insets getInsets() {
        return new Insets((int) (getHeight() * 0.21), (int) (getWidth() * 0.11),
                (int) (getHeight() * 0.15), (int) (getWidth() * 0.11));
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.drawImage(artwork, 0, 0, getWidth(), getHeight(), null);
    }

    static JPanel card(DashboardRecords.Progress entry, int index, boolean calendar) {
        boolean complete = entry.seconds() != null;
        JPanel card      = new JPanel(new BorderLayout(16, 0));
        Color accent     = calendar ? new Color(89, 113, 84) : new Color(121, 67, 51);
        card.setPreferredSize(new Dimension(380, 94));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, calendar ? 3 : 10, 0, calendar ? new Color(160, 144, 113) : WOOD),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel badge = text(calendar ? (complete ? "완료" : "대기") : String.format("%02d", index), 20,
                complete ? INK : new Color(78, 72, 62));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(complete ? accent : new Color(198, 182, 150));
        badge.setPreferredSize(new Dimension(62, 60));
        badge.setBorder(BorderFactory.createMatteBorder(3, 5, 3, 2, complete ? accent.darker() : PAPER));
        card.add(badge, BorderLayout.WEST);
        JPanel details = new JPanel(new java.awt.GridLayout(2, 1, 0, 6));
        details.setOpaque(false);
        Color foreground = calendar ? new Color(54, 47, 37) : INK;
        JLabel room      = text(entry.room(), 19, foreground);
        room.setToolTipText(entry.room());
        details.add(room);
        details.add(text(complete ? "탈출 완료 · 최고 기록 " + DashboardRecords.time(entry.seconds())
                : "아직 탈출 기록이 없습니다", 15, foreground));
        card.add(details, BorderLayout.CENTER);
        card.getAccessibleContext().setAccessibleName(entry.room() + (complete ? " 탈출 완료" : " 기록 없음"));
        return card;
    }

    static JPanel solvedCard(DashboardRecords.Solved entry) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 8, 0, WOOD),
                BorderFactory.createEmptyBorder(12, 12, 16, 12)));
        JLabel heading = text(entry.room() + " · 문제 " + entry.stepNo(), 18, INK);
        heading.setToolTipText(heading.getText());
        card.add(heading, BorderLayout.NORTH);
        javax.swing.JTextArea code = new javax.swing.JTextArea(entry.code());
        code.setEditable(false);
        code.setOpaque(false);
        code.setForeground(INK);
        code.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 15));
        code.setCaretPosition(0);
        code.getAccessibleContext().setAccessibleName(heading.getText() + " 코드");
        card.add(code, BorderLayout.CENTER);
        String answer = entry.answer();
        if (answer.length() == 9) {
            answer = answer.substring(0, 3) + " / " + answer.substring(3, 6) + " / " + answer.substring(6);
        }
        card.add(text("정답  " + answer, 18, new Color(224, 184, 103)), BorderLayout.SOUTH);
        return card;
    }

    private static JLabel text(String value, float size, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(SceneObjectButton.PixelFont.at(size));
        label.setForeground(color);
        return label;
    }
}
