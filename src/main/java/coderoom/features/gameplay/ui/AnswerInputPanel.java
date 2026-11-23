package coderoom.features.gameplay.ui;

import coderoom.shared.audio.GameAudio;
import coderoom.features.gameplay.PuzzleVariant;
import coderoom.entities.game.WorkshopRoom;
import coderoom.entities.puzzle.AnswerType;
import coderoom.shared.ui.PixelTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
public final class AnswerInputPanel extends JPanel {
    public enum InputMode { DIGITS4, GRID3X3, DRAG, DEBUG, WORKSHOP }

    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel guide   = new JLabel(" ", SwingConstants.CENTER);
    private final JButton submit = PixelTheme.button("확인");
    private final JPanel object = new JPanel(new BorderLayout());
    private InputMode mode = InputMode.DIGITS4;
    private StudyLockPanel drawer;
    private StudyLockPanel wall;
    private JTextField debugField;
    private DragPuzzlePanel drag;
    private WorkshopPanel workshop;
    private final java.util.Map<WorkshopRoom.Task, WorkshopPanel> workshopStates = new java.util.HashMap<>();
    private final JButton reset = headerButton("↻", "다시 놓기");
    private final JButton close = headerButton("×", "닫기");
    private Runnable onClose;
    private Consumer<String> onSubmit;

    public AnswerInputPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        close.addActionListener(event -> {
            setVisible(false);
            if (onClose != null) { onClose.run(); }
        });
        reset.addActionListener(event -> {
            switch (mode) {
                case DIGITS4 -> drawer.reset();
                case GRID3X3 -> wall.reset();
                case DRAG -> drag.reset();
                case DEBUG -> debugField.setText("");
                case WORKSHOP -> workshop.reset();
            }
            showMessage(" ");
        });
        header.add(reset, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        object.setOpaque(false);
        add(object, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        guide.setFont(PixelTheme.font(13));
        guide.setForeground(PixelTheme.LIGHT);
        message.setFont(PixelTheme.font(13));
        message.setForeground(PixelTheme.LIGHT);
        guide.setVisible(false);
        message.setPreferredSize(new Dimension(1, 24));
        footer.add(guide, BorderLayout.NORTH);
        footer.add(message, BorderLayout.CENTER);
        footer.add(submit, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        submit.addActionListener(event -> submitAnswer());
        showFourDigits();
    }

    private static JButton headerButton(String icon, String label) {
        JButton button = new JButton(icon);
        button.setFont(PixelTheme.font(28));
        button.setForeground(PixelTheme.LIGHT);
        button.setPreferredSize(new Dimension(36, 36));
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        return button;
    }

    public void showPuzzle(AnswerType type) {
        switch (type) {
            case DIGITS4 -> showFourDigits();
            case GRID3X3 -> showGrid3x3();
            case VALVE -> showDrag("ABCD", 1);
            case LEVER_ORDER -> showDrag("1234", 4);
            case CARD_ORDER -> showDrag("1234", 4);
            case DEBUG_LINES -> showDebug();
            case WORKSHOP -> throw new IllegalArgumentException("방과 장치가 필요합니다.");
        }
    }

    public void showWorkshop(WorkshopRoom room, int index, PuzzleVariant variant, int platformDirection) {
        if (workshop != null) { workshop.cancel(); }
        mode     = InputMode.WORKSHOP;
        workshop = workshopStates.computeIfAbsent(room.task(index), key -> {
            WorkshopPanel device = new WorkshopPanel(room, index, variant, platformDirection);
            device.onInteractionChanged(() -> showMessage(" "));
            device.onStart(() -> {
                if (isVisible() && workshop == device) { submitAnswer(); }
            });
            device.onFailedRoute(route -> {
                if (isVisible() && workshop == device && onSubmit != null) { onSubmit.accept(route); }
            });
            return device;
        });
        object.removeAll();
        object.add(workshop);
        guide.setText(" ");
        refresh();
    }

    private void showDrag(String labels, int targets) {
        mode = InputMode.DRAG;
        object.removeAll();
        drag = new DragPuzzlePanel(labels, targets);
        object.add(drag);
        guide.setText("사물을 끌어 빈 자리에 놓으세요. 키보드는 방향키와 스페이스를 사용합니다.");
        submit.setText("배치 확인");
        refresh();
    }

    private void showDebug() {
        mode = InputMode.DEBUG;
        object.removeAll();
        debugField = new JTextField(4);
        configure(debugField, 4, "오류가 있는 줄 번호 두 개");
        debugField.setOpaque(true);
        debugField.setBackground(PixelTheme.PAPER);
        debugField.setHorizontalAlignment(SwingConstants.CENTER);
        object.add(debugField);
        guide.setText("잘못된 두 줄을 앞에서부터 두 자리씩 입력하세요. 예시 0310");
        submit.setText("오류 줄 확인");
        refresh();
    }

    public void showFourDigits() {
        if (drawer == null) { drawer = new StudyLockPanel(true, this::submitAnswer); }
        showLock(InputMode.DIGITS4, drawer);
    }

    public void showGrid3x3() {
        if (wall == null) { wall = new StudyLockPanel(false, this::submitAnswer); }
        showLock(InputMode.GRID3X3, wall);
    }

    private void showLock(InputMode lockMode, StudyLockPanel lock) {
        mode = lockMode;
        object.removeAll();
        object.add(lock);
        setPreferredSize(new Dimension(620, 570));
        refresh();
    }

    /** 현재 풀이 화면에서 확대 장치나 입력 영역이 그려지는 위쪽 위치. */
    public int puzzleContentTop() {
        doLayout();
        object.doLayout();
        int offset = switch (mode) {
            case DIGITS4 -> drawer.getY() + drawer.imageBounds().y;
            case GRID3X3 -> wall.getY() + wall.imageBounds().y;
            case WORKSHOP -> workshop.getY() + workshop.artworkTop();
            case DRAG -> drag.getY();
            case DEBUG -> debugField.getY();
        };
        return object.getY() + offset;
    }

    public boolean rattleLock() {
        if (mode == InputMode.WORKSHOP) {
            workshop.resist();
            return true;
        }
        StudyLockPanel lock = switch (mode) {
            case DIGITS4 -> drawer;
            case GRID3X3 -> wall;
            default -> null;
        };
        if (lock == null) { return false; }
        lock.reject();
        return true;
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
        if (mode == InputMode.WORKSHOP) {
            String value = workshop.answer();
            if (value.isEmpty()) {
                GameAudio.play(GameAudio.Cue.LOCKED);
                workshop.resist();
            }
            else if (onSubmit != null) { onSubmit.accept(value); }
            return;
        }
        if (mode == InputMode.DRAG || mode == InputMode.DEBUG) {
            String value = mode == InputMode.DRAG ? drag.answer() : debugField.getText();
            if (value.isEmpty() || mode == InputMode.DEBUG && value.length() != 4) {
                showMessage("모든 입력을 완료하세요.");
            } else if (onSubmit != null) {
                onSubmit.accept(value);
            }
            return;
        }
        boolean drawerLock = mode == InputMode.DIGITS4;
        String value       = (drawerLock ? drawer : wall).answer();
        if (value.length() != (drawerLock ? 4 : 9)) {
            showMessage(drawerLock ? "숫자 네 자리를 맞춰 주세요." : "아홉 다이얼을 모두 맞춰 주세요.");
            return;
        }
        if (onSubmit != null) { onSubmit.accept(value); }
    }

    private void refresh() {
        boolean physical = mode == InputMode.WORKSHOP || mode == InputMode.DIGITS4 || mode == InputMode.GRID3X3;
        guide.setVisible(!physical);
        submit.setVisible(!physical);
        message.setVisible(mode != InputMode.WORKSHOP);
        message.setText(" ");
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> {
            focusInput();
        });
    }

    @Override public void setVisible(boolean visible) {
        if (!visible && workshop != null) { workshop.cancel(); }
        super.setVisible(visible);
    }

    public void showMessage(String value) {
        message.setText(value);
        getAccessibleContext().setAccessibleDescription(value);
    }
    public void focusInput() {
        java.awt.Component first = switch (mode) {
            case DIGITS4 -> drawer.firstDigit();
            case GRID3X3 -> wall.firstDigit();
            case DRAG -> drag;
            case DEBUG -> debugField;
            case WORKSHOP -> workshop;
        };
        if (isShowing() && first.isEnabled()) {
            first.requestFocusInWindow();
            if (first instanceof JTextField field) { field.selectAll(); }
        }
    }
    public void setOnClose(Runnable callback) { onClose = callback; }
    public void setOnSubmit(Consumer<String> callback) { onSubmit = callback; }

}
