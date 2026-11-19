package coderoom.entities.puzzle;

public enum AnswerType {
    DIGITS4(4), GRID3X3(9);

    private final int length;

    AnswerType(int length) { this.length = length; }

    public boolean accepts(String answer) {
        return answer != null && answer.matches("[0-9]{" + length + "}");
    }
}
