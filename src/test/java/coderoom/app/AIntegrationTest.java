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
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** A의 운영 컨트롤러와 GamePage를 실행하는 통합 드라이버. B~G 기능 자체의 성공을 뜻하지 않는다. */
class AIntegrationTest {
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
        edt(() -> { page.load(new LoginUser(), 1); return null; });
        await(() -> !widgets.busy);
        assertTrue(edt(page::isTimerRunning));
    }

    @Test
    void loginEscapeReturnLogoutAndDifferentAccount() throws Exception {
        AIntegrationStubs.Host host       = new AIntegrationStubs.Host();
        AIntegrationStubs.Screens screens = new AIntegrationStubs.Screens();
        AppController controller          = edt(() -> new AppController(host, screens, () -> true));
        LoginUser first                   = new LoginUser();
        LoginUser second                  = new LoginUser();
        edt(() -> {
            controller.start();
            assertEquals("login", host.visible);
            screens.login.accept(first);
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
            screens.logout.run();
            assertEquals("login", host.visible);
            assertEquals(1, host.pages.size());
            screens.login.accept(second);
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
            page.load(new LoginUser(), 8);
            page.load(new LoginUser(), 9);
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
        edt(() -> { page.load(new LoginUser(), 1); return null; });
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
        edt(() -> { page.load(new LoginUser(), 1); return null; });
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
            screens.login.accept(new LoginUser());
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
            screens.login.accept(new LoginUser());
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
            old.accept(new LoginUser());
            screens.logout.run();
            old.accept(new LoginUser());
            assertEquals("login", host.visible);
            assertTrue(host.close.getAsBoolean());
            screens.login.accept(new LoginUser());
            assertTrue(host.pages.isEmpty());
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
        assertThrows(IllegalStateException.class, () -> page.load(new LoginUser(), 1));
        assertThrows(IllegalStateException.class, page::disposeGame);
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
