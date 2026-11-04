package coderoom.pages.login;

import coderoom.entities.user.LoginUser;
import coderoom.features.auth.LoginResult;
import coderoom.features.auth.LoginService;
import coderoom.shared.db.StorageException;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * 담당: B. 로그인 입력과 결과 표시를 담당하는 Swing 패널.
 * EDT에서 생성하고 사용한다. LoginService와 성공 콜백은 A의 조립 지점에서 전달한다.
 * DB 조회·비밀번호 검증은 작업 스레드에서 실행하며 AppController를 직접 참조하지 않는다.
 */
public final class LoginPage extends JPanel {
    private static final Color BACKGROUND = new Color(244, 247, 251);
    private static final Color TEXT = new Color(25, 45, 67);
    private static final Color MUTED = new Color(97, 117, 138);
    private static final Color ERROR = new Color(176, 36, 36);
    private static final Color SUCCESS = new Color(0, 115, 90);
    private final LoginService service;
    private final Consumer<LoginUser> onLogin;
    private final JTextField loginIdField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JButton loginButton = new JButton("로그인");
    private final JTextArea statusMessage = new JTextArea(2, 24);
    private boolean busy;
    private boolean authenticated;
    private long generation;

    public LoginPage(LoginService service, Consumer<LoginUser> onLogin) {
        requireEdt();
        this.service = Objects.requireNonNull(service, "로그인 서비스가 필요합니다.");
        this.onLogin = Objects.requireNonNull(onLogin, "로그인 성공 콜백이 필요합니다.");
        setLayout(new GridBagLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 229, 239)),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)));
        Font bodyFont = loginIdField.getFont().deriveFont(14f);
        JLabel title = new JLabel("CODE ROOM");
        title.setFont(bodyFont.deriveFont(Font.BOLD, 26f));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("계정으로 로그인하세요.");
        subtitle.setFont(bodyFont);
        subtitle.setForeground(MUTED);

        loginIdField.setName("loginId");
        passwordField.setName("password");
        loginButton.setName("loginButton");
        statusMessage.setName("statusMessage");
        loginIdField.setFont(bodyFont);
        passwordField.setFont(bodyFont);
        loginButton.setFont(bodyFont);
        loginIdField.getAccessibleContext().setAccessibleName("로그인 ID");
        passwordField.getAccessibleContext().setAccessibleName("비밀번호");
        JLabel idLabel = new JLabel("ID");
        idLabel.setLabelFor(loginIdField);
        idLabel.setDisplayedMnemonic(KeyEvent.VK_I);
        JLabel passwordLabel = new JLabel("비밀번호");
        passwordLabel.setLabelFor(passwordField);
        passwordLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        loginButton.setMnemonic(KeyEvent.VK_L);
        statusMessage.setFont(bodyFont.deriveFont(12f));
        statusMessage.setForeground(MUTED);
        statusMessage.setEditable(false);
        statusMessage.setFocusable(false);
        statusMessage.setOpaque(false);
        statusMessage.setLineWrap(true);
        statusMessage.setWrapStyleWord(true);
        statusMessage.getAccessibleContext().setAccessibleName("로그인 안내");

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.weightx = 1;
        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.anchor = GridBagConstraints.LINE_START;
        layout.gridy = 0;
        layout.insets = new Insets(0, 0, 6, 0);
        form.add(title, layout);
        layout.gridy++;
        layout.insets = new Insets(0, 0, 24, 0);
        form.add(subtitle, layout);
        layout.gridy++;
        layout.insets = new Insets(0, 0, 6, 0);
        form.add(idLabel, layout);
        layout.gridy++;
        layout.insets = new Insets(0, 0, 16, 0);
        form.add(loginIdField, layout);
        layout.gridy++;
        layout.insets = new Insets(0, 0, 6, 0);
        form.add(passwordLabel, layout);
        layout.gridy++;
        layout.insets = new Insets(0, 0, 20, 0);
        form.add(passwordField, layout);
        layout.gridy++;
        layout.insets = new Insets(0, 0, 12, 0);
        form.add(loginButton, layout);
        layout.gridy++;
        layout.insets = new Insets(0, 0, 0, 0);
        form.add(statusMessage, layout);
        add(form);

        loginButton.addActionListener(event -> submitLogin());
        passwordField.addActionListener(event -> submitLogin());
        loginIdField.addActionListener(event -> passwordField.requestFocusInWindow());
    }

    /** 외부에서 받은 장애도 비밀값을 포함하지 않는 사용자용 메시지로 표시한다. */
    public void showError(String message) {
        requireEdt();
        statusMessage.setForeground(ERROR);
        statusMessage.setText(Objects.requireNonNull(message, "안내 메시지가 필요합니다."));
    }

    private void submitLogin() {
        requireEdt();
        if (busy || authenticated) {
            return;
        }
        String loginId = loginIdField.getText();
        char[] password = passwordField.getPassword();
        passwordField.setText("");
        if (loginId.isBlank() || password.length == 0) {
            Arrays.fill(password, '\0');
            showError("ID와 비밀번호를 입력하세요.");
            if (loginId.isBlank()) loginIdField.requestFocusInWindow();
            else passwordField.requestFocusInWindow();
            return;
        }
        setBusy(true);
        long token = ++generation;
        statusMessage.setForeground(MUTED);
        statusMessage.setText("로그인 확인 중입니다...");
        new SwingWorker<LoginResult, Void>() {
            @Override
            protected LoginResult doInBackground() {
                try {
                    return service.login(loginId, password);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                // 화면이 제거된 후 도착한 결과로 화면을 갱신하거나 성공을 알리지 않는다.
                if (token != generation) return;
                LoginResult result = null;
                try {
                    result = get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError("로그인이 중단되었습니다. 다시 시도하세요.");
                } catch (CancellationException ex) {
                    showError("로그인이 취소되었습니다. 다시 시도하세요.");
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof StorageException) {
                        showError("사용자 정보를 불러올 수 없습니다. 잠시 후 다시 시도하세요.");
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
                    passwordField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        loginButton.setText(value ? "확인 중..." : "로그인");
        updateControls();
    }

    private void updateControls() {
        boolean enabled = !busy && !authenticated;
        loginIdField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
    }

    @Override
    public void removeNotify() {
        // 진행 중 작업은 finally에서 암호 배열을 지울 때까지 완료시키고, 이전 완료 알림을 무효화한다.
        generation++;
        authenticated = false;
        passwordField.setText("");
        setBusy(false);
        super.removeNotify();
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("로그인 화면은 EDT에서 사용해야 합니다.");
        }
    }
}
