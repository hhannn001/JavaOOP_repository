package coderoom.features.gameplay;

import java.util.Map;
import java.util.regex.Pattern;

/** Localizes the railway instructions after the current variant has been applied. */
final class ClueText {
    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");
    private static final Map<String, String> RAILWAY = Map.of(
            "flooded", "낮은선로침수", "bufferAhead", "본선정지턱있음",
            "highTrack", "높은선로", "lowTrack", "낮은선로", "sideTrack", "우회선로", "mainTrack", "본선");

    private ClueText() { }

    static String korean(long puzzleId, String code) {
        if (puzzleId != 405) { return code; }
        String rules = WORD.matcher(code).replaceAll(word -> RAILWAY.getOrDefault(word.group(), word.group()));
        return "낮은 선로에 물이 차 있으면 높은 선로로 간다.\n"
                + "본선 앞에 정지턱이 있으면 우회선로로 간다.\n\n" + rules;
    }
}
