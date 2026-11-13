package coderoom.app;

import coderoom.entities.user.UserAccount;
import coderoom.entities.user.UserRepository;
import coderoom.features.auth.LoginService;
import coderoom.features.auth.PasswordVerifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * 담당: A
 * 역할: 실행 진입점과 객체 조립.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class Main {
    // [연결]
    // coderoom.shared.db.Database와 각 entities의 Jdbc...Repository를 생성한다.
    // Repository를 features의 서비스 생성자에 전달하고 AppController와 MainFrame을 연결한다.
    // 구체 JDBC 구현은 이 조립 지점에서만 직접 생성한다.

    // [구현 예정]
    // main(String[] args): DB 초기화 완료 후 SwingUtilities.invokeLater로 화면을 생성한다.
    // 초기화 실패 시 오류를 알리고 정상 실행으로 진행하지 않는다.

    /** G의 DB 초기화와 실제 서비스 조립을 연결할 경계. 테스트 드라이버가 스텁을 주입한다. */
    public interface Bootstrap {
        void initialize();
        AppController createController();
    }

    /** 메모리 시연 계정으로 실제 인증 후 A의 화면 전환을 확인한다. DB 연동은 별도다. */
    public static void main(String[] args) {
        // 해시 생성은 EDT 밖에서 수행한다. 시연 계정은 실행 중 메모리에만 보관한다.
        LoginService loginService = createDemoLoginService();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setTitle("CODE ROOM · 시연");
            AppController controller = new AppController(frame, new AppScreens(loginService), () -> true);
            controller.start();
            frame.setVisible(true);
        });
    }

    /** G의 DB가 준비되면 Main의 조립 지점에서 실제 JdbcUserRepository로 교체한다. */
    static LoginService createDemoLoginService() {
        PasswordVerifier verifier = new PasswordVerifier();
        Map<String, UserAccount> accounts = new HashMap<>();
        char[] demoPassword = "test-only".toCharArray();
        char[] testPassword = "test1234".toCharArray();
        try {
            for (long id = 1; id <= 3; id++) {
                String loginId = "demo" + id;
                accounts.put(loginId, new UserAccount(id, loginId, verifier.hash(demoPassword)));
            }
            // 요청한 로컬 테스트 계정은 기존 시연 계정과 다른 ID로 추가한다.
            accounts.put("test", new UserAccount(4L, "test", verifier.hash(testPassword)));
        } finally {
            Arrays.fill(demoPassword, '\0');
            Arrays.fill(testPassword, '\0');
        }
        Map<String, UserAccount> readOnlyAccounts = Map.copyOf(accounts);
        UserRepository users = id -> Optional.ofNullable(readOnlyAccounts.get(id));
        return new LoginService(users, verifier);
    }

    /** DB 준비를 EDT 밖에서 완료한 경우에만 화면을 만든다. 실패 시 정상 흐름을 시작하지 않는다. */
    public static SwingWorker<Void, Void> launch(Bootstrap bootstrap, Consumer<Throwable> onFailure) {
        Objects.requireNonNull(bootstrap);
        Objects.requireNonNull(onFailure);
        SwingWorker<Void, Void> startup = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                bootstrap.initialize();
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    get();
                    bootstrap.createController().start();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(exception);
                } catch (ExecutionException exception) {
                    onFailure.accept(exception.getCause());
                } catch (RuntimeException exception) {
                    onFailure.accept(exception);
                }
            }
        };
        startup.execute();
        return startup;
    }

}
