package coderoom.features.gameplay;

import coderoom.entities.game.GameSession;
import coderoom.entities.game.GameStage;
import coderoom.entities.game.ItemId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 정답이나 저장소를 포함하지 않는 화면 스냅샷. */
public record GameView(GameStage stage, List<ItemView> items, ItemId selectedItem, long elapsedSeconds,
                       int solvedCount, int puzzleCount, boolean hintAvailable, Long previousBest,
                       Set<Integer> solvedSteps, Set<Integer> evidence, Set<Integer> poweredModules,
                       char installedSleeve, int platformDirection, int attemptsRemaining) {
    public GameView(GameStage stage, List<ItemView> items, ItemId selectedItem, long elapsedSeconds,
            int solvedCount, int puzzleCount, boolean hintAvailable, Long previousBest,
            Set<Integer> solvedSteps, Set<Integer> evidence, Set<Integer> poweredModules,
            char installedSleeve, int platformDirection) {
        this(stage, items, selectedItem, elapsedSeconds, solvedCount, puzzleCount, hintAvailable, previousBest,
                solvedSteps, evidence, poweredModules, installedSleeve, platformDirection,
                GameSession.MAX_WRONG_ATTEMPTS);
    }

    public GameView(GameStage stage, List<ItemView> items, ItemId selectedItem, long elapsedSeconds,
            int solvedCount, int puzzleCount, boolean hintAvailable, Long previousBest,
            Set<Integer> solvedSteps, Set<Integer> evidence) {
        this(stage, items, selectedItem, elapsedSeconds, solvedCount, puzzleCount, hintAvailable, previousBest,
                solvedSteps, evidence, Set.of(0, 2, 3), 'B', 1);
    }
    public GameView(GameStage stage, List<ItemView> items, ItemId selectedItem, long elapsedSeconds,
            int solvedCount, int puzzleCount, boolean hintAvailable, Long previousBest) {
        this(stage, items, selectedItem, elapsedSeconds, solvedCount, puzzleCount, hintAvailable, previousBest,
                IntStream.range(0, solvedCount).boxed().collect(Collectors.toUnmodifiableSet()), Set.of());
    }
    public GameView(GameStage stage, List<ItemView> items, ItemId selectedItem, long elapsedSeconds) {
        this(stage, items, selectedItem, elapsedSeconds, 0, 0, false, null);
    }
    public boolean attemptsExhausted() { return attemptsRemaining == 0; }

    public GameView {
        Objects.requireNonNull(stage);
        items       = List.copyOf(items);
        solvedSteps = Set.copyOf(solvedSteps);
        evidence    = Set.copyOf(evidence);
        poweredModules = Set.copyOf(poweredModules);
        if (elapsedSeconds < 0) {
            throw new IllegalArgumentException("경과 시간은 음수일 수 없습니다.");
        }
    }
}
