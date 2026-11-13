package coderoom.app;

import coderoom.entities.user.LoginUser;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * 담당: A
 * 역할: 로그인 직후의 방탈출 대시보드와 맵 선택 진입점을 표시한다.
 * 기록과 진척도 서비스가 연결되기 전에는 해당 오브젝트에서 연동 대기 안내만 보여 준다.
 */
final class DashboardPage extends JPanel {
    private static final int SCENE_WIDTH  = 1585;
    private static final int SCENE_HEIGHT = 992;
    private static final BufferedImage SCENE    = loadImage("/images/dashboard/study_dashboard.png");
    private static final BufferedImage PLANK    = loadImage("/images/dashboard/title_plank.png");
    private static final BufferedImage TITLE    = loadImage("/images/dashboard/title_logo.png");
    private static final BufferedImage PROMPT   = loadImage("/images/dashboard/press_enter_pixel.png");
    private static final BufferedImage GEAR     = loadImage("/images/dashboard/settings_gray.png");
    private static final BufferedImage LIGHTNING = loadImage("/images/dashboard/window_lightning.png");
    private static final Image RAIN             = loadGif("/images/dashboard/window_rain.gif");
    private static final Color WHITE            = Color.WHITE;
    private static final float[] LIGHTNING_STEP = {0.65f, 0f, 0.90f, 0.38f, 0f};

    private final JLabel avatar;
    private final JLabel accountCaption;
    private final JLabel accountName;
    private final JButton settings;
    private final JButton books;
    private final JButton door;
    private final JButton journal;
    private final Runnable logout;
    private final Runnable onIntroDismissed;
    private boolean introVisible;
    private Timer lightningDelay;
    private Timer lightningPulse;
    private int lightningIndex;
    private float lightningOpacity;
    private String hoveredObject;

    /** A의 화면 전환 콜백을 받아 사용자 ID만 표시한다. 기록 값은 임의로 생성하지 않는다. */
    DashboardPage(LoginUser user, Runnable openMaps, Runnable logout) {
        this(user, openMaps, logout, true, () -> {});
    }

    /** 제목 화면은 로그인 직후 한 번만 보이고 맵에서 돌아오면 이미 열린 방을 보여 준다. */
    DashboardPage(LoginUser user, Runnable openMaps, Runnable logout,
            boolean showIntro, Runnable onIntroDismissed) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(openMaps);
        this.logout           = Objects.requireNonNull(logout);
        this.onIntroDismissed = Objects.requireNonNull(onIntroDismissed);
        this.introVisible     = showIntro;
        setLayout(null);
        setBackground(new Color(8, 19, 19));

        // 계정 정보는 방의 어두운 좌상단에 작게 겹친다.
        String initial = user.loginId().substring(0, 1).toUpperCase(Locale.ROOT);
        avatar = new JLabel(initial, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(14, 27, 25, 175));
                g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                g.setColor(new Color(234, 201, 133));
                g.setStroke(new BasicStroke(2f));
                g.drawOval(1, 1, getWidth() - 2, getHeight() - 2);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        avatar.setFont(DashboardFont.bold(20f));
        avatar.setForeground(WHITE);
        accountCaption = new JLabel("현재 로그인된 아이디");
        accountCaption.setFont(DashboardFont.regular(12f));
        accountCaption.setForeground(WHITE);
        accountName = new JLabel(user.loginId());
        accountName.setFont(DashboardFont.bold(21f));
        accountName.setForeground(WHITE);

        settings = settingsIconButton(() -> showSettings());
        books    = hotspot("풀이한 문제", () -> showPending("풀이한 문제"));
        door     = hotspot("방 선택하기", openMaps);
        journal  = hotspot("진척도", () -> showPending("진척도"));
        add(avatar);
        add(accountCaption);
        add(accountName);
        add(settings);
        add(books);
        add(door);
        add(journal);
        applyIntroState();

        // 나무판, 제목 로고, Enter 안내는 독립 이미지라 각각 애니메이션을 붙일 수 있다.
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "dismissIntro");
        getActionMap().put("dismissIntro", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                dismissIntro();
            }
        });
    }

    /** 게임 화면에서도 같은 중립적인 회색 설정 아이콘을 쓴다. */
    static JButton settingsIconButton(Runnable openSettings) {
        JButton button = new JButton("설정") {
            @Override protected void paintComponent(Graphics graphics) {
                if (GEAR == null) {
                    super.paintComponent(graphics);
                    return;
                }
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int size = Math.min(getWidth(), getHeight()) - 6;
                g.drawImage(GEAR, (getWidth() - size) / 2, (getHeight() - size) / 2, size, size, null);
                g.dispose();
            }
        };
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> openSettings.run());
        return button;
    }

    private JButton hotspot(String name, Runnable action) {
        JButton button = new JButton(name) {
            @Override protected void paintComponent(Graphics graphics) {
                // 이름은 배경 그림의 나무 표찰에 새겨져 있으므로 클릭 영역은 그리지 않는다.
            }
        };
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(name);
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                hoveredObject = name;
                repaint();
            }

            @Override public void mouseExited(MouseEvent event) {
                hoveredObject = null;
                repaint();
            }
        });
        button.addActionListener(event -> action.run());
        return button;
    }

    /** 제목 화면에서는 오브젝트·계정·설정을 잠시 숨겨 Enter 입력에 집중시킨다. */
    private void applyIntroState() {
        boolean shown = !introVisible;
        avatar.setVisible(shown);
        accountCaption.setVisible(shown);
        accountName.setVisible(shown);
        settings.setVisible(shown);
        books.setVisible(shown);
        door.setVisible(shown);
        journal.setVisible(shown);
        repaint();
    }

    private void dismissIntro() {
        if (introVisible) {
            introVisible = false;
            applyIntroState();
            onIntroDismissed.run();
        }
    }

    /** 배경, 창문 빗줄기, 불규칙한 번개, 제목 간판을 독립 레이어로 그린다. */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (SCENE != null) {
            g.drawImage(SCENE, 0, 0, getWidth(), getHeight(), null);
        }
        if (RAIN != null) {
            drawAtScene(g, RAIN, 1250, 0, 280, 430);
        }
        if (introVisible) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (lightningOpacity > 0f) {
            paintLightning(g);
        }
        if (introVisible) {
            paintIntro(g);
        } else if (hoveredObject != null) {
            paintHoverOutline(g);
        }
        g.dispose();
    }

    /** 나무판과 제목·키 안내 이미지를 분리해서 그린다. */
    private void paintIntro(Graphics2D g) {
        drawAtScene(g, PLANK, 415, 160, 755, 245);
        drawAtScene(g, TITLE, 475, 218, 635, 154);
        drawAtScene(g, PROMPT, 555, 457, 475, 41);
    }

    /** 호버 중인 책장·문·일지의 외곽에만 금빛 윤곽을 표시한다. */
    private void paintHoverOutline(Graphics2D g) {
        Graphics2D layer = (Graphics2D) g.create();
        layer.scale(getWidth() / (double) SCENE_WIDTH, getHeight() / (double) SCENE_HEIGHT);
        layer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Path2D outline = new Path2D.Double();
        switch (hoveredObject) {
            case "풀이한 문제" -> {
                outline.moveTo(27, 45);
                outline.lineTo(500, 91);
                outline.lineTo(500, 785);
                outline.lineTo(27, 870);
                outline.closePath();
            }
            case "방 선택하기" -> {
                outline.moveTo(635, 90);
                outline.lineTo(985, 90);
                outline.lineTo(985, 780);
                outline.lineTo(635, 780);
                outline.closePath();
            }
            case "진척도" -> {
                outline.moveTo(1140, 414);
                outline.lineTo(1540, 432);
                outline.lineTo(1510, 670);
                outline.lineTo(1084, 650);
                outline.closePath();
            }
            default -> { layer.dispose(); return; }
        }
        layer.setColor(new Color(255, 221, 147, 65));
        layer.setStroke(new BasicStroke(13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        layer.draw(outline);
        layer.setColor(new Color(255, 245, 214, 220));
        layer.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        layer.draw(outline);
        layer.dispose();
    }

    /** 별도 번개 이미지를 창문에 얹고 방 전체에도 약한 반사광을 준다. */
    private void paintLightning(Graphics2D g) {
        int roomAlpha = Math.round(28 * lightningOpacity);
        g.setColor(new Color(195, 220, 255, roomAlpha));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (LIGHTNING != null) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, lightningOpacity));
            drawAtScene(g, LIGHTNING, 1250, 0, 280, 430);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    private void drawAtScene(Graphics2D g, Image image, int x, int y, int width, int height) {
        if (image != null) {
            g.drawImage(image, scaleX(x), scaleY(y), scaleX(width), scaleY(height), this);
        }
    }

    private int scaleX(int x) {
        return Math.round(x * getWidth() / (float) SCENE_WIDTH);
    }

    private int scaleY(int y) {
        return Math.round(y * getHeight() / (float) SCENE_HEIGHT);
    }

    /** 원본 그림의 오브젝트 영역에 투명 클릭 영역을 맞춘다. */
    @Override public void doLayout() {
        avatar.setBounds(scaleX(62), scaleY(61), scaleX(54), scaleY(54));
        accountCaption.setBounds(scaleX(128), scaleY(63), scaleX(270), scaleY(20));
        accountName.setBounds(scaleX(128), scaleY(83), scaleX(290), scaleY(30));
        settings.setBounds(scaleX(1474), scaleY(53), scaleX(67), scaleY(67));
        books.setBounds(scaleX(25), scaleY(135), scaleX(480), scaleY(665));
        door.setBounds(scaleX(628), scaleY(113), scaleX(345), scaleY(665));
        journal.setBounds(scaleX(1070), scaleY(403), scaleX(465), scaleY(400));
    }

    /** 비 GIF는 화면이 보이는 동안 자동 반복하고 번개 시점은 매번 새 난수로 정한다. */
    @Override public void addNotify() {
        super.addNotify();
        scheduleLightning();
    }

    @Override public void removeNotify() {
        if (lightningDelay != null) {
            lightningDelay.stop();
        }
        if (lightningPulse != null) {
            lightningPulse.stop();
        }
        super.removeNotify();
    }

    private void scheduleLightning() {
        if (!isDisplayable()) {
            return;
        }
        int delay = ThreadLocalRandom.current().nextInt(25000, 55000);
        lightningDelay = new Timer(delay, event -> {
            lightningDelay.stop();
            lightningIndex = 0;
            lightningPulse = new Timer(75, pulse -> {
                lightningOpacity = LIGHTNING_STEP[lightningIndex++];
                repaint();
                if (lightningIndex == LIGHTNING_STEP.length) {
                    lightningPulse.stop();
                    scheduleLightning();
                }
            });
            lightningPulse.setInitialDelay(0);
            lightningPulse.start();
        });
        lightningDelay.setRepeats(false);
        lightningDelay.start();
    }

    /** 다른 담당자의 기록 계약이 완성되기 전에는 메뉴 이름과 연결 대기 상태만 알린다. */
    private void showPending(String title) {
        JOptionPane.showMessageDialog(this, title + " · 연동 전");
    }

    /** 설정 창의 맨 아래 로그아웃은 A의 세션 종료 콜백에 연결한다. */
    private void showSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "설정", JDialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(null);
        panel.setBackground(Color.WHITE);
        JLabel heading = new JLabel("설정", SwingConstants.CENTER);
        heading.setFont(DashboardFont.bold(27f));
        heading.setBounds(30, 26, 300, 55);
        JLabel pending = new JLabel("다른 메뉴는 준비 중입니다", SwingConstants.CENTER);
        pending.setFont(DashboardFont.regular(16f));
        pending.setBounds(30, 100, 300, 35);
        JButton exit = new JButton("로그아웃");
        exit.setFont(DashboardFont.bold(18f));
        exit.setBounds(35, 216, 290, 48);
        exit.addActionListener(event -> {
            dialog.dispose();
            logout.run();
        });
        panel.add(heading);
        panel.add(pending);
        panel.add(exit);
        dialog.setContentPane(panel);
        dialog.setSize(360, 310);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** 이미지 에셋이 누락돼도 화면 전환 자체는 유지한다. */
    private static BufferedImage loadImage(String path) {
        try (InputStream stream = DashboardPage.class.getResourceAsStream(path)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Image loadGif(String path) {
        URL url = DashboardPage.class.getResource(path);
        return url == null ? null : new ImageIcon(url).getImage();
    }

    /** 세부 계정 글꼴은 기존 Pretendard 에셋을 재사용한다. */
    private static final class DashboardFont {
        private static final java.awt.Font REGULAR = load("/fonts/Pretendard-Regular.otf");
        private static final java.awt.Font BOLD    = load("/fonts/Pretendard-Bold.otf");

        private static java.awt.Font regular(float size) {
            return REGULAR.deriveFont(size);
        }

        private static java.awt.Font bold(float size) {
            return BOLD.deriveFont(size);
        }


        private static java.awt.Font load(String path) {
            try (InputStream stream = DashboardPage.class.getResourceAsStream(path)) {
                return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream);
            } catch (Exception ignored) {
                return new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 18);
            }
        }
    }
}
