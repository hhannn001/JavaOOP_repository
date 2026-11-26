package coderoom.app;

import coderoom.features.dashboard.DashboardRecords;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

/** 로비의 책장과 달력을 확장한 픽셀 기록 화면. */
final class DashboardRecordSurface extends JPanel {
    private static final Color INK   = new Color(239, 216, 174);
    private static final Color DARK  = new Color(54, 47, 37);
    private static final Color GREEN = new Color(52, 108, 72);
    private static final Color GRAY  = new Color(83, 81, 78);
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

    static JPanel progressRow(DashboardRecords.RoomProgress entry) {
        JPanel row    = recordRow();
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(text(entry.room(), 18, DARK), BorderLayout.CENTER);
        String best = entry.seconds() == null ? "—" : DashboardRecords.time(entry.seconds());
        header.add(text("최고기록  " + best, 15, DARK), BorderLayout.EAST);
        row.add(header, BorderLayout.NORTH);

        int total  = entry.problems().size();
        int solved = entry.solvedCount();
        JProgressBar bar = new JProgressBar(0, Math.max(1, total));
        bar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override protected Color getSelectionForeground() { return Color.WHITE; }
            @Override protected Color getSelectionBackground() { return Color.WHITE; }
        });
        bar.setValue(solved);
        bar.setString(solved + " / " + total);
        bar.setStringPainted(true);
        bar.setFont(SceneObjectButton.PixelFont.at(15));
        bar.setForeground(GREEN);
        bar.setBackground(GRAY);
        bar.setOpaque(true);
        bar.setBorder(BorderFactory.createLineBorder(DARK));
        bar.setPreferredSize(new Dimension(0, 24));
        bar.getAccessibleContext().setAccessibleName(entry.room() + " " + bar.getString());
        row.add(bar, BorderLayout.SOUTH);
        return row;
    }

    static JPanel problemRow(DashboardRecords.RoomProgress entry, Consumer<DashboardRecords.Solved> open) {
        JPanel row   = recordRow();
        JPanel slots = new JPanel(new FlowLayout(FlowLayout.LEADING, 7, 0));
        slots.setOpaque(false);
        row.add(text(entry.room(), 18, INK), BorderLayout.NORTH);
        row.add(slots, BorderLayout.SOUTH);
        for (DashboardRecords.Problem problem : entry.problems()) {
            String number  = Integer.toString(problem.number());
            boolean solved = problem.solution().isPresent();
            Color fill     = solved ? GREEN : GRAY;
            JLabel locked  = text(number, 17, INK);
            JButton button = new JButton(number);
            javax.swing.JComponent slot = solved ? button : locked;
            locked.setHorizontalAlignment(SwingConstants.CENTER);
            button.setFont(SceneObjectButton.PixelFont.at(17));
            button.setForeground(Color.WHITE);
            button.setMargin(new Insets(0, 0, 0, 0));
            problem.solution().ifPresent(answer -> button.addActionListener(event -> open.accept(answer)));
            slot.setOpaque(true);
            slot.setBackground(fill);
            slot.setBorder(BorderFactory.createLineBorder(solved ? new Color(132, 183, 134) : fill.brighter()));
            slot.setPreferredSize(new Dimension(42, 34));
            slot.getAccessibleContext().setAccessibleName(entry.room() + " 문제 " + number
                    + (solved ? " 정답 보기" : " 미달성"));
            slots.add(slot);
        }
        return row;
    }

    private static JPanel recordRow() {
        JPanel row = new JPanel(new BorderLayout(0, 6));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        return row;
    }

    private static JLabel text(String value, float size, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(SceneObjectButton.PixelFont.at(size));
        label.setForeground(color);
        return label;
    }
}
