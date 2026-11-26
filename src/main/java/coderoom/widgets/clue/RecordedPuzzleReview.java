package coderoom.widgets.clue;

import coderoom.features.gameplay.ui.PuzzleDevicePreview;
import coderoom.shared.ui.PixelTheme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** A read-only view of the saved note beside the equipment it describes. */
public final class RecordedPuzzleReview extends JPanel {
    private static final Dimension REVIEW_SIZE = new Dimension(1040, 585);
    public RecordedPuzzleReview(String room, int step, long puzzleId, String code, String answer) {
        super(new BorderLayout());
        RecordedPuzzleGuide guide = RecordedPuzzleGuide.forPuzzle(puzzleId, code, answer);
        setBackground(PixelTheme.DARK);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(REVIEW_SIZE);

        JPanel columns = new JPanel(new GridLayout(1, 0, 16, 0));
        columns.setOpaque(false);
        columns.add(note(room + " · 문제 " + step, guide, puzzleId, code, answer));
        columns.add(equipment(guide, puzzleId, code, answer));
        add(columns);
    }

    private static JPanel note(String location, RecordedPuzzleGuide guide, long puzzleId, String code, String answer) {
        JPanel paper = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                PixelTheme.asset(g, "paper", 0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        paper.setOpaque(false);
        paper.setBorder(BorderFactory.createEmptyBorder(23, 24, 25, 24));

        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        heading.add(label(location, 14, PixelTheme.INK), BorderLayout.NORTH);
        heading.add(label(guide.title(), 24, PixelTheme.INK), BorderLayout.CENTER);
        JTextArea task = text(guide.task(), 16, PixelTheme.INK);
        heading.add(task, BorderLayout.SOUTH);
        paper.add(heading, BorderLayout.NORTH);

        var reading = new ClueReading();
        reading.showNote(code);
        JScrollPane scroll = new JScrollPane(reading, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                reading.fit(scroll.getViewport().getWidth(), scroll.getViewport().getHeight());
            }
        });

        var layout = new CardLayout();
        JPanel views = new JPanel(layout);
        views.setOpaque(false);
        views.add(scroll, "code");
        RecordedPuzzleDiagram.create(puzzleId, code, answer).ifPresent(diagram -> {
            views.add(diagram, "diagram");
            var toggle = PixelTheme.button("그림으로 보기");
            toggle.addActionListener(event -> {
                boolean showDiagram = !diagram.isVisible();
                layout.show(views, showDiagram ? "diagram" : "code");
                toggle.setText(showDiagram ? "코드와 안내 보기" : "그림으로 보기");
            });
            paper.add(toggle, BorderLayout.SOUTH);
            if (puzzleId == 313 && code.contains("분배 작업표")) {
                layout.show(views, "diagram");
                toggle.setText("코드와 안내 보기");
            }
        });
        paper.add(views, BorderLayout.CENTER);
        return paper;
    }

    private static JPanel equipment(RecordedPuzzleGuide guide, long puzzleId, String code, String answer) {
        JPanel right = new JPanel(new BorderLayout(0, 10)) {
            @Override public void doLayout() {
                var layout = (BorderLayout) getLayout();
                var picture = layout.getLayoutComponent(BorderLayout.CENTER);
                var details = layout.getLayoutComponent(BorderLayout.SOUTH);
                if (picture != null && details != null) {
                    int detailsHeight = details.getPreferredSize().height;
                    int artHeight     = Math.min(getWidth() * 2 / 3, Math.max(0, getHeight() - detailsHeight - 10));
                    picture.setBounds(0, 0, getWidth(), artHeight);
                    details.setBounds(0, artHeight + 10, getWidth(), detailsHeight);
                } else {
                    super.doLayout();
                }
            }
        };
        right.setOpaque(false);
        PuzzleDevicePreview.render(puzzleId, code, answer).ifPresent(art -> {
            JPanel picture = new JPanel() {
                @Override protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    double scale = Math.min(getWidth() / (double) art.getWidth(), getHeight() / (double) art.getHeight());
                    int width    = (int) (art.getWidth() * scale);
                    int height   = (int) (art.getHeight() * scale);
                    graphics.drawImage(art, (getWidth() - width) / 2, 0, width, height, this);
                }
            };
            picture.setOpaque(false);
            picture.getAccessibleContext().setAccessibleName(guide.title() + " 장치 그림");
            right.add(picture, BorderLayout.CENTER);
        });
        JPanel solution = new JPanel(new BorderLayout(0, 5));
        solution.setBackground(new Color(52, 40, 28));
        solution.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        String heading = switch ((int) puzzleId) {
            case 313 -> code.contains("분배 작업표") ? "풀이 · 그림은 현재 장치 구성" : "풀이";
            case 402 -> code.contains("왼쪽 화분") ? "풀이 · 그림은 현재 장치에 재현한 배치" : "풀이";
            case 404 -> code.contains("seedWeight") ? "풀이 · 그림은 현재 장치 구성" : "풀이";
            default -> "풀이";
        };
        solution.add(label(heading, 18, PixelTheme.LIGHT), BorderLayout.NORTH);
        JTextArea explanation = text(guide.solution(), 16, PixelTheme.LIGHT);
        explanation.setRows(2);
        solution.add(explanation, BorderLayout.CENTER);
        solution.add(label("저장된 정답  " + answer, 13, PixelTheme.LIGHT), BorderLayout.SOUTH);
        right.add(solution, BorderLayout.SOUTH);
        return right;
    }

    private static JLabel label(String value, int size, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(PixelTheme.font(size).deriveFont(Font.BOLD));
        label.setForeground(color);
        return label;
    }

    private static JTextArea text(String value, int size, Color color) {
        JTextArea area = new JTextArea(value);
        area.setFont(PixelTheme.font(size));
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }
}
