package coderoom.entities.puzzle;

public enum AnswerType {
    DIGITS4("[0-9]{4}"), GRID3X3("[0-9]{9}"), VALVE("[A-D]"),
    LEVER_ORDER("[1-4]{4}"), DEBUG_LINES("[0-9]{4}"), CARD_ORDER("[1-4]{4}"), WORKSHOP("[A-Z]{1,8}");

    private final String pattern;

    AnswerType(String pattern) { this.pattern = pattern; }

    public boolean accepts(String answer) {
        return answer != null && answer.matches(pattern);
    }
}
