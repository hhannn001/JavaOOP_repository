package coderoom.entities.game;

import java.util.List;

/** 한 번의 플레이 상태. 호출자는 상태 변경을 EDT에서 직렬화한다. */
public final class GameSession {
    private final long userId;
    private final long mapId;
    private final long startedAtNanos;
    private GameStage stage = GameStage.START;
    private ItemId selectedItem;
    private Long clearedSeconds;

    public GameSession(long userId, long mapId, long startedAtNanos) {
        if (userId <= 0 || mapId <= 0) {
            throw new IllegalArgumentException("사용자와 맵 ID는 양수여야 합니다.");
        }
        this.userId         = userId;
        this.mapId          = mapId;
        this.startedAtNanos = startedAtNanos;
    }

    public List<ItemId> inventory() {
        return switch (stage) {
            case START -> List.of(ItemId.NOTE1);
            case DRAWER_OPEN -> List.of(ItemId.NOTE1, ItemId.NOTE2);
            case SAFE_OPEN, ESCAPED -> List.of(ItemId.NOTE1, ItemId.NOTE2, ItemId.KEY);
        };
    }

    public void select(ItemId item) {
        if (stage == GameStage.ESCAPED || !inventory().contains(item)) {
            throw new IllegalArgumentException("선택할 수 없는 아이템입니다.");
        }
        selectedItem = item;
    }

    public void unlockDrawer() {
        if (stage != GameStage.START) {
            throw new IllegalStateException("이미 열린 서랍입니다.");
        }
        stage = GameStage.DRAWER_OPEN;
    }

    public void unlockSafe() {
        if (stage != GameStage.DRAWER_OPEN) {
            throw new IllegalStateException("서랍을 먼저 열어야 합니다.");
        }
        stage = GameStage.SAFE_OPEN;
    }

    public boolean escape(long nowNanos) {
        if (stage == GameStage.ESCAPED) {
            return true;
        }
        if (stage != GameStage.SAFE_OPEN || selectedItem != ItemId.KEY) {
            return false;
        }
        clearedSeconds = elapsedSeconds(nowNanos);
        stage          = GameStage.ESCAPED;
        return true;
    }

    public long elapsedSeconds(long nowNanos) {
        return clearedSeconds != null ? clearedSeconds : Math.max(0, (nowNanos - startedAtNanos) / 1_000_000_000L);
    }

    public long userId() { return userId; }
    public long mapId() { return mapId; }
    public GameStage stage() { return stage; }
    public ItemId selectedItem() { return selectedItem; }
}
