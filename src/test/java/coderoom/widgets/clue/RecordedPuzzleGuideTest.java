package coderoom.widgets.clue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordedPuzzleGuideTest {
    @Test void routesKeepTheSavedAnswerFormat() {
        assertEquals("점검: 높은 선로 → 우회선로\n운반: 낮은 선로 → 본선",
                RecordedPuzzleGuide.forPuzzle(405, "", "BAAB").solution());
        assertEquals("첫 분기에서 높은 선로, 두 번째 분기에서 출구 쪽 선로로 이동",
                RecordedPuzzleGuide.forPuzzle(405, "takeHighTrack(); takeDoorBranch();", "BA").solution());
    }
}
