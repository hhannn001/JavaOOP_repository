package coderoom.widgets.result;

import java.awt.BorderLayout;
import java.awt.AlphaComposite;
import coderoom.entities.game.GameSession;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import coderoom.shared.ui.PixelTheme;
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

    private final JLabel title = new JLabel("탈출 성공!", SwingConstants.CENTER) {
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            PixelTheme.pixels(g);
            int half    = titles.getHeight() / 2;
            int source  = failed ? half : 0;
            double fit  = Math.min(getWidth() / (double) titles.getWidth(), getHeight() / (double) half);
            int width   = (int) (titles.getWidth() * fit);
            int height  = (int) (half * fit);
            int x       = (getWidth() - width) / 2;
            int y       = (getHeight() - height) / 2;
            g.drawImage(titles, x, y, x + width, y + height, 0, source, titles.getWidth(), source + half, null);
            g.dispose();
        }
    };
    private final BufferedImage titles = loadTitles();
    private boolean failed;
    private int earnedStars;
    private final JPanel stars = new JPanel() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            int size     = 64;
            int gap      = 18;
            int start    = (getWidth() - 3 * size - 2 * gap) / 2;
            for (int index = 0; index < 3; index++) {
                g.setComposite(AlphaComposite.SrcOver.derive(index < earnedStars ? 1f : 0.16f));
                PixelTheme.asset(g, "result-star", start + index * (size + gap),
                        index == 1 ? 0 : 8, size, size);
            }
            g.dispose();
        }
    };
    private final JLabel time     = new JLabel("", SwingConstants.CENTER);
    private final JLabel status   = new JLabel("", SwingConstants.CENTER);
    private final JButton retry   = PixelTheme.button("기록 저장 재시도");
    private final JButton restart = PixelTheme.button("다시 시작");
    private final JButton back    = PixelTheme.button("맵 목록으로");
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
        setBackground(PixelTheme.DARK);
        setPreferredSize(new Dimension(620, 400));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(128, 94, 48), 3),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        JPanel messages = new JPanel(new BorderLayout(0, 12));
        JPanel details  = new JPanel(new GridLayout(2, 1, 0, 10));
        details.setOpaque(false);
        messages.setOpaque(false);
        title.setPreferredSize(new Dimension(540, 100));
        stars.setOpaque(false);
        stars.setPreferredSize(new Dimension(540, 80));
        for (JLabel label : new JLabel[] {title, time, status}) {
            label.setForeground(PixelTheme.LIGHT);
            label.setFont(PixelTheme.font(16));

        }
        JPanel achievement = new JPanel(new BorderLayout());
        achievement.setOpaque(false);
        achievement.add(title, BorderLayout.NORTH);
        achievement.add(stars, BorderLayout.CENTER);
        messages.add(achievement, BorderLayout.NORTH);
        details.add(time);
        details.add(status);
        messages.add(details, BorderLayout.CENTER);
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
        failed = false;
        updateStars(elapsedSeconds, true);
        title.setText("탈출 성공!");
        saveState = Objects.requireNonNull(state, "state");
        hasResult = true;
        time.setText(String.format(Locale.ROOT, "탈출 시간: %02d:%02d",
                elapsedSeconds / 60, elapsedSeconds % 60));
        status.setText(switch (state) {
            case NONE -> "기록 저장 대기 중";
            case SAVING -> "기록 저장 중…";
            case SAVED -> "기록 저장 완료";
            case FAILED -> "기록을 저장하지 못했습니다. 저장을 다시 시도해 주세요.";
        });
        updateButtons();
        setVisible(true);
    }

    public void renderFailure() {
        renderFailure(String.format(Locale.ROOT, "남은 시간 00:00 · 제한시간 %02d:%02d",
                GameSession.TIME_LIMIT_SECONDS / 60, GameSession.TIME_LIMIT_SECONDS % 60),
                "시간이 다 됐습니다. 다시 도전해 보세요.");
    }

    public void renderFailure(String detail, String reason) {
        requireEdt();
        updateStars(GameSession.TIME_LIMIT_SECONDS, false);
        failed    = true;
        hasResult = true;
        saveState = SaveState.NONE;
        title.setText("탈출 실패");
        time.setText(detail);
        status.setText(reason);
        updateButtons();
        setVisible(true);
        repaint();
    }

    /** 제한시간 변경 시에도 동일한 1/3 구간으로 성과를 평가한다. */
    public static int starCount(long elapsedSeconds, long limitSeconds, boolean escaped) {
        if (elapsedSeconds < 0 || limitSeconds <= 0) {
            throw new IllegalArgumentException("경과 시간은 0 이상, 제한시간은 양수여야 합니다.");
        }
        if (!escaped || elapsedSeconds >= limitSeconds) {
            return 0;
        }
        double fraction = elapsedSeconds / (double) limitSeconds;
        return fraction <= 1.0 / 3 ? 3 : fraction <= 2.0 / 3 ? 2 : 1;
    }

    private void updateStars(long elapsedSeconds, boolean escaped) {
        earnedStars = starCount(elapsedSeconds, GameSession.TIME_LIMIT_SECONDS, escaped);
        stars.getAccessibleContext().setAccessibleName("클리어 성과: 별 " + earnedStars + "개 / 3개");
        stars.setToolTipText("제한시간의 1/3 이내: 별 3개 · 2/3 이내: 별 2개 · 시간 내 탈출: 별 1개");
        stars.repaint();
    }

    private static BufferedImage loadTitles() {
        try (var input = ResultPanel.class.getResourceAsStream("/images/game-ui/result-titles.png")) {
            return ImageIO.read(input);
        } catch (Exception exception) {
            throw new IllegalStateException("결과 제목 이미지를 읽을 수 없습니다.", exception);
        }
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
        earnedStars = 0;
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
