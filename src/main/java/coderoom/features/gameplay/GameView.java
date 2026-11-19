package coderoom.features.gameplay;

import coderoom.entities.game.GameStage;
import coderoom.entities.game.ItemId;
import java.util.List;
import java.util.Objects;

/** 정답이나 저장소를 포함하지 않는 화면 스냅샷. */
public record GameView(GameStage stage, List<ItemView> items, ItemId selectedItem, long elapsedSeconds) {
    public GameView {
        Objects.requireNonNull(stage);
        items = List.copyOf(items);
        if (elapsedSeconds < 0) {
            throw new IllegalArgumentException("경과 시간은 음수일 수 없습니다.");
        }
    }
}
