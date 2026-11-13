package coderoom.features.gameplay.ui;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * 담당: F
 * 역할: 서랍 4자리 암호 / 벽 3x3 다이얼 입력 UI
 *
 * 현재는 UI만 구현한다.
 * 실제 정답 판정 및 데이터 연동은 추후 구현한다.
 */
public class AnswerInputPanel extends JPanel {

    public enum InputMode {
        DIGITS4,
        GRID3X3
    }

    // 프로토타입 색상
    private static final Color BG = new Color(40, 59, 45);
    private static final Color INPUT_BG = new Color(24, 39, 29);
    private static final Color GOLD = new Color(220, 196, 142);
    private static final Color TEXT = new Color(235, 231, 207);
    private static final Color LINE = new Color(123, 139, 105);

    private final JLabel titleLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel guideLabel = new JLabel();
    private final JLabel messageLabel = new JLabel(" ");

    private final JPanel inputContainer =
            new JPanel(new BorderLayout());

    private final JButton submitButton = new JButton();
    private final JButton closeButton = new JButton("×");

    private JTextField fourDigitField;
    private JTextField[] gridFields;

    private InputMode mode = InputMode.DIGITS4;

    private Runnable onClose;
    private Consumer<String> onSubmit;

    public AnswerInputPanel() {

        setLayout(new BorderLayout(0, 15));
        setBackground(BG);

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 4),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(7, 7, 7, 7),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(LINE, 1),
                                new EmptyBorder(25, 28, 25, 28)
                        )
                )
        ));

        setPreferredSize(new Dimension(440, 540));

        // 상단: 제목 + 닫기
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        titleLabel.setFont(
                new Font("Dialog", Font.BOLD, 23)
        );
        titleLabel.setForeground(TEXT);

        closeButton.setFont(
                new Font("Dialog", Font.PLAIN, 28)
        );
        closeButton.setForeground(TEXT);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        closeButton.addActionListener(e -> {
            setVisible(false);

            if (onClose != null) {
                onClose.run();
            }
        });

        header.add(titleLabel, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // 중앙: 설명 + 입력칸
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(15, 0, 0, 0));

        descriptionLabel.setFont(
                new Font("Dialog", Font.PLAIN, 13)
        );
        descriptionLabel.setForeground(TEXT);
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        guideLabel.setFont(
                new Font("Dialog", Font.PLAIN, 13)
        );
        guideLabel.setForeground(TEXT);
        guideLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        inputContainer.setOpaque(false);
        inputContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputContainer.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 235)
        );

        messageLabel.setFont(
                new Font("Dialog", Font.PLAIN, 12)
        );
        messageLabel.setForeground(GOLD);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(descriptionLabel);
        center.add(Box.createVerticalStrut(30));
        center.add(guideLabel);
        center.add(Box.createVerticalStrut(25));
        center.add(inputContainer);
        center.add(Box.createVerticalStrut(10));
        center.add(messageLabel);

        add(center, BorderLayout.CENTER);

        // 하단: 제출 버튼
        submitButton.setBackground(GOLD);
        submitButton.setForeground(BG);
        submitButton.setFont(
                new Font("Dialog", Font.BOLD, 16)
        );
        submitButton.setFocusPainted(false);
        submitButton.setOpaque(true);
        submitButton.setBorderPainted(false);
        submitButton.setPreferredSize(
                new Dimension(340, 55)
        );
        submitButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        submitButton.addActionListener(e -> submitAnswer());

        add(submitButton, BorderLayout.SOUTH);

        // 초기 화면
        showFourDigits();

        // [추후 연동 - A/E 담당]
        // GamePage가 AnswerType을 전달
        // GameFacade에서 정답을 판정
        // SubmitResult의 결과 메시지만 UI에 반영
    }

    /**
     * 4자리 암호 화면
     */
    public void showFourDigits() {

        mode = InputMode.DIGITS4;
        inputContainer.removeAll();

        titleLabel.setText("잠긴 서랍");
        descriptionLabel.setText(
                "네 자리 숫자 잠금 장치가 있다."
        );
        guideLabel.setText("네 자리 암호");
        submitButton.setText("서랍 열기  →");
        messageLabel.setText(" ");

        // 빈 입력칸에 점(.)이 표시되는 입력 필드
        fourDigitField = createInputField();

        fourDigitField.setHorizontalAlignment(
                JTextField.CENTER
        );
        fourDigitField.setFont(
                new Font(Font.MONOSPACED, Font.BOLD, 33)
        );
        fourDigitField.setForeground(GOLD);
        fourDigitField.setBackground(INPUT_BG);
        fourDigitField.setCaretColor(GOLD);
        fourDigitField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD, 2),
                        new EmptyBorder(10, 12, 10, 12)
                )
        );

        // 숫자만 입력 가능, 최대 4자리
        setDigitLimit(fourDigitField, 4);

        fourDigitField.addActionListener(e -> submitAnswer());

        JPanel inputBox = new JPanel(new BorderLayout());
        inputBox.setOpaque(false);
        inputBox.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 90)
        );
        inputBox.setPreferredSize(new Dimension(350, 90));
        inputBox.add(fourDigitField, BorderLayout.CENTER);

        inputContainer.add(inputBox, BorderLayout.NORTH);

        // 긴 입력창을 표시하는 4자리 모드
        inputContainer.setPreferredSize(
                new Dimension(350, 100)
        );

        refreshLayout();

        SwingUtilities.invokeLater(
                fourDigitField::requestFocusInWindow
        );
    }

    /**
     * 3x3 다이얼 화면
     */
    public void showGrid3x3() {

        mode = InputMode.GRID3X3;
        inputContainer.removeAll();

        titleLabel.setText("벽의 3 × 3 다이얼");
        descriptionLabel.setText(
                "세 줄의 숫자가 작은 금고의 암호인 것 같다."
        );
        guideLabel.setText(
                "메모의 출력값을 행 순서대로 입력하세요."
        );
        submitButton.setText("다이얼 확인  →");
        messageLabel.setText(" ");

        JPanel grid = new JPanel(
                new GridLayout(3, 3, 9, 9)
        );
        grid.setOpaque(false);

        gridFields = new JTextField[9];

        for (int i = 0; i < 9; i++) {

            final int index = i;

            // 빈칸에 점(.) 표시
            JTextField field = createInputField();

            field.setHorizontalAlignment(
                    JTextField.CENTER
            );
            field.setFont(
                    new Font(Font.MONOSPACED, Font.BOLD, 27)
            );
            field.setForeground(GOLD);
            field.setBackground(INPUT_BG);
            field.setCaretColor(GOLD);

            field.setBorder(
                    BorderFactory.createLineBorder(LINE, 2)
            );

            // 각 칸에는 숫자 1개만 허용
            setDigitLimit(field, 1);

            gridFields[i] = field;
            grid.add(field);

            // 숫자를 입력하면 다음 칸으로 이동
            field.getDocument().addDocumentListener(
                    new javax.swing.event.DocumentListener() {

                        @Override
                        public void insertUpdate(
                                javax.swing.event.DocumentEvent e) {
                            moveNext();
                        }

                        @Override
                        public void removeUpdate(
                                javax.swing.event.DocumentEvent e) {
                        }

                        @Override
                        public void changedUpdate(
                                javax.swing.event.DocumentEvent e) {
                        }

                        private void moveNext() {
                            if (index < 8) {
                                SwingUtilities.invokeLater(() -> {
                                    if (field.getText().length() == 1) {
                                        gridFields[index + 1]
                                                .requestFocusInWindow();
                                    }
                                });
                            }
                        }
                    }
            );

            // Backspace를 누르면 이전 칸으로 이동
            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {

                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
                            && field.getText().isEmpty()
                            && index > 0) {

                        gridFields[index - 1]
                                .requestFocusInWindow();
                    }
                }
            });

            // Enter로 제출
            field.addActionListener(e -> submitAnswer());
        }

        inputContainer.add(grid, BorderLayout.CENTER);
        inputContainer.setPreferredSize(
                new Dimension(350, 235)
        );

        refreshLayout();

        SwingUtilities.invokeLater(
                () -> gridFields[0].requestFocusInWindow()
        );
    }

    /**
     * 실제 입력 데이터 수집
     * 정답 판정은 하지 않음.
     */
    private void submitAnswer() {

        String answer;

        if (mode == InputMode.DIGITS4) {

            answer = fourDigitField.getText();

            if (answer.length() != 4) {
                showMessage("숫자 4자리를 입력해 주세요.");
                return;
            }

        } else {

            StringBuilder builder = new StringBuilder();

            for (JTextField field : gridFields) {

                if (field.getText().length() != 1) {
                    showMessage("9개 칸을 모두 입력해 주세요.");
                    return;
                }

                builder.append(field.getText());
            }

            // 왼쪽 위부터 행 순서로 이어 붙임
            answer = builder.toString();
        }

        // UI 테스트용 안내 메시지
        showMessage("입력 완료: " + answer);

        // [추후 연동 - A/E 담당]
        // onSubmit.accept(answer);
        //
        // GamePage.submit(objectId, answer);
        // 위 호출은 A 담당자가 연결
        // 정답 비교는 GameFacade에서 처리
    }

    /**
     * 입력값을 숫자로 제한
     */
    private void setDigitLimit(
            JTextField field, int maxLength) {

        AbstractDocument document =
                (AbstractDocument) field.getDocument();

        document.setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(
                    FilterBypass fb,
                    int offset,
                    String string,
                    AttributeSet attr)
                    throws BadLocationException {

                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(
                    FilterBypass fb,
                    int offset,
                    int length,
                    String text,
                    AttributeSet attrs)
                    throws BadLocationException {

                if (text == null) {
                    return;
                }

                String current = fb.getDocument().getText(
                        0, fb.getDocument().getLength()
                );

                String result =
                        current.substring(0, offset)
                        + text
                        + current.substring(offset + length);

                if (result.matches("[0-9]*")
                        && result.length() <= maxLength) {

                    super.replace(
                            fb, offset, length, text, attrs
                    );
                }
            }
        });
    }

    /**
     * 빈 입력칸에 점(.) 표시
     * 실제 입력값에는 포함되지 않음.
     */
    private JTextField createInputField() {

        return new JTextField() {

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                if (!getText().isEmpty()) {
                    return;
                }

                Graphics placeholder = g.create();

                try {
                    placeholder.setFont(getFont());
                    placeholder.setColor(GOLD);

                    FontMetrics metrics =
                            placeholder.getFontMetrics();

                    int x =
                            (getWidth() - metrics.stringWidth(".")) / 2;

                    int y =
                            (getHeight() - metrics.getHeight()) / 2
                            + metrics.getAscent();

                    placeholder.drawString(".", x, y);

                } finally {
                    placeholder.dispose();
                }
            }
        };
    }

    /**
     * 안내 메시지 표시
     */
    public void showMessage(String message) {
        messageLabel.setText(message);
    }

    /**
     * 닫기 콜백 등록
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * 추후 GamePage와 연결할 제출 콜백
     */
    public void setOnSubmit(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit;
    }

    /**
     * 화면 갱신
     */
    private void refreshLayout() {
        inputContainer.revalidate();
        inputContainer.repaint();
        revalidate();
        repaint();
    }
}
