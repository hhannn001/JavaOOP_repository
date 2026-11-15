package coderoom.features.gameplay;

import coderoom.entities.game.GameSession;
import coderoom.entities.game.GameStage;
import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.map.GameMap;
import coderoom.entities.map.MapRepository;
import coderoom.entities.puzzle.AnswerCheck;
import coderoom.entities.puzzle.AnswerType;
import coderoom.entities.puzzle.Puzzle;
import coderoom.entities.puzzle.PuzzleRepository;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.record.ClearRecordRepository;
import java.util.List;
import java.util.Objects;

/** 게임 규칙을 조율한다. DB 조회·저장은 작업 스레드, 세션 조작은 EDT에서 호출한다. */
public final class GameFacade {
    private final MapRepository maps;
    private final PuzzleRepository puzzles;
    private final ClearRecordRepository records;
    private GameSetup setup;
    private GameSession session;

    public GameFacade(MapRepository maps, PuzzleRepository puzzles, ClearRecordRepository records) {
        this.maps    = Objects.requireNonNull(maps);
        this.puzzles = Objects.requireNonNull(puzzles);
        this.records = Objects.requireNonNull(records);
    }

    public GameSetup loadMap(long mapId) {
        GameMap map = maps.findById(Math.toIntExact(mapId))
                .orElseThrow(() -> new IllegalArgumentException("맵을 찾을 수 없습니다."));
        GameSetup loaded = new GameSetup(map, puzzles.findByMapId(mapId));
        validate(loaded);
        return loaded;
    }

    private static void validate(GameSetup loaded) {
        List<Puzzle> entries = loaded.puzzles();
        if (!loaded.map().isPlayable() || entries.size() != 2
                || entries.get(0).stepNo() != 1 || entries.get(1).stepNo() != 2
                || entries.get(0).answerType() != AnswerType.DIGITS4
                || entries.get(1).answerType() != AnswerType.GRID3X3
                || entries.stream().anyMatch(puzzle -> puzzle.mapId() != loaded.map().getMapId())) {
            throw new IllegalArgumentException("플레이할 수 없는 맵 또는 잘못된 퍼즐 구성입니다.");
        }
    }

    public GameView start(long userId, GameSetup loaded, long nowNanos) {
        validate(loaded);
        setup   = loaded;
        session = new GameSession(userId, loaded.map().getMapId(), nowNanos);
        return view(nowNanos);
    }

    public InspectResult inspect(RoomObjectId object) {
        requireSession();
        return switch (Objects.requireNonNull(object)) {
            case DRAWER -> session.stage() == GameStage.START
                    ? new InspectResult(object, "첫 번째 메모의 출력값으로 서랍을 여세요.", AnswerType.DIGITS4)
                    : new InspectResult(object, "열린 서랍에서 두 번째 메모를 얻었습니다.", null);
            case WALL_DIAL -> session.stage() == GameStage.DRAWER_OPEN
                    ? new InspectResult(object, "두 번째 메모의 출력값을 행 순서대로 입력하세요.", AnswerType.GRID3X3)
                    : new InspectResult(object, session.stage() == GameStage.START
                            ? "서랍을 먼저 열어 두 번째 메모를 찾으세요." : "금고는 이미 열려 있습니다.", null);
            case DOOR -> new InspectResult(object, "인벤토리에서 열쇠를 선택한 뒤 문을 누르세요.", null);
            case BOOKS -> new InspectResult(object, "손에 든 메모에 적힌 Java 코드가 잠금장치의 단서입니다.", null);
            case WINDOW -> new InspectResult(object, "창문은 잠겨 있습니다. 문을 통해 나가야 합니다.", null);
        };
    }

    public SubmitResult submit(RoomObjectId object, String answer) {
        requireSession();
        if (session.stage() == GameStage.ESCAPED
                || (object != RoomObjectId.DRAWER && object != RoomObjectId.WALL_DIAL)) {
            return result(SubmitResult.Verdict.BLOCKED, "지금은 입력할 수 없습니다.");
        }
        boolean drawer = object == RoomObjectId.DRAWER;
        if ((drawer && session.stage() != GameStage.START)
                || (!drawer && session.stage() == GameStage.SAFE_OPEN)) {
            return result(SubmitResult.Verdict.ALREADY_SOLVED, "이미 해결한 장치입니다.");
        }
        if (!drawer && session.stage() == GameStage.START) {
            return result(SubmitResult.Verdict.BLOCKED, "서랍을 먼저 열어야 합니다.");
        }
        AnswerCheck check = setup.puzzles().get(drawer ? 0 : 1).check(answer);
        if (check == AnswerCheck.INVALID) {
            return result(SubmitResult.Verdict.INVALID, drawer ? "숫자 4자리를 입력하세요." : "숫자 9자리를 입력하세요.");
        }
        if (check == AnswerCheck.WRONG) {
            return result(SubmitResult.Verdict.WRONG, "암호가 맞지 않습니다. 메모를 다시 확인하세요.");
        }
        if (drawer) {
            session.unlockDrawer();
        } else {
            session.unlockSafe();
        }
        return result(SubmitResult.Verdict.SOLVED, drawer
                ? "서랍이 열렸습니다. 두 번째 메모를 얻었습니다." : "금고가 열렸습니다. 열쇠를 얻었습니다.");
    }

    private SubmitResult result(SubmitResult.Verdict verdict, String message) {
        return new SubmitResult(verdict, message, view(System.nanoTime()));
    }

    public GameView selectItem(ItemId item) {
        requireSession();
        session.select(item);
        return view(System.nanoTime());
    }

    public EscapeResult useKey(long nowNanos) {
        requireSession();
        if (!session.escape(nowNanos)) {
            return new EscapeResult(view(nowNanos), null, "열쇠를 획득하고 선택한 뒤 문을 누르세요.");
        }
        return new EscapeResult(view(nowNanos),
                new ClearRecord(session.userId(), session.mapId(), session.elapsedSeconds(nowNanos)), "탈출 성공!");
    }

    public void saveClear(ClearRecord record) {
        records.saveBest(record);
    }

    public GameView view(long nowNanos) {
        requireSession();
        List<ItemView> items = session.inventory().stream().map(item -> switch (item) {
            case NOTE1 -> note(item, "메모 1", setup.puzzles().get(0));
            case NOTE2 -> note(item, "메모 2", setup.puzzles().get(1));
            case KEY -> new ItemView(item, "열쇠", "문을 열 수 있는 열쇠입니다. 선택한 상태로 문을 누르세요.");
        }).toList();
        return new GameView(session.stage(), items, session.selectedItem(), session.elapsedSeconds(nowNanos));
    }

    private ItemView note(ItemId item, String title, Puzzle puzzle) {
        return new ItemView(item, title, puzzle.code(), puzzle.hint());
    }

    private void requireSession() {
        if (session == null) {
            throw new IllegalStateException("게임이 시작되지 않았습니다.");
        }
    }
}
