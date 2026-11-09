package coderoom.widgets.clue;
import coderoom.features.gameplay.ItemView;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 담당: F
 * 역할: 게임 화면에서 단서 클릭 시 나타나는 패널
 *
 * 실제 데이터 조회 및 게임 로직은 구현하지 않음.
 */
public class CluePanel extends JPanel {

    // 디자인 색상
    private static final Color PAPER = new Color(232, 228, 195);
    private static final Color DARK = new Color(35, 55, 42);
    private static final Color CODE_BG = new Color(215, 215, 180);
    private static final Color TEXT = new Color(65, 70, 50);
    private static final Color GOLD = new Color(173, 160, 111);

    // 단서 표시 컴포넌트
    private final JLabel numberLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel answerLabel = new JLabel();

    private final JTextArea descriptionArea = new JTextArea();
    private final JTextArea codeArea = new JTextArea();
    private final JTextArea hintArea = new JTextArea();

    private final JPanel hintContent = new JPanel(new BorderLayout());
    private final JButton hintButton = new JButton("♧  작은 힌트   +");

    private Runnable onClose;

    public CluePanel() {

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setPreferredSize(new Dimension(350, 540));

        // 1. 상단 영역
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel status = new JLabel("손에 든 단서");
        status.setForeground(Color.WHITE);

        JButton closeButton = new JButton("접어두기  X");
        closeButton.setForeground(Color.WHITE);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);

        closeButton.addActionListener(e -> closeClue());

        top.add(status, BorderLayout.WEST);
        top.add(closeButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // 2. 종이 영역
        JPanel paper = new JPanel();
        paper.setLayout(new BoxLayout(paper, BoxLayout.Y_AXIS));
        paper.setBackground(PAPER);
        paper.setBorder(new EmptyBorder(20, 20, 20, 20));

        numberLabel.setForeground(TEXT);
        numberLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        numberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        setupTextArea(descriptionArea, PAPER, 13);
        setupTextArea(codeArea, CODE_BG, 12);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setLineWrap(false);

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setPreferredSize(new Dimension(290, 165));
        codeScroll.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 165));
        codeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        codeScroll.setBorder(
                BorderFactory.createLineBorder(GOLD, 1));

        descriptionArea.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 70));

        answerLabel.setForeground(TEXT);
        answerLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        answerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        paper.add(numberLabel);
        paper.add(Box.createVerticalStrut(15));
        paper.add(new JSeparator());
        paper.add(Box.createVerticalStrut(20));
        paper.add(titleLabel);
        paper.add(Box.createVerticalStrut(15));
        paper.add(descriptionArea);
        paper.add(Box.createVerticalStrut(15));
        paper.add(codeScroll);
        paper.add(Box.createVerticalStrut(20));
        paper.add(answerLabel);

        // 3. 힌트 영역
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setBackground(DARK);

        hintButton.setBackground(DARK);
        hintButton.setForeground(PAPER);
        hintButton.setFocusPainted(false);
        hintButton.setHorizontalAlignment(SwingConstants.LEFT);
        hintButton.setBorder(new EmptyBorder(12, 12, 12, 12));

        setupTextArea(hintArea, DARK, 12);
        hintArea.setForeground(PAPER);

        hintContent.setBackground(DARK);
        hintContent.setBorder(new EmptyBorder(5, 12, 12, 12));
        hintContent.add(hintArea, BorderLayout.CENTER);
        hintContent.setVisible(false);

        hintButton.addActionListener(e -> {
            boolean open = !hintContent.isVisible();
            hintContent.setVisible(open);
            hintButton.setText(
                    open ? "♧  작은 힌트   -"
                         : "♧  작은 힌트   +");
            hintPanel.revalidate();
            hintPanel.repaint();
        });

        hintPanel.add(hintButton, BorderLayout.NORTH);
        hintPanel.add(hintContent, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(paper, BorderLayout.CENTER);
        body.add(hintPanel, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);

        // 초기 상태: 숨김
        setVisible(false);

        // [추후 연동]
        // GamePage에서 선택된 ItemView 데이터를 전달받는다.
        // 단서 조회, 획득 여부 판단은 이 클래스에서 하지 않는다.
    }

    private void setupTextArea(
            JTextArea area, Color bg, int fontSize) {

        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(bg);
        area.setForeground(TEXT);
        area.setFont(new Font("Dialog", Font.PLAIN, fontSize));
        area.setBorder(new EmptyBorder(5, 5, 5, 5));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /**
     * 외부에서 단서 표시 데이터 전달
     */
    public void setClueData(ItemView item) {
    numberLabel.setText(item.id().toString());
    titleLabel.setText(item.title());
    codeArea.setText(item.content());

    // ItemView에 없는 데이터는 표시하지 않음
    descriptionArea.setVisible(false);
    answerLabel.setVisible(false);
    hintButton.setVisible(false);
    hintContent.setVisible(false);

    codeArea.setCaretPosition(0);

    revalidate();
    repaint();
}

    // 단서 펼치기
    public void openClue() {
        setVisible(true);
        revalidate();
        repaint();
    }

    // 단서 접기
    public void closeClue() {
        setVisible(false);

        if (onClose != null) {
            onClose.run();
        }
    }

    // 외부에서 접기 이벤트 연결
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
}