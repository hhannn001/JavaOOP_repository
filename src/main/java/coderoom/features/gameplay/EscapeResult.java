package coderoom.features.gameplay;

import coderoom.entities.record.ClearRecord;

/** record가 null이면 탈출 조건을 아직 만족하지 않은 결과다. */
public record EscapeResult(GameView view, ClearRecord record, String message) { }
