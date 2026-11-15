package coderoom.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.user.LoginUser;
import coderoom.pages.game.GamePage;
import coderoom.widgets.room.RoomPanel;
import coderoom.widgets.inventory.InventoryPanel;
import coderoom.widgets.clue.CluePanel;
import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JTextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import coderoom.shared.db.Database;
import coderoom.entities.user.JdbcUserRepository;
import coderoom.features.auth.LoginService;
import coderoom.features.auth.PasswordVerifier;

/** A의 운영 컨트롤러와 GamePage를 실행하는 통합 드라이버. B~G 기능 자체의 성공을 뜻하지 않는다. */
class AIntegrationTest {
    @TempDir Path directory;
    private Database database;

    @BeforeEach
    void initializeDatabase() {
        database = new Database(directory.resolve("app.db").toString());
        database.initialize();
    }

    private LoginService loginService() {
        return new LoginService(new JdbcUserRepository(database), new PasswordVerifier());
    }

    private final List<GamePage> cleanup = new ArrayList<>();

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    // SwingWorker.done()까지 기다리되 EDT를 블로킹하지 않는다.
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!edt(condition::getAsBoolean)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("EDT 결과 대기 시간 초과");
            }
            Thread.sleep(10);
        }
    }

    @AfterEach
    void disposeTimers() throws Exception {
        edt(() -> {
            cleanup.forEach(GamePage::disposeGame);
            return null;
        });
    }

    private GamePage page(AIntegrationStubs.Game game, AIntegrationStubs.Widgets widgets) throws Exception {
        GamePage page = edt(() -> new GamePage(game, widgets));
        cleanup.add(page);
        return page;
    }

    private void start(GamePage page, AIntegrationStubs.Widgets widgets) throws Exception {
        edt(() -> { page.load(new LoginUser(1L, "test"), 1); return null; });
        await(() -> !widgets.busy);
        assertTrue(edt(page::isTimerRunning));
    }

    @Test
    void loginEscapeReturnLogoutAndDifferentAccount() throws Exception {
        AIntegrationStubs.Host host       = new AIntegrationStubs.Host();
        AIntegrationStubs.Screens screens = new AIntegrationStubs.Screens();
        AppController controller          = edt(() -> new AppController(host, screens, () -> true));
        LoginUser first                   = new LoginUser(1L, "test");
        LoginUser second                  = new LoginUser(2L, "other");
        edt(() -> {
            controller.start();
            assertEquals("login", host.visible);
            screens.login.accept(first);
            assertEquals("dashboard", host.visible);
            screens.openMaps.run();
            assertSame(first, screens.mapUser);
            screens.open.accept(1);
            cleanup.add(screens.page);
            assertEquals("game", host.visible);
            return null;
        });
        await(() -> !screens.widgets.busy);
        GamePage old = screens.page;
        edt(() -> {
            old.inspect(null);
            old.submit(null, "스텁 입력");
            old.selectItem(null);
            assertSame(screens.game.inspected, screens.widgets.inspected);
            assertSame(screens.game.submitted, screens.widgets.submitted);
            assertSame(screens.game.view, screens.widgets.rendered);
            old.useKey();
            return null;
        });
        await(() -> screens.widgets.state == GamePage.SaveState.SAVED);
        edt(() -> {
            screens.back.run();
            assertEquals("maps", host.visible);
            assertEquals(2, screens.mapLoads);
            assertFalse(old.isTimerRunning());
            screens.dashboard.run();
            assertEquals("dashboard", host.visible);
            screens.logout.run();
            assertEquals("login", host.visible);
            assertEquals(1, host.pages.size());
            screens.login.accept(second);
            screens.openMaps.run();
            assertSame(second, screens.mapUser);
            screens.open.accept(1);
            cleanup.add(screens.page);
            assertNotSame(old, screens.page);
            return null;
        });
        await(() -> !screens.widgets.busy);
        assertSame(second, screens.game.user);
        edt(() -> { assertTrue(host.close.getAsBoolean()); return null; });
        assertTrue(host.pages.isEmpty());
    }

    @Test
    void loadingBlocksNavigationAndDuplicateStart() throws Exception {
        AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
        AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
        game.loadGate = new CountDownLatch(1);
        GamePage page = page(game, widgets);
        edt(() -> {
            page.load(new LoginUser(1L, "test"), 8);
            page.load(new LoginUser(1L, "test"), 9);
            assertFalse(page.canLeave(() -> true));
            page.submit(null, "ignored");
            return null;
        });
        AIntegrationStubs.gate(game.loading);
        game.loadGate.countDown();
        await(() -> !widgets.busy);
        assertEquals(8, game.mapId);
        assertEquals(1, game.starts);
        assertEquals(0, game.submissions);
    }

    @Test
    void failedLoadCanRetry() throws Exception {
        AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
        AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
        game.loadFailure = true;
        GamePage page = page(game, widgets);
        edt(() -> { page.load(new LoginUser(1L, "test"), 1); return null; });
        await(() -> !widgets.busy);
        assertFalse(edt(page::isTimerRunning));
        assertTrue(widgets.message.contains("불러오지 못"));
        game.loadFailure = false;
        start(page, widgets);
        assertEquals(1, game.starts);
    }

    @Test
    void savingBlocksExitAndRetriesTheSameRecord() throws Exception {
        AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
        AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
        game.saveGate    = new CountDownLatch(1);
        game.saveFailure = true;
        GamePage page = page(game, widgets);
        start(page, widgets);
        edt(() -> {
            page.useKey();
            page.useKey();
            page.retrySave();
            assertFalse(page.isTimerRunning());
            assertFalse(page.canLeave(() -> true));
            return null;
        });
        AIntegrationStubs.gate(game.saving);
        game.saveGate.countDown();
        await(() -> widgets.state == GamePage.SaveState.FAILED);
        assertEquals(1, game.saved.size());
        edt(() -> {
            assertFalse(page.canLeave(() -> false));
            assertTrue(page.canLeave(() -> true));
            assertSame(game.record, widgets.escape.record());
            game.saveFailure = false;
            page.retrySave();
            return null;
        });
        await(() -> widgets.state == GamePage.SaveState.SAVED);
        assertEquals(2, game.saved.size());
        assertSame(game.saved.get(0), game.saved.get(1));
        assertTrue(edt(() -> page.canLeave(() -> false)));
    }

    @Test
    void blockedEscapeKeepsTimerAndDoesNotSave() throws Exception {
        AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
        AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
        game.escapeAllowed = false;
        GamePage page = page(game, widgets);
        start(page, widgets);
        edt(() -> { page.useKey(); return null; });
        assertTrue(edt(page::isTimerRunning));
        assertTrue(game.saved.isEmpty());
        assertEquals("열쇠 선택 필요", widgets.message);
    }

    @Test
    void disposedLoadingPageDoesNotStartOnLateCompletion() throws Exception {
        AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
        AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
        game.loadGate = new CountDownLatch(1);
        GamePage page = page(game, widgets);
        edt(() -> { page.load(new LoginUser(1L, "test"), 1); return null; });
        AIntegrationStubs.gate(game.loading);
        edt(() -> { page.disposeGame(); page.disposeGame(); return null; });
        game.loadGate.countDown();
        // SwingWorker의 취소 완료 콜백이 전달된 후에도 화면이 살아나지 않아야 한다.
        Thread.sleep(100);
        edt(() -> {
            assertFalse(page.isTimerRunning());
            assertEquals(0, game.starts);
            assertEquals(0, widgets.renders);
            return null;
        });
    }

    @Test
    void restartDisposesOldTimerAndOldCallbacksCannotNavigate() throws Exception {
        AIntegrationStubs.Host host       = new AIntegrationStubs.Host();
        AIntegrationStubs.Screens screens = new AIntegrationStubs.Screens();
        AppController controller          = edt(() -> new AppController(host, screens, () -> true));
        edt(() -> {
            controller.start();
            screens.login.accept(new LoginUser(1L, "test"));
            screens.openMaps.run();
            screens.open.accept(1);
            cleanup.add(screens.page);
            return null;
        });
        await(() -> !screens.widgets.busy);
        GamePage old     = screens.page;
        Runnable oldBack = screens.back;
        edt(() -> {
            screens.restart.run();
            cleanup.add(screens.page);
            assertFalse(old.isTimerRunning());
            assertNotSame(old, screens.page);
            oldBack.run();
            assertEquals("game", host.visible);
            return null;
        });
        await(() -> !screens.widgets.busy);
        assertTrue(edt(screens.page::isTimerRunning));
    }

    @Test
    void controllerBlocksEveryExitUntilSaveFailureIsConfirmed() throws Exception {
        AIntegrationStubs.Host host       = new AIntegrationStubs.Host();
        AIntegrationStubs.Screens screens = new AIntegrationStubs.Screens();
        boolean[] discard                 = {false};
        AppController controller          = edt(() -> new AppController(host, screens, () -> discard[0]));
        edt(() -> {
            controller.start();
            screens.login.accept(new LoginUser(1L, "test"));
            screens.openMaps.run();
            screens.open.accept(1);
            cleanup.add(screens.page);
            return null;
        });
        await(() -> !screens.widgets.busy);
        edt(() -> {
            screens.game.saveGate    = new CountDownLatch(1);
            screens.game.saveFailure = true;
            screens.page.useKey();
            controller.backToMaps();
            controller.logout();
            controller.restart();
            assertFalse(controller.close());
            assertEquals("game", host.visible);
            return null;
        });
        screens.game.saveGate.countDown();
        await(() -> screens.widgets.state == GamePage.SaveState.FAILED);
        edt(() -> {
            controller.logout();
            assertEquals("game", host.visible);
            discard[0] = true;
            controller.logout();
            assertEquals("login", host.visible);
            assertFalse(screens.page.isTimerRunning());
            return null;
        });
    }

    @Test
    void oldLoginCallbackCannotRestoreLoggedOutUser() throws Exception {
        AIntegrationStubs.Host host       = new AIntegrationStubs.Host();
        AIntegrationStubs.Screens screens = new AIntegrationStubs.Screens();
        AppController controller          = edt(() -> new AppController(host, screens, () -> true));
        edt(() -> {
            controller.start();
            Consumer<LoginUser> old = screens.login;
            old.accept(new LoginUser(1L, "test"));
            screens.logout.run();
            old.accept(new LoginUser(1L, "test"));
            assertEquals("login", host.visible);
            assertTrue(host.close.getAsBoolean());
            screens.login.accept(new LoginUser(1L, "test"));
            assertTrue(host.pages.isEmpty());
            return null;
        });
    }

    @Test
    void discardedDashboardCallbacksCannotChangeTheCurrentScreen() throws Exception {
        AIntegrationStubs.Host host       = new AIntegrationStubs.Host();
        AIntegrationStubs.Screens screens = new AIntegrationStubs.Screens();
        AppController controller          = edt(() -> new AppController(host, screens, () -> true));
        edt(() -> {
            controller.start();
            screens.login.accept(new LoginUser(1L, "test"));
            assertEquals("dashboard", host.visible);
            Runnable oldOpen   = screens.openMaps;
            Runnable oldLogout = screens.logout;
            oldOpen.run();
            assertEquals("maps", host.visible);
            oldLogout.run();
            assertEquals("maps", host.visible);
            screens.dashboard.run();
            assertEquals("dashboard", host.visible);
            oldOpen.run();
            assertEquals("dashboard", host.visible);
            screens.logout.run();
            assertEquals("login", host.visible);
            assertEquals(1, host.pages.size());
            return null;
        });
    }

    @Test
    void bootstrapInitializesOffEdtAndBuildsScreensOnEdt() throws Exception {
        AtomicInteger initialized         = new AtomicInteger();
        AIntegrationStubs.Host host       = new AIntegrationStubs.Host();
        AIntegrationStubs.Screens screens = new AIntegrationStubs.Screens();
        Main.launch(new Main.Bootstrap() {
            @Override
            public void initialize() {
                assertFalse(SwingUtilities.isEventDispatchThread());
                initialized.incrementAndGet();
            }
            @Override
            public AppController createController() {
                assertTrue(SwingUtilities.isEventDispatchThread());
                assertEquals(1, initialized.get());
                return new AppController(host, screens, () -> true);
            }
        }, failure -> { throw new AssertionError(failure); });
        await(() -> "login".equals(host.visible));
    }

    @Test
    void initializationFailureNeverCreatesScreens() throws Exception {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger errors  = new AtomicInteger();
        Main.launch(new Main.Bootstrap() {
            @Override
            public void initialize() { throw new IllegalStateException("DB 실패 스텁"); }
            @Override
            public AppController createController() {
                created.incrementAndGet();
                return null;
            }
        }, failure -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            assertEquals("DB 실패 스텁", failure.getMessage());
            errors.incrementAndGet();
        });
        await(() -> errors.get() == 1);
        assertEquals(0, created.get());
    }

    @Test
    void rejectsUiActionsOutsideEdt() throws Exception {
        AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
        AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
        GamePage page                     = page(game, widgets);
        assertThrows(IllegalStateException.class, () -> page.load(new LoginUser(1L, "test"), 1));
        assertThrows(IllegalStateException.class, page::disposeGame);
    }

    /** 새 로컬 시연 계정이 실제 인증 서비스에서 성공하고 오답은 거부되는지 확인한다. */
    @Test
    void testAccountAuthenticates() {
        var service = loginService();
        char[] password = "test1234".toCharArray();
        var result = service.login("test", password);
        assertTrue(result.success());
        assertEquals(new LoginUser(4L, "test"), result.user());
        assertEquals('\0', password[0]);
        assertFalse(service.login("test", "wrong".toCharArray()).success());
    }

    @Test
    void databaseScreensMountInventoryAndClueWidgets() throws Exception {
        edt(() -> {
            AppScreens screens = Main.createScreens(database);
            GamePage page      = screens.game(new LoginUser(4L, "test"), 1, () -> { }, () -> { });
            cleanup.add(page);
            InventoryPanel inventory = (InventoryPanel) findNamed(page, "gameInventory");
            CluePanel clue           = (CluePanel) findNamed(page, "gameClue");
            assertTrue(inventory.isVisible());
            assertEquals(5, inventory.getComponentCount());
            for (Component component : inventory.getComponents()) {
                JButton slot = (JButton) component;
                assertFalse(slot.isEnabled());
                slot.doClick();
            }
            assertFalse(clue.isVisible());
            clue.openClue();
            assertTrue(clue.isVisible());
            findButton(clue, "접어두기  X").doClick();
            assertFalse(clue.isVisible());
            return null;
        });
    }

    /** 임시 대시보드를 포함한 화면 흐름과 맵 복귀를 확인한다. */
    @Test
    void databaseScreensNavigateAndLoadGame() throws Exception {
        AIntegrationStubs.Host host = new AIntegrationStubs.Host();
        AppController controller = edt(() -> new AppController(host, Main.createScreens(database), () -> true));
        edt(() -> {
            controller.start();
            assertEquals("login", host.visible);
            submitLogin(host.pages.get("login"));
            return null;
        });
        await(() -> "dashboard".equals(host.visible));
        edt(() -> {
            assertEquals("dashboard", host.visible);
            DashboardPage dashboard = (DashboardPage) host.pages.get("dashboard");
            assertEquals(new Color(8, 19, 19), dashboard.getBackground());
            assertEquals(4, countButtons(dashboard));
            assertFalse(findButton(dashboard, "방 선택하기").isVisible());
            dashboard.getActionMap().get("dismissIntro").actionPerformed(null);
            assertFalse(findButton(dashboard, "방 선택하기").isVisible());
            // 반복 Enter 입력은 진행 중인 애니메이션을 재시작하지 않는다.
            dashboard.getActionMap().get("dismissIntro").actionPerformed(null);
            return null;
        });
        await(() -> findButton(host.pages.get("dashboard"), "방 선택하기").isVisible());
        edt(() -> {
            DashboardPage dashboard = (DashboardPage) host.pages.get("dashboard");
            findButton(dashboard, "방 선택하기").doClick();
            assertEquals("maps", host.visible);
            assertEquals(0, countMessage(host.pages.get("maps")));
            return null;
        });
        await(() -> hasButton(host.pages.get("maps"), "입장하기"));
        edt(() -> {
            assertTrue(findButton(host.pages.get("maps"), "입장하기").isEnabled());
            assertFalse(findButton(host.pages.get("maps"), "준비 중").isEnabled());
            findButton(host.pages.get("maps"), "입장하기").doClick();
            assertEquals("game", host.visible);
            cleanup.add((GamePage) host.pages.get("game"));
            return null;
        });
        await(() -> ((GamePage) host.pages.get("game")).isTimerRunning());
        edt(() -> {
            assertEquals(0, countMessage(host.pages.get("game")));
            findButton(host.pages.get("game"), "설정");
            RoomPanel room = findRoomPanel(host.pages.get("game"));
            room.setSize(1586, 992);
            // D의 서랍 클릭 영역이 실제 A 게임 화면에서도 이벤트를 받는지 확인한다.
            long when = System.currentTimeMillis();
            room.dispatchEvent(new MouseEvent(room, MouseEvent.MOUSE_MOVED, when, 0,
                    600, 600, 0, false));
            assertEquals("서랍장", room.getToolTipText());
            room.dispatchEvent(new MouseEvent(room, MouseEvent.MOUSE_PRESSED, when, 0,
                    600, 600, 1, false, MouseEvent.BUTTON1));
            room.dispatchEvent(new MouseEvent(room, MouseEvent.MOUSE_RELEASED, when, 0,
                    600, 600, 1, false, MouseEvent.BUTTON1));
            assertThrows(IllegalArgumentException.class,
                    () -> findButton(host.pages.get("game"), "맵 선택으로"));
            assertThrows(IllegalArgumentException.class,
                    () -> findButton(host.pages.get("game"), "화면 다시 열기"));
            return null;
        });
        edt(() -> {
            controller.backToMaps();
            assertEquals("maps", host.visible);
            findButton(host.pages.get("maps"), "대시보드로 돌아가기").doClick();
            assertEquals("dashboard", host.visible);
            findButton(host.pages.get("dashboard"), "방 선택하기").doClick();
            assertEquals("maps", host.visible);
            findButton(host.pages.get("maps"), "대시보드로 돌아가기").doClick();
            controller.logout();
            assertEquals("login", host.visible);
            return null;
        });
    }

    @Test
    void databaseGameWidgetsSubmitBothPuzzlesAndSaveResult() throws Exception {
        GamePage page = edt(() -> {
            GamePage created = Main.createScreens(database).game(new LoginUser(4, "test"), 1, () -> {}, () -> {});
            cleanup.add(created);
            created.load(new LoginUser(4, "test"), 1);
            return created;
        });
        await(page::isTimerRunning);
        edt(() -> {
            findButton(page, "메모 1").doClick();
            assertTrue(findNamed(page, "gameClue").isVisible());
            page.inspect(coderoom.entities.game.RoomObjectId.DRAWER);
            java.util.List<JTextField> fields = new ArrayList<>();
            collectFields(page, fields);
            assertEquals(1, fields.size());
            fields.get(0).setText("0000");
            findButton(page, "서랍 열기  →").doClick();
            assertFalse(hasButton(page, "메모 2"));
            fields.get(0).setText("2312");
            findButton(page, "서랍 열기  →").doClick();
            findButton(page, "메모 2").doClick();
            page.inspect(coderoom.entities.game.RoomObjectId.WALL_DIAL);
            fields.clear();
            collectFields(page, fields);
            assertEquals(9, fields.size());
            String answer = "423312133";
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setText(answer.substring(i, i + 1));
            }
            findButton(page, "다이얼 확인  →").doClick();
            findButton(page, "열쇠").doClick();
            page.useKey();
            assertFalse(page.isTimerRunning());
            return null;
        });
        await(() -> hasLabel(page, "기록 저장 완료"));
        var records = new coderoom.entities.record.JdbcClearRecordRepository(database).findByUserId(4);
        assertEquals(1, records.size());
        assertTrue(records.get(0).getBestSeconds() >= 0);
    }

    private static void collectFields(Component component, List<JTextField> fields) {
        if (component instanceof JTextField field) {
            fields.add(field);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectFields(child, fields);
            }
        }
    }

    private static boolean hasLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (hasLabel(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasButton(Component root, String title) {
        try {
            return findButton(root, title).isShowing() || findButton(root, title).isVisible();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static int countMessage(Component root) {
        int count = root instanceof JLabel label && "임시 진입점 구현".equals(label.getText()) ? 1 : 0;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                count += countMessage(child);
            }
        }
        return count;
    }

    /** B 로그인 폼에 시연 계정을 입력하고 실제 비동기 로그인 경로를 실행한다. */
    private static void submitLogin(Component root) {
        ((JTextField) findNamed(root, "loginId")).setText("test");
        ((JPasswordField) findNamed(root, "password")).setText("test1234");
        findButton(root, "로그인").doClick();
    }

    private static Component findNamed(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                try {
                    return findNamed(child, name);
                } catch (IllegalArgumentException ignored) {
                    // 다음 자식 컴포넌트에서 이름이 지정된 입력란을 찾는다.
                }
            }
        }
        throw new IllegalArgumentException("컴포넌트 없음: " + name);
    }

    private static int countButtons(Component root) {
        int count = root instanceof JButton ? 1 : 0;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                count += countButtons(child);
            }
        }
        return count;
    }

    private static JButton findButton(Component root, String title) {
        if (root instanceof JButton button && title.equals(button.getText())) {
            return button;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                try {
                    return findButton(child, title);
                } catch (IllegalArgumentException ignored) {
                    // 같은 화면의 다음 컴포넌트에서 버튼을 계속 찾는다.
                }
            }
        }
        throw new IllegalArgumentException("버튼 없음: " + title);
    }

    private static RoomPanel findRoomPanel(Component root) {
        if (root instanceof RoomPanel room) {
            return room;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                try {
                    return findRoomPanel(child);
                } catch (IllegalArgumentException ignored) {
                    // 다음 자식 컴포넌트에서 D의 방 화면을 찾는다.
                }
            }
        }
        throw new IllegalArgumentException("방 화면 없음");
    }

    @Test
    void mainOpensTheDemoFrame() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "실제 창은 디스플레이 환경에서 검증");
        String previous = System.getProperty("coderoom.db");
        try {
            System.setProperty("coderoom.db", directory.resolve("app.db").toString());
            Main.main(new String[0]);
        } finally {
            if (previous == null) {
                System.clearProperty("coderoom.db");
            } else {
                System.setProperty("coderoom.db", previous);
            }
        }
        await(() -> java.util.Arrays.stream(Window.getWindows())
                .anyMatch(window -> window instanceof MainFrame && window.isShowing()));
        MainFrame frame = edt(() -> {
            MainFrame found = null;
            for (Window window : Window.getWindows()) {
                if (window instanceof MainFrame candidate && candidate.isShowing()) {
                    found = candidate;
                    break;
                }
            }
            assertTrue(found != null, "시연 창이 화면에 표시되어야 합니다.");
            assertEquals(256, found.getIconImage().getWidth(null));
            submitLogin(found);
            return found;
        });
        await(() -> {
            try {
                return findButton(frame, "방 선택하기").isShowing()
                        || findButton(frame, "방 선택하기").getParent().isShowing();
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        });
        edt(() -> {
            JButton dashboardMaps = findButton(frame, "방 선택하기");
            DashboardPage dashboard = (DashboardPage) dashboardMaps.getParent();
            dashboard.getActionMap().get("dismissIntro").actionPerformed(null);
            assertFalse(dashboardMaps.isShowing());
            return null;
        });
        await(() -> findButton(frame, "방 선택하기").isShowing());
        edt(() -> {
            JButton dashboardMaps = findButton(frame, "방 선택하기");
            assertTrue(dashboardMaps.isShowing(), "대시보드의 맵 이동 버튼이 보여야 합니다.");
            dashboardMaps.doClick();
            return null;
        });
        await(() -> hasButton(frame, "입장하기"));
        edt(() -> {
            JButton enter = findButton(frame, "입장하기");
            assertTrue(enter.isShowing(), "맵 화면의 이동 버튼이 실제 창에 표시되어야 합니다.");
            enter.doClick();
            return null;
        });
        await(() -> hasButton(frame, "메모 1"));
        edt(() -> {
            frame.validate();
            JButton settings = findButton(frame, "설정");
            assertTrue(settings.isShowing(), "설정 버튼이 게임 사진 위에 표시되어야 합니다.");
            assertFalse(settings.isOpaque(), "설정 아이콘 뒤에 사각 배경이 없어야 합니다.");
            assertTrue(settings.getWidth() > 0);
            assertEquals(settings.getWidth(), settings.getHeight());
            assertTrue(SwingUtilities.convertPoint(settings, 0, 0, frame).x > frame.getWidth() / 2,
                    "설정 버튼은 게임 화면 우상단에 있어야 합니다.");
            // 모달 창의 이벤트 루프에서 맨 아래 나가기 버튼을 눌러 화면 복귀를 확인한다.
            Timer closeMenu = new Timer(100, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog && dialog.isShowing()) {
                        ((Timer) event.getSource()).stop();
                        findButton(dialog, "나가기").doClick();
                    }
                }
            });
            closeMenu.start();
            settings.doClick();
            return null;
        });
        await(() -> hasButton(frame, "입장하기"));
        edt(() -> {
            assertTrue(findButton(frame, "입장하기").isShowing());
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            assertFalse(frame.isDisplayable());
            return null;
        });
    }

    @Test
    void realFrameReplacesCardsAndHonorsCloseGuard() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "헤드리스 CI에서는 Host 스텁으로 검증");
        edt(() -> {
            MainFrame frame = new MainFrame();
            try {
                JPanel old         = new JPanel();
                JPanel replacement = new JPanel();
                frame.setPage("login", old);
                frame.setPage("login", replacement);
                frame.showPage("login");
                assertNull(old.getParent());
                assertEquals(1, frame.getContentPane().getComponentCount());
                assertThrows(IllegalArgumentException.class, () -> frame.showPage("missing"));
                frame.setVisible(true);
                frame.setCloseRequest(() -> false);
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                assertTrue(frame.isDisplayable());
                frame.setCloseRequest(() -> true);
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                assertFalse(frame.isDisplayable());
            } finally {
                frame.dispose();
            }
            return null;
        });
    }
}
