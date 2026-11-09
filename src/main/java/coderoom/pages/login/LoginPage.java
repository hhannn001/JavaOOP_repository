package coderoom.pages.login;

import coderoom.entities.user.LoginUser;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 담당: B
 * 역할: 로그인 폼
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class LoginPage extends JPanel {
    // [연결]
    // coderoom.features.auth.LoginService를 생성자로 받는다.
    // 성공 시 상위에서 받은 Consumer<LoginUser>를 호출한다.

    // [구현 예정]
    // ID·비밀번호 입력과 빈 입력 안내, showError(message)를 구현한다.
    // 로그인·해시 검증은 SwingWorker에서 수행하고 done에서 UI를 갱신한다.
    // 작업 중 중복 제출을 막고 비밀번호 배열은 사용 후 지운다.

    // [경계]
    // app.AppController나 다른 pages 슬라이스를 직접 import하지 않는다.

    /** 실제 인증 서비스가 연결되기 전 화면 이동만 확인하는 최소 진입점이다. */
    public LoginPage(Consumer<LoginUser> onLogin) {
        Objects.requireNonNull(onLogin);
        setLayout(new BorderLayout());
        add(new JLabel("임시 진입점 구현", SwingConstants.CENTER), BorderLayout.CENTER);
        JButton enter = new JButton("맵 선택 화면으로 이동");
        // 임시 사용자로 콜백만 전달하며 인증 성공 여부는 검사하지 않는다.
        enter.addActionListener(event -> onLogin.accept(new LoginUser()));
        add(enter, BorderLayout.SOUTH);
    }
}
