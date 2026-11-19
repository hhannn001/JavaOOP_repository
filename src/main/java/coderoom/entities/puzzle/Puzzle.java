package coderoom.entities.puzzle;

import java.util.Objects;

/** 정답은 도메인 안에 보관하며 화면 DTO에 전달하지 않는다. */
public final class Puzzle {
    private final long puzzleId;
    private final long mapId;
    private final int stepNo;
    private final String code;
    private final AnswerType answerType;
    private final String answer;
    private final String hint;

    public Puzzle(long puzzleId, long mapId, int stepNo, String code,
            AnswerType answerType, String answer, String hint) {
        if (puzzleId <= 0 || mapId <= 0 || stepNo <= 0) {
            throw new IllegalArgumentException("퍼즐 ID와 순서는 양수여야 합니다.");
        }
        this.puzzleId   = puzzleId;
        this.mapId      = mapId;
        this.stepNo     = stepNo;
        this.code       = Objects.requireNonNull(code);
        this.answerType = Objects.requireNonNull(answerType);
        this.answer     = Objects.requireNonNull(answer);
        this.hint       = Objects.requireNonNull(hint);
        if (!answerType.accepts(answer)) {
            throw new IllegalArgumentException("저장된 퍼즐 정답 형식이 잘못되었습니다.");
        }
    }

    public AnswerCheck check(String rawAnswer) {
        if (!answerType.accepts(rawAnswer)) {
            return AnswerCheck.INVALID;
        }
        return answer.equals(rawAnswer) ? AnswerCheck.CORRECT : AnswerCheck.WRONG;
    }

    public long puzzleId() { return puzzleId; }
    public long mapId() { return mapId; }
    public int stepNo() { return stepNo; }
    public String code() { return code; }
    public AnswerType answerType() { return answerType; }
    public String hint() { return hint; }
}
