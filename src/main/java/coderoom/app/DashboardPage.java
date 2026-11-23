package coderoom.app;

import coderoom.entities.user.LoginUser;
import coderoom.features.dashboard.DashboardRecords;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * 담당: A
 * 역할: 로그인 직후의 방탈출 카페 로비와 맵 선택 진입점을 표시한다.
 * 배경·창문·사물을 독립 레이어로 합성하고 기록 조회는 DashboardRecords에 위임한다.
 */
final class DashboardPage extends JPanel {
    private static final int SCENE_WIDTH      = 1585;
    private static final int SCENE_HEIGHT     = 992;
    private static final Color INK            = new Color(241, 217, 174);
    private static final BufferedImage SCENE  = SceneObjectButton.read("background-v2.png");
    private static final BufferedImage WINDOW = SceneObjectButton.read("window.png");
    private static final BufferedImage TITLE  = loadTitle();

    private static final BufferedImage CLEAN_COUNTER = SceneObjectButton.read("/images/dashboard/cat/counter-clean.png");
    private final JLabel account;
    private final JButton settings;
    private final SceneObjectButton books;
    private final SceneObjectButton curtain;
    private final SceneObjectButton calendar;
    private final SceneObjectButton leaderboard;
    private final DashboardBell bell;
    private final JButton catInput;
    private final Runnable logout;
    private final Runnable onIntroDismissed;
    private boolean introVisible;
    private Timer introAnimation;
    private float introProgress;
    private final DashboardCat cat = new DashboardCat();
    private long catLastFrame;
    private final Timer catTimer = new Timer(16, event -> advanceCat());
    private final WindowAmbient windowAmbient = new WindowAmbient();
    private final Timer windowTimer = new Timer(WindowAmbient.FRAME_MS, event -> {
        windowAmbient.advance();
        repaint();
    });

    DashboardPage(LoginUser user, Runnable openMaps, Runnable logout) {
        this(user, openMaps, logout, true, () -> {}, null);
    }

    DashboardPage(LoginUser user, Runnable openMaps, Runnable logout,
            boolean showIntro, Runnable onIntroDismissed) {
        this(user, openMaps, logout, showIntro, onIntroDismissed, null);
    }

    DashboardPage(LoginUser user, Runnable openMaps, Runnable logout,
            boolean showIntro, Runnable onIntroDismissed, DashboardRecords records) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(openMaps);
        this.logout           = Objects.requireNonNull(logout);
        this.onIntroDismissed = Objects.requireNonNull(onIntroDismissed);
        this.introVisible     = showIntro;
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    windowTimer.start();
                    catLastFrame = System.nanoTime();
                    catTimer.start();
                } else {
                    windowTimer.stop();
                    catTimer.stop();
                    cat.releaseKeys();
                }
            }
        });
        setLayout(null);
        setBackground(new Color(27, 23, 20));
        account = new JLabel(user.loginId());
        account.setFont(SceneObjectButton.PixelFont.at(20));
        account.setForeground(INK);
        settings    = settingsIconButton(this::showSettings);
        books       = new SceneObjectButton("풀이한 문제", "bookshelf", 1, () -> openRecords(records, user, "풀이한 문제"));
        curtain     = new SceneObjectButton("방 선택하기", "curtain", 6, openMaps);
        calendar    = new SceneObjectButton("진척도", "calendar", 6, () -> openRecords(records, user, "진척도"));
        leaderboard = new SceneObjectButton("리더보드", "leaderboard", 1, () -> openRecords(records, user, "리더보드"));
        bell        = new DashboardBell(() -> {
            if (!Boolean.TRUE.equals(getClientProperty("sceneBusy"))) cat.callToBell();
        });
        catInput = new JButton("고양이 쓰다듬기") {
            @Override public boolean contains(int x, int y) {
                return scale() > 0 && cat.contains((x - offsetX()) / scale(), (y - offsetY()) / scale());
            }
            @Override protected void paintComponent(Graphics graphics) { }
        };
        catInput.setOpaque(false);
        catInput.setContentAreaFilled(false);
        catInput.setBorderPainted(false);
        catInput.setFocusPainted(false);
        catInput.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        catInput.setToolTipText("고양이 쓰다듬기 · ← → 이동 · ↑ 점프");
        catInput.getAccessibleContext().setAccessibleDescription("클릭하면 눈웃음, 일어서기, 기지개, 짜증, 호기심, 졸림, 부끄러움, 놀람을 표현합니다.");
        catInput.addActionListener(event -> {
            if (!Boolean.TRUE.equals(getClientProperty("sceneBusy"))) cat.interact();
        });
        add(account);
        add(settings);
        for (SceneObjectButton object : objects()) {
            add(object);
        }
        add(bell);
        add(catInput, 0);
        applyIntroState();
        java.awt.event.MouseAdapter signInput = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (!introVisible && SwingUtilities.isLeftMouseButton(event) && onRoomLabel(event.getX(), event.getY())) {
                    curtain.doClick(0);
                }
            }
            @Override public void mouseMoved(java.awt.event.MouseEvent event) {
                curtain.pointerHover(!introVisible && onRoomLabel(event.getX(), event.getY()));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent event) {
                curtain.clearHighlight();
            }
        };
        addMouseListener(signInput);
        addMouseMotionListener(signInput);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "dismissIntro");
        getActionMap().put("dismissIntro", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { dismissIntro(); }
        });
        for (int key : new int[]{java.awt.event.KeyEvent.VK_LEFT, java.awt.event.KeyEvent.VK_RIGHT,
                java.awt.event.KeyEvent.VK_UP}) {
            bindCatKey(key, false);
            bindCatKey(key, true);
        }
    }

    private void bindCatKey(int key, boolean released) {
        String action = "cat-" + key + "-" + released;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0, released), action);
        getActionMap().put(action, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (isShowing()) cat.key(key, !released);
            }
        });
    }

    private void advanceCat() {
        long now               = System.nanoTime();
        double elapsed         = (now - catLastFrame) / 1_000_000_000.0;
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        catLastFrame = now;
        if (window == null || !window.isFocused() || Boolean.TRUE.equals(getClientProperty("sceneBusy"))) {
            cat.releaseKeys();
            return;
        }
        cat.advance(elapsed);
        bell.advance(elapsed);
        // Repaint the cat's full movement band, including the jump arc, without invalidating layout.
        repaint(offsetX(), offsetY() + (int) (650 * scale()),
                (int) (SCENE_WIDTH * scale()) + 1, (int) (260 * scale()) + 1);
    }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(offsetX(), offsetY());
        g.scale(scale(), scale());
        cat.paint(g);
        g.dispose();
    }

    private boolean onRoomLabel(int x, int y) {
        if (scale() <= 0) {
            return false;
        }
        double localX = (x - offsetX()) / scale();
        double localY = (y - offsetY()) / scale();
        return new java.awt.Rectangle(635, 150, 320, 70).contains(localX, localY);
    }

    private List<SceneObjectButton> objects() { return List.of(books, curtain, calendar, leaderboard); }

    private void openRecords(DashboardRecords records, LoginUser user, String title) {
        objects().forEach(SceneObjectButton::clearHighlight);
        try {
            new DashboardRecordDialog(this, records, user.loginId(), title).setVisible(true);
        } finally {
            objects().forEach(SceneObjectButton::clearHighlight);
        }
    }

    static JButton settingsIconButton(Runnable openSettings) {
        JButton button = imageButton("설정", "settings");
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> openSettings.run());
        return button;
    }

    private void applyIntroState() {
        account.setVisible(!introVisible);
        settings.setVisible(!introVisible);
        bell.setVisible(!introVisible);
        catInput.setVisible(true);
        objects().forEach(object -> object.setVisible(!introVisible));
        repaint();
    }

    private void dismissIntro() {
        if (!introVisible || introAnimation != null) {
            return;
        }
        long started = System.nanoTime();
        introAnimation = new Timer(16, event -> {
            float elapsed = Math.min(1f, (System.nanoTime() - started) / 900_000_000f);
            introProgress = elapsed * elapsed * (3f - 2f * elapsed);
            if (elapsed >= 1f) {
                introAnimation.stop();
                introAnimation = null;
                introVisible   = false;
                applyIntroState();
                onIntroDismissed.run();
            }
            repaint();
        });
        introAnimation.start();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.translate(offsetX(), offsetY());
        g.scale(scale(), scale());
        g.drawImage(SCENE, 0, 0, SCENE_WIDTH, SCENE_HEIGHT, null);
        // Use only the inpainted bell footprint; every other original room pixel remains untouched.
        g.drawImage(CLEAN_COUNTER, 1299, 580, 1357, 636,
                1299 * CLEAN_COUNTER.getWidth() / SCENE_WIDTH, 580 * CLEAN_COUNTER.getHeight() / SCENE_HEIGHT,
                1357 * CLEAN_COUNTER.getWidth() / SCENE_WIDTH, 636 * CLEAN_COUNTER.getHeight() / SCENE_HEIGHT, null);
        int windowHeight = 460 * WINDOW.getHeight() / WINDOW.getWidth();
        g.drawImage(WINDOW, 70, 155, 460, windowHeight, null);
        windowAmbient.paint(g, 70, 155, 460, windowHeight);
        g.dispose();
        if (introVisible) {
            // 버튼은 숨겨 입력을 막되 로비의 사물 그림은 시작 화면 뒤에도 유지한다.
            for (SceneObjectButton object : objects()) {
                Graphics2D layer = (Graphics2D) graphics.create(object.getX(), object.getY(), object.getWidth(), object.getHeight());
                object.paint(layer);
                layer.dispose();
            }
            Graphics2D bellLayer = (Graphics2D) graphics.create(bell.getX(), bell.getY(), bell.getWidth(), bell.getHeight());
            bell.paint(bellLayer);
            bellLayer.dispose();
            paintIntro(graphics);
        }
    }

    private void paintIntro(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - introProgress));
        g.setColor(new Color(16, 12, 9, 170));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.translate(offsetX(), offsetY() - 180 * introProgress * scale());
        g.scale(scale(), scale());
        g.setColor(new Color(121, 90, 47));
        g.fillRect(365, 280, 855, 360);
        g.setColor(new Color(38, 30, 22));
        g.fillRect(377, 292, 831, 336);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(TITLE, 445, 340, 695, 169, null);
        BufferedImage enter = SceneObjectButton.read("label-enter.png");
        int enterWidth      = 440;
        int enterHeight     = enterWidth * enter.getHeight() / enter.getWidth();
        g.drawImage(enter, (SCENE_WIDTH - enterWidth) / 2, 535, enterWidth, enterHeight, null);
        g.dispose();
    }

    private double scale() { return Math.min(getWidth() / (double) SCENE_WIDTH, getHeight() / (double) SCENE_HEIGHT); }
    private int offsetX() { return (int) ((getWidth() - SCENE_WIDTH * scale()) / 2); }
    private int offsetY() { return (int) ((getHeight() - SCENE_HEIGHT * scale()) / 2); }

    private void place(JComponent component, int x, int y, int width, int height) {
        component.setBounds(offsetX() + (int) (x * scale()), offsetY() + (int) (y * scale()),
                (int) (width * scale()), (int) (height * scale()));
    }

    @Override public void doLayout() {
        place(account, 80, 35, 620, 48);
        place(settings, 1460, 35, 75, 75);
        place(books, 65, 430, 470, 400);
        place(curtain, 584, 298, 414, 522);
        place(leaderboard, 1040, 410, 165, 420);
        place(calendar, 1195, 290, 250, 245);
        place(bell, 1295, 577, 74, 58);
        catInput.setBounds(0, 0, getWidth(), getHeight());
    }

    @Override public void removeNotify() {
        windowTimer.stop();
        catTimer.stop();
        cat.releaseKeys();
        if (introAnimation != null) {
            introAnimation.stop();
            introAnimation = null;
            introProgress  = 0f;
        }
        super.removeNotify();
    }

    private void showSettings() {
        objects().forEach(SceneObjectButton::clearHighlight);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "설정", JDialog.ModalityType.APPLICATION_MODAL);
        JPanel panel   = new JPanel(null);
        panel.setBackground(new Color(27, 23, 20));
        JLabel heading = imageLabel("설정", "settings");
        heading.setFont(SceneObjectButton.PixelFont.at(28));
        heading.setForeground(INK);
        heading.setBounds(30, 26, 300, 55);
        JButton exit = imageButton("로그아웃", "logout");
        exit.setFont(SceneObjectButton.PixelFont.at(20));
        exit.setBounds(35, 185, 290, 48);
        JButton music   = AppScreens.musicToggle();
        JButton effects = AppScreens.effectsToggle();
        music.setBounds(35, 92, 140, 42);
        effects.setBounds(185, 92, 140, 42);
        panel.add(music);
        panel.add(effects);
        exit.addActionListener(event -> {
            dialog.dispose();
            logout.run();
        });
        panel.add(heading);
        panel.add(exit);
        dialog.setContentPane(panel);
        dialog.setSize(360, 270);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    static JLabel imageLabel(String name, String asset) {
        BufferedImage label = SceneObjectButton.read("label-" + asset + ".png");
        JLabel result = new JLabel(name) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                double scale = Math.min(getWidth() / (double) label.getWidth(), getHeight() / (double) label.getHeight());
                int width    = (int) (label.getWidth() * scale);
                int height   = (int) (label.getHeight() * scale);
                g.drawImage(label, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
                g.dispose();
            }
        };
        result.setPreferredSize(new java.awt.Dimension(240, 40));
        return result;
    }

    static JButton imageButton(String name, String asset) {
        BufferedImage label = SceneObjectButton.read("label-" + asset + ".png");
        JButton button = new JButton(name) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                double scale = Math.min((getWidth() - 12.0) / label.getWidth(), (getHeight() - 12.0) / label.getHeight());
                int width    = (int) (label.getWidth() * scale);
                int height   = (int) (label.getHeight() * scale);
                g.drawImage(label, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
                g.dispose();
            }
        };
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText(name);
        button.getAccessibleContext().setAccessibleName(name);
        return button;
    }

    private static BufferedImage loadTitle() {
        try (InputStream stream = DashboardPage.class.getResourceAsStream("/images/dashboard/title_logo.png")) {
            return ImageIO.read(stream);
        } catch (Exception exception) {
            throw new IllegalStateException("시작 화면 제목을 읽을 수 없습니다.", exception);
        }
    }
}
