package coderoom.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.user.LoginUser;
import coderoom.features.gameplay.GameSetup;
import coderoom.features.gameplay.GameView;
import coderoom.features.gameplay.InspectResult;
import coderoom.features.gameplay.SubmitResult;
import coderoom.pages.game.GamePage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** B~G의 로직을 검증하지 않는 A 전용 스텁. 실제 A 클래스에 주입해 호출 순서와 스레드를 검사한다. */
final class AIntegrationStubs {
    private AIntegrationStubs() { }

    static void gate(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "작업 대기 시간 초과");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    static final class Host implements AppController.Host {
        final Map<String, JComponent> pages = new HashMap<>();
        String visible;
        BooleanSupplier close;

        @Override
        public void setPage(String name, JComponent page) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            pages.put(name, page);
        }

        @Override
        public void removePage(String name) {
            pages.remove(name);
        }

        @Override
        public void showPage(String name) {
            assertTrue(pages.containsKey(name));
            visible = name;
        }

        @Override
        public void setCloseRequest(BooleanSupplier request) {
            close = request;
        }
    }

    static final class Widgets implements GamePage.Widgets {
        final JPanel room     = new JPanel();
        final JPanel controls = new JPanel();
        GamePage actions;
        GameView rendered;
        InspectResult inspected;
        SubmitResult submitted;
        GamePage.Escape escape;
        GamePage.SaveState state = GamePage.SaveState.NONE;
        String message           = "";
        boolean busy             = false;
        int renders              = 0;

        @Override
        public JComponent room() { return room; }
        @Override
        public JComponent controls() { return controls; }
        @Override
        public void bind(GamePage page) { actions = page; }
        @Override
        public void render(GameView view) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            rendered = view;
            renders++;
        }
        @Override
        public void inspected(InspectResult result) { inspected = result; }
        @Override
        public void submitted(SubmitResult result) { submitted = result; }
        @Override
        public void status(String value) { message = value; }
        @Override
        public void busy(boolean value) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            busy = value;
        }
        @Override
        public void result(GamePage.Escape result, GamePage.SaveState value) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            escape = result;
            state  = value;
        }
    }

    static final class Game implements GamePage.GamePort {
        final GameView view           = new GameView();
        final GameSetup setup         = new GameSetup();
        final ClearRecord record      = new ClearRecord();
        final InspectResult inspected = new InspectResult();
        final SubmitResult submitted  = new SubmitResult();
        final List<ClearRecord> saved = new ArrayList<>();
        CountDownLatch loadGate       = new CountDownLatch(0);
        CountDownLatch saveGate       = new CountDownLatch(0);
        final CountDownLatch loading  = new CountDownLatch(1);
        final CountDownLatch saving   = new CountDownLatch(1);
        boolean loadFailure           = false;
        boolean saveFailure           = false;
        boolean escapeAllowed         = true;
        int starts                    = 0;
        int submissions               = 0;
        int selections                = 0;
        LoginUser user;
        long mapId;

        @Override
        public GameSetup loadMap(long id) {
            assertFalse(SwingUtilities.isEventDispatchThread());
            mapId = id;
            loading.countDown();
            gate(loadGate);
            if (loadFailure) {
                throw new IllegalStateException("조회 실패 스텁");
            }
            return setup;
        }
        @Override
        public GameView start(LoginUser value, GameSetup data, long now) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            assertTrue(data == setup);
            user = value;
            starts++;
            return view;
        }
        @Override
        public InspectResult inspect(RoomObjectId object) { return inspected; }
        @Override
        public SubmitResult submit(RoomObjectId object, String answer) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            submissions++;
            return submitted;
        }
        @Override
        public GameView selectItem(ItemId item) {
            selections++;
            return view;
        }
        @Override
        public GamePage.Escape useKey(long now) {
            return new GamePage.Escape(view, escapeAllowed ? record : null, "열쇠 선택 필요");
        }
        @Override
        public void saveClear(ClearRecord value) {
            assertFalse(SwingUtilities.isEventDispatchThread());
            saved.add(value);
            saving.countDown();
            gate(saveGate);
            if (saveFailure) {
                throw new IllegalStateException("저장 실패 스텁");
            }
        }
        @Override
        public GameView view(long now) {
            assertTrue(SwingUtilities.isEventDispatchThread());
            return view;
        }
    }

    static final class Screens implements AppController.Screens {
        Consumer<LoginUser> login;
        LongConsumer open;
        Runnable logout;
        Runnable back;
        Runnable restart;
        LoginUser mapUser;
        Game game;
        Widgets widgets;
        GamePage page;
        int mapLoads = 0;

        @Override
        public JComponent login(Consumer<LoginUser> callback) {
            login = callback;
            return new JPanel();
        }
        @Override
        public JComponent maps(LoginUser user, LongConsumer callback, Runnable exit) {
            mapUser = user;
            open    = callback;
            logout  = exit;
            mapLoads++;
            return new JPanel();
        }
        @Override
        public GamePage game(LoginUser user, long mapId, Runnable onBack, Runnable onRestart) {
            game    = new Game();
            widgets = new Widgets();
            page    = new GamePage(game, widgets);
            back    = onBack;
            restart = onRestart;
            return page;
        }
    }
}
