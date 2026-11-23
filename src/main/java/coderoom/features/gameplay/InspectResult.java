package coderoom.features.gameplay;

import coderoom.entities.game.RoomObjectId;
import coderoom.entities.puzzle.AnswerType;

/** 장치 입력 정보 또는 안내. 정답은 전달하지 않는다. */
public record InspectResult(RoomObjectId object, String message, AnswerType answerType, ItemView instructions, PuzzleVariant variant) {
    public InspectResult(RoomObjectId object, String message, AnswerType answerType) {
        this(object, message, answerType, null, PuzzleVariant.STANDARD);
    }
}
