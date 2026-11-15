package coderoom.widgets.result;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * 담당: G
 * 역할: 탈출 결과와 기록 저장 상태
 * 상위 타입: javax.swing.JPanel
 * GamePage의 위젯 어댑터가 확정 시간과 저장 상태를 전달한다. DB에는 접근하지 않는다.
 */
public final class ResultPanel extends JPanel {
    public enum SaveState { NONE, SAVING, SAVED, FAILED }

    private final JLabel title = new JLabel("탈출 성공!", SwingConstants.CENTER);
    private final JLabel time = new JLabel("", SwingConstants.CENTER);
    private final JLabel status = new JLabel("", SwingConstants.CENTER);
    private final JButton retry = new JButton("기록 저장 재시도");
    private final JButton restart = new JButton("다시 시작");
    private final JButton back = new JButton("맵 목록으로");
    private Runnable onRetrySave;
    private Runnable onRestart;
    private Runnable onBackToMaps;
    private SaveState saveState = SaveState.NONE;
    private boolean hasResult;
    private boolean busy;

    /** 생성·갱신·콜백 등록은 Swing EDT에서 수행한다. */
    public ResultPanel() {
        requireEdt();
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(30, 45, 36));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel messages = new JPanel(new GridLayout(3, 1, 0, 10));
        messages.setOpaque(false);
        title.setFont(new Font("Dialog", Font.BOLD, 24));
        for (JLabel label : new JLabel[] {title, time, status}) {
            label.setForeground(new Color(223, 200, 145));
            messages.add(label);
        }
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);
        actions.add(retry);
        actions.add(restart);
        actions.add(back);
        add(messages, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        retry.addActionListener(event -> {
            if (onRetrySave != null) {
                // 저장 결과가 돌아오기 전에는 중복 요청과 화면 이동을 막는다.
                saveState = SaveState.SAVING;
                status.setText("기록 저장 중…");
                updateButtons();
                onRetrySave.run();
            }
        });
        restart.addActionListener(event -> onRestart.run());
        back.addActionListener(event -> onBackToMaps.run());
        reset();
    }

    /** 저장 재시도에도 최초 탈출 시 확정된 동일한 초 값을 전달한다. */
    public void render(long elapsedSeconds, SaveState state) {
        requireEdt();
        if (elapsedSeconds < 0) {
            throw new IllegalArgumentException("탈출 시간은 0 이상이어야 합니다.");
        }
        saveState = Objects.requireNonNull(state, "state");
        hasResult = true;
        time.setText(String.format(Locale.ROOT, "탈출 시간: %02d:%02d",
                elapsedSeconds / 60, elapsedSeconds % 60));
        status.setText(switch (state) {
            case NONE -> "기록 저장 대기 중";
            case SAVING -> "기록 저장 중…";
            case SAVED -> "기록 저장 완료";
            case FAILED -> "탈출에 성공했습니다. 기록이 저장되지 않았습니다. 다시 시도해 주세요.";
        });
        updateButtons();
        setVisible(true);
    }

    public void setOnRetrySave(Runnable callback) {
        requireEdt();
        onRetrySave = Objects.requireNonNull(callback, "callback");
        updateButtons();
    }

    public void setOnRestart(Runnable callback) {
        requireEdt();
        onRestart = Objects.requireNonNull(callback, "callback");
        updateButtons();
    }

    public void setOnBackToMaps(Runnable callback) {
        requireEdt();
        onBackToMaps = Objects.requireNonNull(callback, "callback");
        updateButtons();
    }

    public void setBusy(boolean busy) {
        requireEdt();
        this.busy = busy;
        updateButtons();
    }

    /** 새 게임 시작 시 이전 결과를 지우고 패널을 숨긴다. */
    public void reset() {
        requireEdt();
        hasResult = false;
        busy = false;
        saveState = SaveState.NONE;
        time.setText("");
        status.setText("");
        updateButtons();
        setVisible(false);
    }

    private void updateButtons() {
        boolean available = hasResult && !busy && saveState != SaveState.SAVING;
        retry.setVisible(hasResult && saveState == SaveState.FAILED);
        retry.setEnabled(available && saveState == SaveState.FAILED && onRetrySave != null);
        restart.setEnabled(available && onRestart != null);
        back.setEnabled(available && onBackToMaps != null);
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("결과 패널은 EDT에서 조작해야 합니다.");
        }
    }
}
