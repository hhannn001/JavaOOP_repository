package coderoom.app;

import coderoom.entities.user.JdbcUserRepository;
import coderoom.entities.map.JdbcMapRepository;
import coderoom.entities.puzzle.JdbcPuzzleRepository;
import coderoom.entities.record.JdbcClearRecordRepository;
import coderoom.features.gameplay.GameFacade;
import coderoom.features.mapcatalog.MapCatalogService;
import coderoom.shared.db.Database;
import javax.swing.JOptionPane;
import coderoom.features.auth.LoginService;
import coderoom.features.auth.PasswordVerifier;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;

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

    // DB 초기화 완료 후 EDT에서 화면을 생성한다.
    // 초기화 실패 시 오류를 알리고 정상 실행으로 진행하지 않는다.

    /** G의 DB 초기화와 실제 서비스 조립을 연결할 경계. 테스트 드라이버가 스텁을 주입한다. */
    public interface Bootstrap {
        void initialize();
        AppController createController();
    }

    /** DB 초기화가 성공한 뒤에만 창을 연다. --init-db는 데이터 준비만 수행한다. */
    public static void main(String[] args) {
        Database database = new Database(System.getProperty("coderoom.db", "coderoom.db"));
        if (args.length == 1 && "--init-db".equals(args[0])) {
            database.initialize();
            System.out.println("DB 초기화 완료");
            return;
        }
        SwingUtilities.invokeLater(() -> launch(new Bootstrap() {
            @Override public void initialize() { database.initialize(); }
            @Override public AppController createController() {
                MainFrame frame = new MainFrame();
                AppController controller = new AppController(frame, createScreens(database),
                        () -> JOptionPane.showConfirmDialog(frame,
                                "기록이 저장되지 않았습니다. 나가시겠습니까?", "기록 저장 확인",
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
                frame.setVisible(true);
                return controller;
            }
        }, failure -> {
            System.err.println("DB 초기화 실패: " + failure.getMessage());
            JOptionPane.showMessageDialog(null, "데이터베이스를 준비하지 못했습니다. DB 경로와 접근 권한을 확인하세요.",
                    "CODE ROOM 시작 실패", JOptionPane.ERROR_MESSAGE);
        }));
    }

    /** 구체 JDBC 저장소는 이 조립 지점에서만 생성한다. */
    static AppScreens createScreens(Database database) {
        JdbcUserRepository users          = new JdbcUserRepository(database);
        JdbcMapRepository maps            = new JdbcMapRepository(database);
        JdbcPuzzleRepository puzzles      = new JdbcPuzzleRepository(database);
        JdbcClearRecordRepository records = new JdbcClearRecordRepository(database);
        return new AppScreens(new LoginService(users, new PasswordVerifier()),
                new MapCatalogService(maps, records), () -> new GameFacade(maps, puzzles, records));
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
