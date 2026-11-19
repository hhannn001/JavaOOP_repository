package coderoom.entities.record;

import java.util.Objects;

/** 클리어한 플레이에서 확인한 문제와 정답의 불변 스냅샷. */
public record SolvedPuzzle(long puzzleId, int stepNo, String code, String answer) {
    public SolvedPuzzle {
        if (puzzleId <= 0 || stepNo <= 0) {
            throw new IllegalArgumentException("문제 ID와 순서는 양수여야 합니다.");
        }
        Objects.requireNonNull(code);
        Objects.requireNonNull(answer);
    }
}
