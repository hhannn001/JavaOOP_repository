package coderoom.pages.login;

import coderoom.entities.user.LoginUser;
import coderoom.features.auth.LoginResult;
import coderoom.features.auth.LoginService;
import coderoom.shared.db.StorageException;
import coderoom.shared.ui.PixelTheme;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;

/**
 * 담당: B. 로그인 입력과 결과 표시를 담당하는 Swing 패널.
 * EDT에서 생성하고 사용한다. LoginService와 성공 콜백은 A의 조립 지점에서 전달한다.
 * DB 조회·비밀번호 검증은 작업 스레드에서 실행하며 AppController를 직접 참조하지 않는다.
 */
public final class LoginPage extends JPanel {
    private static final Color BACKGROUND       = new Color(16, 28, 24);
    private static final Color PANEL            = new Color(20, 29, 26, 232);
    private static final Color PANEL_EDGE       = new Color(231, 201, 150, 82);
    private static final Color FIELD            = new Color(249, 239, 220);
    private static final Color GOLD             = new Color(248, 199, 117);
    private static final Color INK              = new Color(43, 31, 20);
    private static final Color TEXT             = new Color(255, 242, 218);
    private static final Color ERROR            = new Color(255, 178, 162);
    private static final Color SUCCESS          = new Color(183, 242, 190);
    private static final BufferedImage ENTRANCE = loadImage("/images/login/entrance.jpg");
    private static final BufferedImage LOGO     = loadImage("/images/dashboard/title_logo.png");

    private final LoginService service;
    private final Consumer<LoginUser> onLogin;
    private final JTextField loginIdField      = new JTextField(14);
    private final JPasswordField passwordField = new JPasswordField(14);
    private final JButton loginButton          = new JButton("로그인");
    private final JButton signup               = new JButton("회원가입");
    private final JCheckBox rememberLogin      = new JCheckBox("자동 로그인");
    private final JToggleButton showPassword   = new JToggleButton(new EyeIcon(false));
    private final char passwordMask            = passwordField.getEchoChar();

    private boolean registering;
    private final JTextArea statusMessage = new JTextArea(2, 0);
    private boolean busy;
    private boolean authenticated;
    private long generation;

    /** 기존 A의 호출을 지원한다. 인증 서비스를 받기 전에는 로그인할 수 없다. */
    public LoginPage(Consumer<LoginUser> onLogin) {
        this(onLogin, null);
    }

    /** 인증 서비스가 연결된 실제 로그인 화면. */
    public LoginPage(LoginService service, Consumer<LoginUser> onLogin) {
        this(onLogin, Objects.requireNonNull(service, "로그인 서비스가 필요합니다."));
    }

    /** 공통 화면 구성. service가 null이면 서비스 연결 대기 상태로 표시한다. */
    private LoginPage(Consumer<LoginUser> onLogin, LoginService service) {
        requireEdt();
        this.service = service;
        this.onLogin = Objects.requireNonNull(onLogin, "로그인 성공 콜백이 필요합니다.");
        setLayout(new GridBagLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PANEL);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g.setColor(PANEL_EDGE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                g.dispose();
            }
        };
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 13, 18));
        Font bodyFont = PixelTheme.font(14);

        loginIdField.setName("loginId");
        passwordField.setName("password");
        loginButton.setName("loginButton");
        statusMessage.setName("statusMessage");
        loginButton.setFont(bodyFont.deriveFont(Font.BOLD));
        loginButton.setForeground(INK);
        loginButton.setUI(new BasicButtonUI() {
            @Override public void paint(Graphics graphics, javax.swing.JComponent component) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = loginButton.getModel().isPressed() ? new Color(224, 164, 84)
                        : loginButton.getModel().isRollover() ? new Color(255, 217, 151) : GOLD;
                g.setColor(loginButton.isEnabled() ? fill : new Color(142, 127, 99));
                g.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 12, 12);
                if (component.hasFocus()) {
                    g.setColor(INK);
                    g.drawRoundRect(3, 3, component.getWidth() - 7, component.getHeight() - 7, 8, 8);
                }
                g.dispose();
                super.paint(graphics, component);
            }
        });
        loginButton.setContentAreaFilled(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder());
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(92, 38));

        signup.setFont(bodyFont.deriveFont(Font.BOLD));
        signup.setForeground(GOLD);
        signup.setContentAreaFilled(false);
        signup.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 2));
        signup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signup.setUI(new javax.swing.plaf.metal.MetalButtonUI());

        rememberLogin.setName("rememberLogin");
        rememberLogin.setFont(bodyFont);
        rememberLogin.setOpaque(false);
        rememberLogin.setForeground(TEXT);
        rememberLogin.setVisible(service != null && service.canRememberLogin());

        showPassword.setName("showPassword");
        showPassword.setSelectedIcon(new EyeIcon(true));
        showPassword.setContentAreaFilled(false);
        showPassword.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        showPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPassword.setFocusPainted(true);
        showPassword.addActionListener(event -> updatePasswordVisibility());
        updatePasswordVisibility();

        JPanel idRow       = fieldRow(loginIdField, bodyFont, null);
        JPanel passwordRow = fieldRow(passwordField, bodyFont, showPassword);
        loginIdField.getAccessibleContext().setAccessibleName("로그인 ID");
        passwordField.getAccessibleContext().setAccessibleName("비밀번호");
        JLabel idLabel = new JLabel("아이디");
        idLabel.setFont(bodyFont);
        idLabel.setForeground(TEXT);
        idLabel.setLabelFor(loginIdField);
        idLabel.setDisplayedMnemonic(KeyEvent.VK_I);
        JLabel passwordLabel = new JLabel("비밀번호");
        passwordLabel.setFont(bodyFont);
        passwordLabel.setForeground(TEXT);
        passwordLabel.setLabelFor(passwordField);
        passwordLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        loginButton.setMnemonic(KeyEvent.VK_L);
        statusMessage.setFont(bodyFont.deriveFont(12f));
        statusMessage.setForeground(TEXT);
        statusMessage.setEditable(false);
        statusMessage.setFocusable(false);
        statusMessage.setOpaque(false);
        statusMessage.setLineWrap(true);
        statusMessage.setWrapStyleWord(true);
        statusMessage.getAccessibleContext().setAccessibleName("로그인 안내");

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx  = 0;
        layout.gridy  = 0;
        layout.fill   = GridBagConstraints.HORIZONTAL;
        layout.anchor = GridBagConstraints.LINE_START;
        layout.insets = new Insets(0, 0, 6, 10);
        form.add(idLabel, layout);
        layout.gridy++;
        form.add(passwordLabel, layout);

        layout.gridx   = 1;
        layout.gridy   = 0;
        layout.weightx = 1;
        form.add(idRow, layout);
        layout.gridy++;
        form.add(passwordRow, layout);

        layout.gridx      = 2;
        layout.gridy      = 0;
        layout.weightx    = 0;
        layout.gridheight = 2;
        layout.fill       = GridBagConstraints.BOTH;
        layout.insets     = new Insets(0, 0, 6, 0);
        form.add(loginButton, layout);

        JPanel options = new JPanel(new BorderLayout(8, 0));
        options.setOpaque(false);
        options.add(rememberLogin, BorderLayout.LINE_START);
        options.add(signup, BorderLayout.LINE_END);
        signup.setVisible(service != null && service.canRegister());

        layout.gridx      = 0;
        layout.gridy      = 2;
        layout.gridwidth  = 3;
        layout.gridheight = 1;
        layout.fill       = GridBagConstraints.HORIZONTAL;
        layout.insets     = new Insets(4, 0, 0, 0);
        form.add(options, layout);

        JPanel entrance = new JPanel(new BorderLayout(0, 16));
        entrance.setOpaque(false);
        entrance.add(logo(), BorderLayout.PAGE_START);
        entrance.add(form, BorderLayout.CENTER);
        statusMessage.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        entrance.add(statusMessage, BorderLayout.PAGE_END);
        add(entrance);

        loginButton.addActionListener(event -> submitLogin());
        passwordField.addActionListener(event -> submitLogin());
        signup.addActionListener(event -> {
            generation++;
            setRegistrationMode(!registering);
        });
        loginIdField.addActionListener(event -> passwordField.requestFocusInWindow());
        updateControls();
        if (service == null) {
            statusMessage.setText("인증 서비스 연결 대기 중입니다.");
        } else if (service.canRememberLogin()) {
            authenticate(() -> service.restoreLogin().orElse(null), true);
        }
    }

    private static JLabel logo() {
        JLabel logo = new JLabel("", JLabel.CENTER) {
            @Override protected void paintComponent(Graphics graphics) {
                if (LOGO == null) {
                    super.paintComponent(graphics);
                    return;
                }
                int width  = Math.min(getWidth(), 420);
                int height = width * LOGO.getHeight() / LOGO.getWidth();
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(LOGO, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
                g.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(420, 106));
        logo.getAccessibleContext().setAccessibleName("코드 방탈출");
        if (LOGO == null) {
            logo.setText("코드 방탈출");
            logo.setFont(PixelTheme.font(34).deriveFont(Font.BOLD));
            logo.setForeground(TEXT);
        }
        return logo;
    }

    private static JPanel fieldRow(JTextComponent field, Font font, JToggleButton toggle) {
        field.setFont(font);
        field.setForeground(INK);
        field.setCaretColor(INK);
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, toggle == null ? 10 : 0));
        JPanel row = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(FIELD);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (field.hasFocus() || (toggle != null && toggle.hasFocus())) {
                    g.setColor(GOLD);
                    g.setStroke(new BasicStroke(2));
                    g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                }
                g.dispose();
            }
        };
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER);
        FocusAdapter repaintOnFocus = new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { row.repaint(); }
            @Override public void focusLost(FocusEvent event) { row.repaint(); }
        };
        field.addFocusListener(repaintOnFocus);
        if (toggle != null) {
            toggle.addFocusListener(repaintOnFocus);
            row.add(toggle, BorderLayout.LINE_END);
        }
        return row;
    }

    private void updatePasswordVisibility() {
        boolean visible = showPassword.isSelected();
        String label    = visible ? "비밀번호 숨기기" : "비밀번호 보기";
        passwordField.setEchoChar(visible ? (char) 0 : passwordMask);
        showPassword.setToolTipText(label);
        showPassword.getAccessibleContext().setAccessibleName(label);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (ENTRANCE == null) {
            return;
        }
        double scale = Math.max((double) getWidth() / ENTRANCE.getWidth(),
                (double) getHeight() / ENTRANCE.getHeight());
        int width     = (int) Math.ceil(ENTRANCE.getWidth() * scale);
        int height    = (int) Math.ceil(ENTRANCE.getHeight() * scale);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(ENTRANCE, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
        g.setPaint(new GradientPaint(0, 0, new Color(9, 14, 13, 34),
                0, getHeight(), new Color(9, 14, 13, 99)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream input = LoginPage.class.getResourceAsStream(path)) {
            return input == null ? null : ImageIO.read(input);
        } catch (IOException exception) {
            return null;
        }
    }

    private record EyeIcon(boolean revealed) implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(component.isEnabled() ? INK : new Color(142, 127, 99));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D eye = new Path2D.Double();
            eye.moveTo(1, 9);
            eye.quadTo(9, -2, 17, 9);
            eye.quadTo(9, 20, 1, 9);
            g.draw(eye);
            g.drawOval(7, 7, 4, 4);
            if (revealed) {
                g.drawLine(2, 16, 16, 2);
            }
            g.dispose();
        }
    }

    /** 외부에서 받은 장애도 비밀값을 포함하지 않는 사용자용 메시지로 표시한다. */
    public void showError(String message) {
        requireEdt();
        statusMessage.setForeground(ERROR);
        statusMessage.setText(Objects.requireNonNull(message, "안내 메시지가 필요합니다."));
    }

    private void submitLogin() {
        requireEdt();
        if (service == null || busy || authenticated) {
            return;
        }
        if (registering) {
            submitSignup();
            return;
        }
        generation++;
        String loginId    = loginIdField.getText();
        char[] password   = passwordField.getPassword();
        boolean remember = rememberLogin.isSelected();
        resetPassword();
        if (loginId.isBlank() || password.length == 0) {
            Arrays.fill(password, '\0');
            showError("ID와 비밀번호를 입력하세요.");
            if (loginId.isBlank()) loginIdField.requestFocusInWindow();
            else passwordField.requestFocusInWindow();
            return;
        }
        authenticate(() -> service.login(loginId, password, remember), false);
    }

    private void authenticate(Supplier<LoginResult> attempt, boolean restoring) {
        if (!restoring) {
            setBusy(true);
            statusMessage.setForeground(TEXT);
            statusMessage.setText("로그인 확인 중...");
        }
        long token = ++generation;
        new SwingWorker<LoginResult, Void>() {
            @Override
            protected LoginResult doInBackground() {
                return attempt.get();
            }

            @Override
            protected void done() {
                // 화면이 제거된 후 도착한 결과로 화면을 갱신하거나 성공을 알리지 않는다.
                if (token != generation) return;
                LoginResult result = null;
                statusMessage.setText("");
                try {
                    result = get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError("로그인이 중단되었습니다. 다시 시도하세요.");
                } catch (CancellationException ex) {
                    showError("로그인이 취소되었습니다. 다시 시도하세요.");
                } catch (ExecutionException ex) {
                    if (restoring) {
                        showError("자동 로그인에 실패했습니다. ID와 비밀번호로 로그인하세요.");
                    } else if (ex.getCause() instanceof StorageException) {
                        showError("로그인 정보를 처리하지 못했습니다. 다시 시도하세요.");
                    } else {
                        showError("로그인 처리 중 문제가 발생했습니다. 다시 시도하세요.");
                    }
                } finally {
                    setBusy(false);
                }
                if (result != null && result.success()) {
                    authenticated = true;
                    updateControls();
                    statusMessage.setForeground(SUCCESS);
                    statusMessage.setText(result.message());
                    // 콜백 오류는 인증 실패로 바꾸지 않는다. 화면 전환은 A가 담당한다.
                    onLogin.accept(result.user());
                } else {
                    if (result != null) showError(result.message());
                    if (restoring) { loginIdField.requestFocusInWindow(); }
                    else { passwordField.requestFocusInWindow(); }
                }
            }
        }.execute();
    }

    private void setRegistrationMode(boolean value) {
        registering = value;
        rememberLogin.setVisible(!value && service != null && service.canRememberLogin());
        resetPassword();
        signup.setText(value ? "로그인으로 돌아가기" : "회원가입");
        statusMessage.setText("");
        setBusy(false);
        revalidate();
        repaint();
    }

    private void submitSignup() {
        String id     = loginIdField.getText();
        char[] secret = passwordField.getPassword();
        resetPassword();
        statusMessage.setForeground(TEXT);
        statusMessage.setText("계정을 만드는 중입니다.");
        setBusy(true);
        long token = ++generation;
        new SwingWorker<LoginService.Registration, Void>() {
            @Override protected LoginService.Registration doInBackground() {
                return service.register(id, secret);
            }

            @Override protected void done() {
                if (token != generation) { return; }
                setBusy(false);
                try {
                    switch (get()) {
                        case CREATED -> {
                            setRegistrationMode(false);
                            statusMessage.setForeground(SUCCESS);
                            statusMessage.setText("가입했습니다. 새 계정으로 로그인하세요.");
                            passwordField.requestFocusInWindow();
                        }
                        case DUPLICATE -> showError("이미 사용 중인 ID입니다.");
                        case EMPTY -> showError("ID와 비밀번호를 입력하세요.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("가입이 중단되었습니다. 다시 시도하세요.");
                } catch (ExecutionException | CancellationException exception) {
                    showError("계정을 저장하지 못했습니다. 다시 시도하세요.");
                }
            }
        }.execute();
    }

    private void resetPassword() {
        passwordField.setText("");
        showPassword.setSelected(false);
        updatePasswordVisibility();
    }

    private void setBusy(boolean value) {
        busy = value;
        loginButton.setText(value ? "확인 중..." : registering ? "가입하기" : "로그인");
        updateControls();
    }

    private void updateControls() {
        boolean enabled = service != null && !busy && !authenticated;
        loginIdField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        signup.setEnabled(enabled);
        rememberLogin.setEnabled(enabled);
        showPassword.setEnabled(enabled);
    }

    @Override
    public void removeNotify() {
        // 진행 중 작업은 finally에서 암호 배열을 지울 때까지 완료시키고, 이전 완료 알림을 무효화한다.
        generation++;
        authenticated = false;
        setRegistrationMode(false);
        super.removeNotify();
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("로그인 화면은 EDT에서 사용해야 합니다.");
        }
    }
}
