package coderoom.widgets.clue;

import coderoom.features.gameplay.PuzzleVariant;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordedPuzzleDiagramTest {
    @Test void balanceDiagramUsesTheSavedLoadAndBridgeHasAnIllustration() {
        var diagram = RecordedPuzzleDiagram.create(404, "int left = 17 * 1;", "CD").orElseThrow();
        assertTrue(diagram.getAccessibleContext().getAccessibleName().contains("무게 17"));
        assertTrue(RecordedPuzzleDiagram.create(403, "가로 발판을 옮긴다.", "AB").isPresent());
    }

    @Test void oldDistributionSnapshotShowsTheOriginalEquipmentAndFaults() {
        var table = (JPanel) RecordedPuzzleDiagram.create(313, """
                분배 작업표
                우선순위 숫자가 작은 장비부터 가동한다.
                고장 장비는 제외하고, 합계 70까지 허용한다.

                devices.sort((a, b) ->
                    Integer.compare(b.priority, a.priority));
                if (used + d.power >= 70) continue;

                EXHAUST 20/1, PUMP 35/2, COOLER 40/3 고장,
                LAMP 15/4, BACKUP 20/5.
                """, "AB").orElseThrow();
        var grid = (JPanel) table.getComponent(1);
        List<String> cells = Arrays.stream(grid.getComponents()).map(JLabel.class::cast).map(JLabel::getText).toList();
        assertEquals(List.of("장비", "소비 전력", "우선순위", "상태",
                "EXHAUST", "20W", "1", "정상", "PUMP", "35W", "2", "정상",
                "COOLER", "40W", "3", "고장", "LAMP", "15W", "4", "정상",
                "BACKUP", "20W", "5", "정상"), cells);
        var note = (JLabel) table.getComponent(2);
        assertTrue(note.getText().contains("서로 다릅니다"));
    }

    @Test void variableDiagramsRequireRecognizableSavedRules() {
        for (PuzzleVariant variant : PuzzleVariant.values()) {
            String code = "Device[] queue = {" + String.join(", ", variant.distributionQueue().split(""))
                    + "};\nint left = " + variant.distributionCapacity() + ";";
            assertTrue(RecordedPuzzleDiagram.create(313, code, "AC").isPresent());
            assertTrue(RecordedPuzzleDiagram.create(401, variant.specimenRule().javaConditions(), "BC").isPresent());
        }
        assertTrue(RecordedPuzzleDiagram.create(401, "legacy code", "BC").isEmpty());
    }
}
