package coderoom.app;

import coderoom.shared.ui.HandCursor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;

final class DashboardHelpDialog extends JDialog {
    private static final Color INK     = new Color(245, 222, 185);
    private static final Color WOOD    = new Color(40, 30, 24);
    private static final Color BRASS   = new Color(155, 112, 65);
    private static final Color BUTTON = new Color(68, 48, 34);
    private static final int PER_PAGE = 3;
    private static final List<HelpTopic> TOPICS = List.of(
            new HelpTopic("'코드 방탈출'은 어떤 게임인가요?",
                    "방에 숨은 단서와 코드를 읽고 장치를 조작해 출구를 여는 퍼즐 게임입니다. "
                    + "코드가 어떤 값을 만들고 어떤 조건에서 동작하는지 추리해 보세요."),
            new HelpTopic("어떻게 방을 탈출할 수 있나요?",
                    "방 안의 물건을 눌러 단서를 찾으세요. 메모와 코드를 읽고 장치에 답을 입력하거나 부품을 맞춰 보세요. "
                    + "제한 시간과 오답 기회가 남아 있을 때 퍼즐을 풀고 출구를 열면 탈출합니다."),
            new HelpTopic("진척도 기능은 어떤 기능인가요?",
                    "로비의 진척도 창에서 방별 최고기록과 맞춘 문제 수를 볼 수 있습니다. "
                    + "진척도는 방을 탈출하면 저장됩니다."),
            new HelpTopic("풀었던 문제의 답은 어디서 보나요?",
                    "로비에서 ‘풀이한 문제’를 열고 초록색 번호를 누르면 당시의 문제와 답안을 볼 수 있습니다. "
                    + "회색 번호는 저장된 풀이 기록이 없는 문제입니다."),
            new HelpTopic("오답 기회를 모두 쓰면 어떻게 되나요?",
                    "틀린 답을 제출할 때마다 남은 오답 기회가 하나 줄어듭니다. "
                    + "기회가 0이 되거나 제한 시간이 끝나면 해당 도전이 종료됩니다."),
            new HelpTopic("리더보드에는 어떤 기록이 나오나요?",
                    "방을 선택하면 가장 빠른 탈출 기록을 확인할 수 있습니다. "
                    + "같은 시간으로 탈출한 플레이어는 공동 순위이며, 내 순위와 기록은 아래에 표시됩니다."));
    private final JPanel surface = new JPanel(new BorderLayout(0, 18));
    private int page;
    private boolean showingAnswer;

    DashboardHelpDialog(JComponent owner) {
        super(SwingUtilities.getWindowAncestor(owner), "도움말", ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        surface.setBackground(WOOD);
        surface.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BRASS, 3), BorderFactory.createEmptyBorder(18, 22, 16, 22)));
        setContentPane(surface);
        getRootPane().registerKeyboardAction(event -> {
            if (showingAnswer) { showTopics(); }
            else { dispose(); }
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(590, 410);
        setLocationRelativeTo(owner);
        showTopics();
    }

    private void showTopics() {
        showingAnswer = false;
        surface.removeAll();
        surface.add(header("도움말", false), BorderLayout.NORTH);

        JPanel questions = new JPanel(new GridLayout(PER_PAGE, 1, 0, 9));
        questions.setOpaque(false);
        int start = page * PER_PAGE;
        int end   = Math.min(start + PER_PAGE, TOPICS.size());
        for (int index = start; index < end; index++) {
            HelpTopic topic   = TOPICS.get(index);
            JButton question = button((index + 1) + ".  " + topic.question());
            question.setHorizontalAlignment(SwingConstants.LEADING);
            question.addActionListener(event -> showAnswer(topic));
            questions.add(question);
        }
        surface.add(questions, BorderLayout.CENTER);

        int pages        = (TOPICS.size() + PER_PAGE - 1) / PER_PAGE;
        JPanel footer    = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        JButton back     = icon("←", "이전 페이지", () -> changePage(-1));
        JButton forward = icon("→", "다음 페이지", () -> changePage(1));
        footer.setOpaque(false);
        back.setEnabled(page > 0);
        forward.setEnabled(page + 1 < pages);
        footer.add(back);
        footer.add(label((page + 1) + " / " + pages, 15));
        footer.add(forward);
        surface.add(footer, BorderLayout.SOUTH);
        refresh();
        (forward.isEnabled() ? forward : back).requestFocusInWindow();
    }

    private void changePage(int direction) {
        page += direction;
        showTopics();
    }

    private void showAnswer(HelpTopic topic) {
        showingAnswer = true;
        surface.removeAll();
        JPanel heading = header(topic.question(), true);
        surface.add(heading, BorderLayout.NORTH);

        BufferedImage art = SceneObjectButton.read("help-answer.png");
        JPanel answer     = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                graphics.drawImage(art, 0, 0, getWidth(), getHeight(), null);
            }
        };
        answer.setBorder(BorderFactory.createEmptyBorder(42, 38, 32, 38));
        JTextArea description = new JTextArea(topic.answer());
        description.setEditable(false);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(SceneObjectButton.PixelFont.at(20));
        description.setForeground(INK);
        description.setCaretPosition(0);
        description.getAccessibleContext().setAccessibleName("답변");
        answer.add(description, BorderLayout.CENTER);
        surface.add(answer, BorderLayout.CENTER);
        refresh();
        heading.getComponent(0).requestFocusInWindow();
    }

    private JPanel header(String title, boolean detail) {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        JLabel text   = label(title, detail ? 20 : 29);
        header.setOpaque(false);
        if (detail) {
            header.add(icon("←", "질문 목록으로 돌아가기", this::showTopics), BorderLayout.WEST);
        } else {
            header.add(javax.swing.Box.createHorizontalStrut(40), BorderLayout.WEST);
        }
        header.add(text, BorderLayout.CENTER);
        header.add(icon("×", "닫기", this::dispose), BorderLayout.EAST);
        return header;
    }

    private void refresh() {
        surface.revalidate();
        surface.repaint();
    }

    private static JLabel label(String text, float size) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(SceneObjectButton.PixelFont.at(size));
        label.setForeground(INK);
        return label;
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(SceneObjectButton.PixelFont.at(19));
        button.setForeground(INK);
        button.setBackground(BUTTON);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BRASS), BorderFactory.createEmptyBorder(3, 14, 3, 14)));
        HandCursor.install(button);
        return button;
    }

    private static JButton icon(String symbol, String name, Runnable action) {
        JButton button = button(symbol);
        button.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        button.setBorder(BorderFactory.createLineBorder(BRASS));
        button.setPreferredSize(new Dimension(40, 36));
        button.setToolTipText(name);
        button.getAccessibleContext().setAccessibleName(name);
        button.addActionListener(event -> action.run());
        return button;
    }

    private record HelpTopic(String question, String answer) { }
}
