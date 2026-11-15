package coderoom.widgets.clue;
import coderoom.features.gameplay.ItemView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
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
        JPanel paper = new PaperPanel();
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
        setupTextArea(codeArea, PAPER, 12);
        descriptionArea.setOpaque(false);
        codeArea.setOpaque(false);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setLineWrap(false);

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setOpaque(false);
        codeScroll.getViewport().setOpaque(false);
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

    /** 이미지 파일 없이 고정된 섬유 무늬와 가장자리 음영을 그린다. */
    private static class PaperPanel extends JPanel {
        private static final int TILE_SIZE = 128;
        private static final BufferedImage TEXTURE = createTexture();

        private static BufferedImage createTexture() {
            BufferedImage texture = new BufferedImage(
                    TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = texture.createGraphics();
            try {
                // 고정 시드: 다시 그릴 때 무늬가 바뀌거나 깜박이지 않는다.
                Random random = new Random(42L);
                for (int i = 0; i < 1800; i++) {
                    int x = random.nextInt(TILE_SIZE);
                    int y = random.nextInt(TILE_SIZE);
                    graphics.setColor(random.nextBoolean()
                            ? new Color(112, 88, 48, 16)
                            : new Color(255, 255, 240, 35));
                    graphics.fillRect(x, y, 1, 1);
                }
                graphics.setColor(new Color(120, 95, 55, 12));
                for (int i = 0; i < 100; i++) {
                    int x = random.nextInt(TILE_SIZE);
                    int y = random.nextInt(TILE_SIZE);
                    graphics.drawLine(x, y, x + 2 + random.nextInt(4), y + 1);
                }
            } finally {
                graphics.dispose();
            }
            return texture;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            Graphics2D graphics = (Graphics2D) g.create();
            try {
                graphics.setPaint(new GradientPaint(
                        0, 0, new Color(245, 238, 211),
                        width, height, PAPER));
                graphics.fillRect(0, 0, width, height);
                graphics.setPaint(new TexturePaint(
                        TEXTURE, new Rectangle(0, 0, TILE_SIZE, TILE_SIZE)));
                graphics.fillRect(0, 0, width, height);

                // 옅은 음영을 겹쳐 가장자리가 조금 낡은 종이처럼 보이게 한다.
                int edge = Math.min(12, Math.min(width, height) / 2);
                for (int i = 0; i < edge; i++) {
                    graphics.setColor(new Color(116, 88, 44, 18 - i));
                    graphics.drawRect(i, i, width - 1 - i * 2, height - 1 - i * 2);
                }
            } finally {
                graphics.dispose();
            }
        }
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

    // 코드와 힌트만 표시하며 정답은 받지 않는다.
    descriptionArea.setVisible(false);
    answerLabel.setVisible(false);
    hintArea.setText(item.hint());
    hintButton.setVisible(!item.hint().isBlank());
    hintButton.setText("♧  작은 힌트   +");
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
