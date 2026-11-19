package coderoom.features.gameplay.ui;

import coderoom.shared.ui.PixelTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * 담당: F
 * 역할: 서랍 4자리 암호와 벽 3×3 다이얼의 확대 입력 UI를 표시한다.
 * 제출 콜백은 A의 화면 연결을 통해 E의 정답 판정으로 전달하고 결과 메시지를 표시한다.
 */
public class AnswerInputPanel extends JPanel {
    public enum InputMode { DIGITS4, GRID3X3 }

    private final JLabel title   = new JLabel();
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel guide   = new JLabel(" ", SwingConstants.CENTER);
    private final JButton submit = PixelTheme.button("서랍 열기  →");
    private final ObjectCanvas object = new ObjectCanvas();
    private InputMode mode = InputMode.DIGITS4;
    private JTextField fourDigitField;
    private JTextField[] gridFields;
    private Runnable onClose;
    private Consumer<String> onSubmit;

    public AnswerInputPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        title.setFont(PixelTheme.font(24));
        title.setForeground(PixelTheme.LIGHT);
        JButton close = PixelTheme.button("닫기 X");
        close.addActionListener(event -> {
            setVisible(false);
            if (onClose != null) { onClose.run(); }
        });
        title.setVisible(false);
        header.add(close, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(object, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        guide.setFont(PixelTheme.font(13));
        guide.setForeground(PixelTheme.LIGHT);
        message.setFont(PixelTheme.font(13));
        message.setForeground(PixelTheme.LIGHT);
        guide.setVisible(false);
        message.setPreferredSize(new Dimension(1, 24));
        footer.add(message, BorderLayout.CENTER);
        footer.add(submit, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        submit.addActionListener(event -> submitAnswer());
        showFourDigits();
    }

    public void showFourDigits() {
        String previous = fourDigitField == null ? "0000" : fourDigitField.getText();
        mode = InputMode.DIGITS4;
        object.removeAll();
        title.setText("서랍의 잠금장치");
        guide.setText("숫자를 입력하거나 각 자리의 바퀴를 누르세요.");
        submit.setText("서랍 열기  →");
        fourDigitField = new JTextField() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                PixelTheme.pixels(g);
                int cell = getWidth() / 4;
                g.setFont(PixelTheme.font(Math.max(16, getHeight() * .58f)));
                g.setColor(PixelTheme.LIGHT);
                for (int i = 0; i < 4; i++) {
                    String digit = getText().length() > i ? getText().substring(i, i + 1) : "0";
                    g.drawString(digit, i * cell + (cell - g.getFontMetrics().stringWidth(digit)) / 2,
                            (getHeight() + g.getFontMetrics().getAscent()) / 2 - 3);
                }
                g.dispose();
            }
        };
        configure(fourDigitField, 4, "서랍 네 자리 암호");
        fourDigitField.setText(previous);
        fourDigitField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                int index = Math.min(3, event.getX() * 4 / Math.max(1, fourDigitField.getWidth()));
                String padded = (fourDigitField.getText() + "0000").substring(0, 4);
                char[] digits = padded.toCharArray();
                int delta = SwingUtilities.isRightMouseButton(event) ? 9 : 1;
                digits[index] = (char) ('0' + (digits[index] - '0' + delta) % 10);
                fourDigitField.setText(new String(digits));
                fourDigitField.select(index, index + 1);
            }
        });
        object.add(fourDigitField);
        setPreferredSize(new Dimension(620, 570));
        refresh();
    }

    public void showGrid3x3() {
        JTextField[] previous = gridFields;
        mode = InputMode.GRID3X3;
        object.removeAll();
        title.setText("벽의 아홉 다이얼");
        guide.setText("눌러서 돌리기 · 오른쪽 클릭은 반대 방향 · 숫자 입력 가능");
        submit.setText("다이얼 확인  →");
        gridFields = new JTextField[9];
        for (int i = 0; i < gridFields.length; i++) {
            JTextField field = new JTextField() {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    int size = Math.min(getWidth(), getHeight());
                    g.setFont(PixelTheme.font(Math.max(16, size * .40f)));
                    g.setColor(new Color(44, 27, 11));
                    String digit = getText().isEmpty() ? "0" : getText();
                    g.drawString(digit, (getWidth() - g.getFontMetrics().stringWidth(digit)) / 2,
                            (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
                    g.dispose();
                }
            };
            configure(field, 1, "다이얼 " + (i / 3 + 1) + "행 " + (i % 3 + 1) + "열");
            field.setText(previous == null ? "0" : previous[i].getText());
            field.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    int value = field.getText().isEmpty() ? 0 : Integer.parseInt(field.getText());
                    int delta = SwingUtilities.isRightMouseButton(event) ? 9 : 1;
                    field.setText(Integer.toString((value + delta) % 10));
                    field.selectAll();
                }
            });
            gridFields[i] = field;
            object.add(field);
        }
        setPreferredSize(new Dimension(620, 570));
        refresh();
    }

    private void configure(JTextField field, int limit, String name) {
        field.setOpaque(false);
        field.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setFont(PixelTheme.font(24));
        field.getAccessibleContext().setAccessibleName(name);
        field.setToolTipText(null);
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributes)
                    throws BadLocationException { replace(fb, offset, 0, text, attributes); }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributes)
                    throws BadLocationException {
                String old   = fb.getDocument().getText(0, fb.getDocument().getLength());
                String value = old.substring(0, offset) + (text == null ? "" : text) + old.substring(offset + length);
                if (value.matches("[0-9]{0," + limit + "}")) { super.replace(fb, offset, length, text, attributes); }
            }
        });
        field.addActionListener(event -> submitAnswer());
    }

    private void submitAnswer() {
        StringBuilder value = new StringBuilder();
        if (mode == InputMode.DIGITS4) { value.append(fourDigitField.getText()); }
        else { for (JTextField field : gridFields) { value.append(field.getText()); } }
        if (value.length() != (mode == InputMode.DIGITS4 ? 4 : 9)) {
            showMessage(mode == InputMode.DIGITS4 ? "숫자 네 자리를 맞춰 주세요." : "아홉 다이얼을 모두 맞춰 주세요.");
            return;
        }
        if (onSubmit != null) { onSubmit.accept(value.toString()); }
    }

    private void refresh() {
        message.setText(" ");
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> {
            if (isShowing()) {
                JTextField first = mode == InputMode.DIGITS4 ? fourDigitField : gridFields[0];
                first.requestFocusInWindow();
                first.selectAll();
            }
        });
    }

    public void showMessage(String value) { message.setText(value); }
    public void setOnClose(Runnable callback) { onClose = callback; }
    public void setOnSubmit(Consumer<String> callback) { onSubmit = callback; }

    private final class ObjectCanvas extends JPanel {
        private ObjectCanvas() { setLayout(null); setOpaque(false); }
        private Rectangle objectBounds() {
            double ratio = mode == InputMode.DIGITS4 ? 3 : 1;
            int width    = Math.min(getWidth(), (int) (getHeight() * ratio));
            int height   = (int) (width / ratio);
            return new Rectangle((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Rectangle b = objectBounds();
            if (mode == InputMode.DIGITS4) {
                PixelTheme.asset(g, "drawer", b.x, b.y, b.width, b.height);
            } else {
                PixelTheme.asset(g, "dial", b.x, b.y, b.width, b.height);
            }
            g.dispose();
        }
        @Override public void doLayout() {
            Rectangle b = objectBounds();
            if (mode == InputMode.DIGITS4) {
                fourDigitField.setBounds(b.x + b.width * 40 / 100, b.y + b.height * 45 / 100,
                        b.width * 21 / 100, b.height * 14 / 100);
            } else {
                for (int i = 0; i < 9; i++) {
                    gridFields[i].setBounds(b.x + b.width * (16 + i % 3 * 25) / 100,
                            b.y + b.height * (16 + i / 3 * 25) / 100,
                            b.width * 18 / 100, b.height * 18 / 100);
                }
            }
        }
    }
}
