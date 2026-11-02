package coderoom.pages.game;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.user.LoginUser;
import coderoom.features.gameplay.GameSetup;
import coderoom.features.gameplay.GameView;
import coderoom.features.gameplay.InspectResult;
import coderoom.features.gameplay.SubmitResult;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * 담당: A
 * 역할: 방 화면과 게임 조작의 연결
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class GamePage extends JPanel {
    // [연결]
    // coderoom.features.gameplay.GameFacade를 생성자로 받는다.
    // widgets의 RoomPanel·InventoryPanel·CluePanel·ResultPanel과 gameplay.ui.AnswerInputPanel을 조립한다.

    // [구현 예정]
    // render(view): 동일한 GameView를 각 위젯에 전달한다.
    // 물건 클릭·암호 제출·아이템 선택·열쇠 사용 콜백을 GameFacade에 연결한다.
    // loadMap은 작업 스레드, start와 게임 상태 변경은 EDT에서 처리한다.
    // 탈출 결과의 불변 ClearRecord를 작업 스레드에서 저장하고 실패 시 재시도를 제공한다.
    // javax.swing.Timer로 경과 시간 표시를 갱신하고 disposeGame에서 정리한다.
    // 작업 중 중복 실행·화면 이동을 막고 결과/저장 실패를 구분해 표시한다.

    // [경계]
    // app.AppController나 다른 pages 슬라이스를 직접 import하지 않는다.

    /** B~G API가 구현되기 전 A가 사용하는 연결 계약. 실제 게임 규칙은 구현하지 않는다. */
    public interface GamePort {
        GameSetup loadMap(long mapId);
        GameView start(LoginUser user, GameSetup setup, long nowNanos);
        InspectResult inspect(RoomObjectId object);
        SubmitResult submit(RoomObjectId object, String answer);
        GameView selectItem(ItemId item);
        Escape useKey(long nowNanos);
        void saveClear(ClearRecord record);
        GameView view(long nowNanos);
    }

    /** record가 없으면 탈출 실패다. 성공 시 동일한 기록을 저장 재시도에도 사용한다. */
    public record Escape(GameView view, ClearRecord record, String message) { }

    /** D/F/G 위젯을 조립하는 어댑터 계약. 테스트에서는 JPanel 스텁으로 제공한다. */
    public interface Widgets {
        JComponent room();
        JComponent controls();
        void bind(GamePage actions);
        void render(GameView view);
        void inspected(InspectResult result);
        void submitted(SubmitResult result);
        void status(String message);
        void busy(boolean busy);
        void result(Escape escape, SaveState state);
    }

    public enum SaveState { NONE, SAVING, SAVED, FAILED }

    private final GamePort game;
    private final Widgets widgets;
    private final Timer timer;
    private SwingWorker<?, ?> worker;
    private Escape escaped;
    private SaveState saveState = SaveState.NONE;
    private boolean started     = false;
    private boolean disposed    = false;

    /** 위젯 내부 대신 배치와 이벤트 연결만 담당한다. 방은 중앙의 가용 영역을 채운다. */
    public GamePage(GamePort game, Widgets widgets) {
        requireEdt();
        this.game    = Objects.requireNonNull(game);
        this.widgets = Objects.requireNonNull(widgets);
        this.timer   = new Timer(250, event -> render(game.view(System.nanoTime())));
        setLayout(new BorderLayout());
        add(widgets.room(), BorderLayout.CENTER);
        add(widgets.controls(), BorderLayout.SOUTH);
        widgets.bind(this);
    }

    /** 조회는 작업 스레드에서, 게임 시작과 UI 갱신은 EDT에서 실행한다. */
    public void load(LoginUser user, long mapId) {
        requireEdt();
        Objects.requireNonNull(user);
        if (disposed || worker != null || started) {
            return;
        }
        widgets.busy(true);
        widgets.status("게임 준비 중");
        SwingWorker<GameSetup, Void> loading = new SwingWorker<>() {
            @Override
            protected GameSetup doInBackground() {
                return game.loadMap(mapId);
            }

            @Override
            protected void done() {
                // 폐기된 화면의 늦은 응답이 새 게임이나 계정 화면에 영향을 주지 않게 한다.
                if (disposed || worker != this) {
                    return;
                }
                worker = null;
                try {
                    GameSetup setup = get();
                    render(game.start(user, setup, System.nanoTime()));
                    started = true;
                    timer.start();
                    widgets.status("");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    widgets.status("게임 준비가 중단되었습니다.");
                } catch (ExecutionException | RuntimeException exception) {
                    widgets.status("게임을 불러오지 못했습니다. 다시 시도해 주세요.");
                } finally {
                    widgets.busy(false);
                }
            }
        };
        worker = loading;
        loading.execute();
    }

    /** 동일한 스냅샷을 위젯 어댑터에 전달한다. 정답 판정은 GamePort의 책임이다. */
    public void render(GameView view) {
        requireEdt();
        if (!disposed) {
            widgets.render(view);
        }
    }

    public void inspect(RoomObjectId object) {
        if (canPlay()) {
            widgets.inspected(game.inspect(object));
        }
    }

    public void submit(RoomObjectId object, String answer) {
        if (canPlay()) {
            widgets.submitted(game.submit(object, answer));
            render(game.view(System.nanoTime()));
        }
    }

    public void selectItem(ItemId item) {
        if (canPlay()) {
            render(game.selectItem(item));
        }
    }

    /** 탈출 성공 순간 타이머를 멈추고 결과를 표시한 뒤 별도 스레드에서 저장한다. */
    public void useKey() {
        if (!canPlay()) {
            return;
        }
        Escape result = game.useKey(System.nanoTime());
        render(result.view());
        if (result.record() == null) {
            widgets.status(result.message());
            return;
        }
        escaped = result;
        timer.stop();
        saveState = SaveState.FAILED;
        retrySave();
    }

    /** 저장 실패 후에도 탈출 성공과 확정 기록을 유지한다. 중복 저장 요청은 무시한다. */
    public void retrySave() {
        requireEdt();
        if (disposed || worker != null || saveState != SaveState.FAILED) {
            return;
        }
        ClearRecord record = escaped.record();
        saveState = SaveState.SAVING;
        widgets.busy(true);
        widgets.result(escaped, saveState);
        SwingWorker<Void, Void> saving = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                game.saveClear(record);
                return null;
            }

            @Override
            protected void done() {
                if (disposed || worker != this) {
                    return;
                }
                worker = null;
                try {
                    get();
                    saveState = SaveState.SAVED;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    saveState = SaveState.FAILED;
                } catch (ExecutionException | CancellationException exception) {
                    saveState = SaveState.FAILED;
                }
                widgets.busy(false);
                widgets.result(escaped, saveState);
            }
        };
        worker = saving;
        saving.execute();
    }

    /** 작업 중에는 이동을 막고, 미저장 결과가 있으면 호출자가 확인창을 표시한다. */
    public boolean canLeave(BooleanSupplier confirmDiscard) {
        requireEdt();
        return worker == null && (saveState != SaveState.FAILED || confirmDiscard.getAsBoolean());
    }

    /** 컨트롤러가 화면을 버릴 때 타이머와 작업을 정리한다. 반복 호출도 안전하다. */
    public void disposeGame() {
        requireEdt();
        disposed = true;
        timer.stop();
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
    }

    public boolean isTimerRunning() {
        requireEdt();
        return timer.isRunning();
    }

    private boolean canPlay() {
        requireEdt();
        return started && !disposed && worker == null && escaped == null;
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("게임 화면 조작은 EDT에서 실행해야 합니다.");
        }
    }

}
