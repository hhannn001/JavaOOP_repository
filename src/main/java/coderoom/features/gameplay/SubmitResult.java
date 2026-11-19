package coderoom.features.gameplay;

public record SubmitResult(Verdict verdict, String message, GameView view) {
    public enum Verdict { WRONG, INVALID, BLOCKED, ALREADY_SOLVED, SOLVED }
}
