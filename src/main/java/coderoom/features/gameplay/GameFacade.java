package coderoom.features.gameplay;

import java.util.Set;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import coderoom.entities.game.GameSession;
import coderoom.entities.game.GameStage;
import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.game.WorkshopRoom;
import coderoom.entities.map.GameMap;
import coderoom.entities.map.MapRepository;
import coderoom.entities.puzzle.AnswerCheck;
import coderoom.entities.puzzle.Puzzle;
import coderoom.entities.puzzle.PuzzleRepository;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.record.ClearRecordRepository;
import java.util.List;
import java.util.ArrayList;
import coderoom.entities.record.SolvedPuzzle;
import java.util.Objects;

/** 게임 규칙을 조율한다. DB 조회·저장은 작업 스레드, 세션 조작은 EDT에서 호출한다. */
public final class GameFacade {
    private static final List<RoomObjectId> DEVICES = List.of(RoomObjectId.DRAWER, RoomObjectId.WALL_DIAL,
            RoomObjectId.THIRD_DEVICE, RoomObjectId.FOURTH_DEVICE, RoomObjectId.FIFTH_DEVICE);

    private final MapRepository maps;
    private final PuzzleRepository puzzles;
    private final ClearRecordRepository records;
    private final List<SolvedPuzzle> solvedPuzzles = new ArrayList<>();
    private final LongFunction<PuzzleVariant> variants;
    private List<PuzzleVariant> deviceVariants = List.of();
    private GameSetup setup;
    private GameSession session;
    private WorkshopRoom workshop;
    private Set<Integer> poweredModules = Set.of();
    private char installedSleeve;

    public GameFacade(MapRepository maps, PuzzleRepository puzzles, ClearRecordRepository records) {
        this(maps, puzzles, records, ignored -> PuzzleVariant.STANDARD);
    }

    public GameFacade(MapRepository maps, PuzzleRepository puzzles, ClearRecordRepository records,
            LongFunction<PuzzleVariant> variants) {
        this.variants = Objects.requireNonNull(variants);
        this.maps     = Objects.requireNonNull(maps);
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

    public GameSetup loadMap(long mapId, long userId) {
        GameSetup loaded = loadMap(mapId);
        Long previousBest = records.findByUserId(userId).stream()
                .filter(record -> record.getMapId() == mapId)
                .map(ClearRecord::getBestSeconds).findFirst().orElse(null);
        return new GameSetup(loaded.map(), loaded.puzzles(), previousBest);
    }

    private static void validate(GameSetup loaded) {
        List<Puzzle> entries = loaded.puzzles();
        if (!loaded.map().isPlayable() || entries.isEmpty() || entries.size() > DEVICES.size()) {
            throw new IllegalArgumentException("플레이할 수 없는 맵 또는 잘못된 퍼즐 구성입니다.");
        }
        if (WorkshopRoom.forMap(loaded.map().getMapId()) != null && entries.size() != 5) {
            throw new IllegalArgumentException("설비 맵은 다섯 문제를 필요로 합니다.");
        }
        for (int index = 0; index < entries.size(); index++) {
            Puzzle puzzle = entries.get(index);
            if (puzzle.stepNo() != index + 1 || puzzle.mapId() != loaded.map().getMapId()) {
                throw new IllegalArgumentException("퍼즐의 맵 또는 순서가 올바르지 않습니다.");
            }
        }
    }

    public GameView start(long userId, GameSetup loaded, long nowNanos) {
        validate(loaded);
        solvedPuzzles.clear();
        poweredModules  = Set.of();
        installedSleeve = '\0';
        PuzzleVariant variant = variants.apply(loaded.map().getMapId());
        List<Puzzle> entries  = loaded.puzzles().stream().map(variant::apply).toList();
        deviceVariants = IntStream.range(0, entries.size())
                .mapToObj(i -> entries.get(i) == loaded.puzzles().get(i) ? PuzzleVariant.STANDARD : variant).toList();
        setup = new GameSetup(loaded.map(), entries, loaded.previousBest());
        workshop = WorkshopRoom.forMap(loaded.map().getMapId());
        session = new GameSession(userId, loaded.map().getMapId(), nowNanos, loaded.puzzles().size());
        return view(nowNanos);
    }

    public InspectResult inspect(RoomObjectId object) {
        requireSession();
        if (object == RoomObjectId.HINT_NOTE) {
            session.collectHint();
            return new InspectResult(object, "접힌 메모의 힌트를 인벤토리에서 다시 읽을 수 있습니다.", null);
        }
        if (workshop != null && object != null && object.evidenceIndex() >= 0) {
            int evidence = object.evidenceIndex();
            session.discover(evidence);
            return new InspectResult(object, "", null);
        }
        int index = DEVICES.indexOf(Objects.requireNonNull(object));
        if (index >= 0 && index < setup.puzzles().size()) {
            if (session.available(index)) {
                return new InspectResult(object, workshop == null ? "메모 " + (index + 1) + "을 확인하세요." : workshop.task(index).title(),
                        setup.puzzles().get(index).answerType(), workshop == null ? null : itemView(ItemId.notes().get(index)),
                        deviceVariants.get(index));
            }
            String message = session.solvedSteps().contains(index) ? "이미 해결한 장치입니다." : blockedMessage(index);
            return new InspectResult(object, message, null);
        }
        return new InspectResult(object, RoomStory.inspection(object, session.stage()), null);
    }

    public SubmitResult submit(RoomObjectId object, String answer) {
        requireSession();
        int index = object == null ? -1 : DEVICES.indexOf(object);
        if (session.stage() == GameStage.ESCAPED || session.attemptsRemaining() == 0
                || index < 0 || index >= setup.puzzles().size()) {
            return result(SubmitResult.Verdict.BLOCKED, "지금은 입력할 수 없습니다.");
        }
        if (session.solvedSteps().contains(index)) {
            return result(SubmitResult.Verdict.ALREADY_SOLVED, "이미 해결한 장치입니다.");
        }
        if (!session.available(index)) {
            return result(SubmitResult.Verdict.BLOCKED, blockedMessage(index));
        }
        Puzzle puzzle = setup.puzzles().get(index);
        AnswerCheck check = puzzle.check(answer);
        if (check == AnswerCheck.INVALID) {
            return result(SubmitResult.Verdict.INVALID,
                    "장치에서 요구하는 형식으로 입력하세요.");
        }
        if (check == AnswerCheck.WRONG) {
            session.recordWrongAnswer();
            return result(SubmitResult.Verdict.WRONG, workshop == null ? "암호가 맞지 않습니다. 메모를 다시 확인하세요."
                    : "시험 결과가 조건과 맞지 않습니다. 장치나 수첩을 살펴보세요.");
        }
        session.solveStep(puzzle.stepNo());
        solvedPuzzles.add(new SolvedPuzzle(puzzle.puzzleId(), puzzle.stepNo(), puzzle.code(), answer));
        if (workshop == WorkshopRoom.ENGINE && index == 0) {
            poweredModules = answer.chars().mapToObj(letter -> letter - 'A')
                    .filter(slot -> slot >= 0 && slot < 5).collect(Collectors.toUnmodifiableSet());
        } else if (workshop == WorkshopRoom.ENGINE && index == 1 && answer.length() > 1) {
            installedSleeve = answer.charAt(1);
        }
        if (setup.map().getMapId() == 1 && setup.puzzles().size() == 2) {
            return result(SubmitResult.Verdict.SOLVED, index == 0
                    ? "서랍이 열렸습니다. 두 번째 메모를 얻었습니다." : "금고가 열렸습니다. 열쇠를 얻었습니다.");
        }
        if (workshop != null) {
            return result(SubmitResult.Verdict.SOLVED, workshop.outcome(index));
        }
        String message = session.solvedCount() == setup.puzzles().size()
                ? "마지막 장치가 열렸습니다. 열쇠를 얻었습니다."
                : "장치가 열렸습니다. 다음 메모를 얻었습니다.";
        return result(SubmitResult.Verdict.SOLVED, message);
    }

    private String blockedMessage(int index) {
        if (workshop == null) { return "앞선 문제를 먼저 해결하세요."; }
        return workshop.task(index).requires().stream().sorted()
                .filter(required -> !session.solvedSteps().contains(required))
                .map(required -> workshop.task(required).title())
                .collect(Collectors.joining(", ", "아직 준비되지 않은 설비: ", ""));
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
            return new EscapeResult(view(nowNanos), null, workshop == null ? "열쇠를 획득하고 선택한 뒤 문을 누르세요."
                    : "출구 안전 걸쇠가 아직 잠겨 있습니다.");
        }
        return new EscapeResult(view(nowNanos),
                new ClearRecord(session.userId(), session.mapId(), session.elapsedSeconds(nowNanos), solvedPuzzles), "탈출 성공!");
    }

    public void saveClear(ClearRecord record) {
        records.saveBest(record);
    }

    public GameView view(long nowNanos) {
        requireSession();
        List<ItemView> items     = session.inventory().stream().map(this::itemView).toList();
        Set<Integer> solvedSteps = session.solvedSteps();
        int platformDirection    = workshop == WorkshopRoom.GREENHOUSE && solvedSteps.contains(2) ? 1 : 0;
        return new GameView(session.stage(), items, session.selectedItem(), session.elapsedSeconds(nowNanos),
                session.solvedCount(), setup.puzzles().size(), session.canCollectHint(), setup.previousBest(), solvedSteps,
                session.foundEvidence(), poweredModules, installedSleeve, platformDirection, session.attemptsRemaining());
    }

    private ItemView itemView(ItemId item) {
        if (item == ItemId.KEY) {
            return new ItemView(item, "열쇠", "문을 열 수 있는 열쇠입니다. 선택한 상태로 문을 누르세요.");
        }
        int index                = ItemId.notes().indexOf(item);
        Puzzle puzzle            = setup.puzzles().get(index);
        String text              = puzzle.code();
        ItemView.Diagram diagram = ItemView.Diagram.forPuzzle(puzzle.puzzleId());
        PuzzleVariant variant    = deviceVariants.get(index);
        if (workshop != null) {
            WorkshopRoom.Task task = workshop.task(index);
            String title           = task.hasEvidence() ? task.evidence() : task.title() + " 작업 안내";
            return new ItemView(item, title, text, "", diagram, variant);
        }
        if (session.mapId() == 1) {
            String instructions = switch (index) {
                case 0 -> "코드가 출력하는 숫자 네 개를 서랍에 왼쪽부터 맞춘다. 숫자 휠을 눌러 돌린 뒤 서랍 손잡이를 누른다.";
                case 1 -> "코드의 출력 한 줄이 다이얼 한 행이다. 위에서 아래로, 각 행은 왼쪽에서 오른쪽으로 맞추고 아래 황동 버튼을 누른다.";
                default -> "";
            };
            if (!instructions.isEmpty()) { text = instructions + "\n\n" + text; }
        }
        return new ItemView(item, "메모 " + (index + 1), text, "", diagram, variant);
    }

    private void requireSession() {
        if (session == null) {
            throw new IllegalStateException("게임이 시작되지 않았습니다.");
        }
    }
}
