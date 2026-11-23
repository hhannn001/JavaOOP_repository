package coderoom.shared.ui;

import java.util.Locale;

/** Shared marks on the greenhouse envelopes and in the specimen card's Java literals. */
public enum SpecimenMark {
    ROUND('●', "둥근 잎"),
    SERRATED('✹', "톱니 잎"),
    INTACT('✓', "정상"),
    DAMAGED('×', "손상"),
    WAVE('∿', "파도"),
    TRIANGLE('▲', "삼각"),
    SQUARE('■', "사각");

    private final char symbol;
    private final String description;

    SpecimenMark(char symbol, String description) {
        this.symbol      = symbol;
        this.description = description;
    }

    public String description() { return description; }

    public String imagePath() {
        return "/images/game-ui/specimen-marks/" + name().toLowerCase(Locale.ROOT) + ".png";
    }

    public static SpecimenMark fromSymbol(char symbol) {
        for (SpecimenMark mark : values()) {
            if (mark.symbol == symbol) { return mark; }
        }
        return null;
    }
}
