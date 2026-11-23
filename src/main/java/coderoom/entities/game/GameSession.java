package coderoom.entities.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 한 번의 플레이 상태. 호출자는 상태 변경을 EDT에서 직렬화한다. */
public final class GameSession {
    public static final long TIME_LIMIT_SECONDS = 600;
    public static final int MAX_WRONG_ATTEMPTS = 5;
    private final long userId;
    private final long mapId;
    private final long startedAtNanos;
    private final int puzzleCount;
    private final WorkshopRoom workshop;
    private final Set<Integer> solved   = new HashSet<>();
    private final Set<Integer> evidence = new HashSet<>();
    private int collectedHints;
    private int attemptsRemaining = MAX_WRONG_ATTEMPTS;
    private ItemId selectedItem;
    private Long clearedSeconds;

    public GameSession(long userId, long mapId, long startedAtNanos) {
        this(userId, mapId, startedAtNanos, 2);
    }

    public GameSession(long userId, long mapId, long startedAtNanos, int puzzleCount) {
        if (userId <= 0 || mapId <= 0) {
            throw new IllegalArgumentException("사용자와 맵 ID는 양수여야 합니다.");
        }
        if (puzzleCount < 1 || puzzleCount > 5) {
            throw new IllegalArgumentException("한 맵에는 문제를 1~5개 배치할 수 있습니다.");
        }
        this.puzzleCount    = puzzleCount;
        this.userId         = userId;
        this.mapId          = mapId;
        this.startedAtNanos = startedAtNanos;
        this.workshop       = WorkshopRoom.forMap(mapId);
    }

    public List<ItemId> inventory() {
        List<ItemId> items = new ArrayList<>();
        for (int index = 0; index < puzzleCount; index++) {
            if (workshop == null ? index <= solved.size() : evidence.contains(index)) {
                items.add(ItemId.notes().get(index));
            }
        }
        if (workshop == null && solved.size() == puzzleCount) {
            items.add(ItemId.KEY);
        }
        return List.copyOf(items);
    }

    public void select(ItemId item) {
        if (clearedSeconds != null || attemptsRemaining == 0 || !inventory().contains(item)) {
            throw new IllegalArgumentException("선택할 수 없는 아이템입니다.");
        }
        selectedItem = item;
    }

    public void solveStep(int step) {
        if (!available(step - 1)) {
            throw new IllegalStateException("지금 풀 수 없는 문제입니다.");
        }
        solved.add(step - 1);
    }

    public boolean available(int index) {
        if (clearedSeconds != null || attemptsRemaining == 0 || index < 0
                || index >= puzzleCount || solved.contains(index)) { return false; }
        return workshop == null ? index == solved.size() : solved.containsAll(workshop.task(index).requires());
    }

    public Set<Integer> solvedSteps() { return Set.copyOf(solved); }

    public Set<Integer> foundEvidence() { return Set.copyOf(evidence); }

    public void discover(int index) {
        if (attemptsRemaining > 0 && workshop != null && index >= 0
                && index < puzzleCount && workshop.task(index).hasEvidence()) {
            evidence.add(index);
        }
    }

    public void unlockDrawer() { solveStep(1); }

    public void unlockSafe() { solveStep(2); }

    public boolean escape(long nowNanos) {
        if (clearedSeconds != null) {
            return true;
        }
        if (attemptsRemaining == 0 || elapsedSeconds(nowNanos) >= TIME_LIMIT_SECONDS
                || solved.size() != puzzleCount || workshop == null && selectedItem != ItemId.KEY) {
            return false;
        }
        clearedSeconds = elapsedSeconds(nowNanos);
        return true;
    }

    public long elapsedSeconds(long nowNanos) {
        return clearedSeconds != null ? clearedSeconds : Math.max(0, (nowNanos - startedAtNanos) / 1_000_000_000L);
    }

    public long userId() { return userId; }
    public long mapId() { return mapId; }
    public boolean hasHint(int index) { return workshop == null && index < collectedHints; }

    public boolean canCollectHint() {
        return workshop == null && clearedSeconds == null && attemptsRemaining > 0
                && collectedHints < Math.min(puzzleCount, solved.size() + 1);
    }

    public void collectHint() {
        if (canCollectHint()) {
            collectedHints++;
        }
    }

    public void recordWrongAnswer() {
        if (attemptsRemaining > 0 && clearedSeconds == null) {
            attemptsRemaining--;
        }
    }

    public int attemptsRemaining() { return attemptsRemaining; }

    public int solvedCount() { return solved.size(); }

    public GameStage stage() {
        if (clearedSeconds != null) {
            return GameStage.ESCAPED;
        }
        if (solved.size() == puzzleCount) {
            return GameStage.SAFE_OPEN;
        }
        return !solved.isEmpty() ? GameStage.DRAWER_OPEN : GameStage.START;
    }
    public ItemId selectedItem() { return selectedItem; }
}
