package coderoom.features.gameplay;

import coderoom.entities.map.GameMap;
import coderoom.entities.puzzle.Puzzle;
import java.util.List;
import java.util.Objects;

/** 조회한 원본 데이터. 정답을 포함하므로 UI에 표시하지 않는다. */
public record GameSetup(GameMap map, List<Puzzle> puzzles) {
    public GameSetup {
        Objects.requireNonNull(map);
        puzzles = List.copyOf(puzzles);
    }
}
