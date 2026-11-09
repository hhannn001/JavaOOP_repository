package coderoom.app;

import coderoom.entities.user.LoginUser;
import coderoom.pages.game.GamePage;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * 담당: A
 * 역할: 로그인·대시보드·맵·게임 화면 전환과 세션 연결.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class AppController {
    // [필드·연결]
    // coderoom.entities.user.LoginUser와 MainFrame, pages의 화면들을 연결한다.
    // LoginPage에는 Consumer<LoginUser>, MapSelectionPage에는 LongConsumer 콜백을 전달한다.

    // [구현 예정]
    // onLogin(user), openMaps(), backToDashboard(), openMap(mapId), backToMaps(), logout(): 화면 전환을 조율한다.
    // 새 게임마다 GameFacade와 GamePage를 새로 만들고 이전 GamePage를 정리한다.
    // 로그아웃 시 이전 계정의 화면 데이터와 게임 세션을 제거한다.
    // 저장 작업 중 전환을 막고, 기록 미저장 상태의 이탈은 사용자 확인 후 처리한다.

    /** 실제 창과 헤드리스 테스트 드라이버가 공유하는 화면 전환 계약이다. */
    public interface Host {
        void setPage(String name, JComponent page);
        void removePage(String name);
        void showPage(String name);
        void setCloseRequest(BooleanSupplier closeRequest);
    }

    /** A 대시보드·B/C 화면과 게임별 새 인스턴스를 제공한다. 계정을 전역 변수로 공유하지 않는다. */
    public interface Screens {
        JComponent login(Consumer<LoginUser> onLogin);
        JComponent dashboard(LoginUser user, Runnable openMaps, Runnable logout);
        JComponent maps(LoginUser user, LongConsumer openMap, Runnable dashboard);
        GamePage game(LoginUser user, long mapId, Runnable back, Runnable restart);
    }

    private final Host host;
    private final Screens screens;
    private final BooleanSupplier confirmDiscard;
    private LoginUser currentUser;
    private GamePage gamePage;
    private long currentMap;
    private Object screenToken;

    /** 화면 생성은 외부 조립 지점에 맡기고 이 클래스는 전환과 수명만 관리한다. */
    public AppController(Host host, Screens screens, BooleanSupplier confirmDiscard) {
        requireEdt();
        this.host           = Objects.requireNonNull(host);
        this.screens        = Objects.requireNonNull(screens);
        this.confirmDiscard = Objects.requireNonNull(confirmDiscard);
        host.setCloseRequest(this::close);
    }

    public void start() {
        requireEdt();
        if (currentUser != null || gamePage != null) {
            throw new IllegalStateException("이미 실행 중인 앱입니다.");
        }
        showLogin();
    }

    /** 로그인 완료 시 이전 폼을 제거하고 현재 사용자의 대시보드를 연다. */
    public void onLogin(LoginUser user) {
        requireEdt();
        Objects.requireNonNull(user);
        if (currentUser != null) {
            return;
        }
        currentUser = user;
        host.removePage("login");
        showDashboard();
    }

    /** 대시보드에서 현재 사용자 세션을 유지한 채 맵 선택 화면으로 이동한다. */
    public void openMaps() {
        requireEdt();
        if (currentUser == null || gamePage != null) {
            return;
        }
        host.removePage("dashboard");
        showMaps();
    }

    /** 맵 선택 화면에서 사용자 정보를 다시 확인하도록 대시보드를 새로 만든다. */
    public void backToDashboard() {
        requireEdt();
        if (currentUser == null || gamePage != null) {
            return;
        }
        host.removePage("maps");
        showDashboard();
    }

    public void openMap(long mapId) {
        requireEdt();
        if (currentUser == null || !leaveGame()) {
            return;
        }
        currentMap = mapId;
        Object token = new Object();
        screenToken = token;
        gamePage = screens.game(currentUser, mapId, () -> {
            if (screenToken == token) {
                backToMaps();
            }
        }, () -> {
            if (screenToken == token) {
                restart();
            }
        });
        host.removePage("maps");
        host.setPage("game", gamePage);
        host.showPage("game");
        gamePage.load(currentUser, mapId);
    }

    /** 복귀 시 C의 화면을 새로 만들어 저장된 기록을 다시 조회하도록 한다. */
    public void backToMaps() {
        requireEdt();
        if (currentUser != null && leaveGame()) {
            showMaps();
        }
    }

    public void restart() {
        requireEdt();
        if (gamePage != null) {
            openMap(currentMap);
        }
    }

    /** 로그아웃 시 이전 사용자·화면·타이머 참조를 제거한다. */
    public void logout() {
        requireEdt();
        if (!leaveGame()) {
            return;
        }
        currentUser = null;
        host.removePage("dashboard");
        host.removePage("maps");
        showLogin();
    }

    public boolean close() {
        requireEdt();
        if (!leaveGame()) {
            return false;
        }
        currentUser = null;
        screenToken = null;
        host.removePage("login");
        host.removePage("dashboard");
        host.removePage("maps");
        return true;
    }

    private void showLogin() {
        // 폐기된 로그인 폼에서 늦게 도착한 콜백은 화면 세대를 비교해 무시한다.
        Object token = new Object();
        screenToken = token;
        host.setPage("login", screens.login(user -> {
            if (screenToken == token) {
                onLogin(user);
            }
        }));
        host.showPage("login");
    }

    private void showMaps() {
        Object token = new Object();
        screenToken = token;
        LoginUser user = currentUser;
        host.setPage("maps", screens.maps(user, mapId -> {
            if (screenToken == token && currentUser == user && gamePage == null) {
                openMap(mapId);
            }
        }, () -> {
            if (screenToken == token && currentUser == user && gamePage == null) {
                backToDashboard();
            }
        }));
        host.showPage("maps");
    }

    private void showDashboard() {
        Object token   = new Object();
        screenToken    = token;
        LoginUser user = currentUser;
        host.setPage("dashboard", screens.dashboard(user, () -> {
            if (screenToken == token && currentUser == user && gamePage == null) {
                openMaps();
            }
        }, () -> {
            if (screenToken == token && currentUser == user && gamePage == null) {
                logout();
            }
        }));
        host.showPage("dashboard");
    }

    private boolean leaveGame() {
        if (gamePage == null) {
            return true;
        }
        if (!gamePage.canLeave(confirmDiscard)) {
            return false;
        }
        gamePage.disposeGame();
        gamePage = null;
        host.removePage("game");
        return true;
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("화면 전환은 EDT에서 실행해야 합니다.");
        }
    }

}
