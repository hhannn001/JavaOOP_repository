package coderoom.features.gameplay.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class DragPuzzlePanelTest {
    @Test
    void dropOutsidePreservesPiecesAndAValidDropCanBeReset() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DragPuzzlePanel panel = new DragPuzzlePanel("ABCD", 1);
            panel.setSize(400, 200);

            drag(panel, 244, 60, 50, 100);
            assertEquals("", panel.answer());

            drag(panel, 244, 60, 200, 150);
            assertEquals("C", panel.answer());

            drag(panel, 156, 60, 200, 150);
            assertEquals("B", panel.answer());

            panel.reset();
            assertEquals("", panel.answer());
            drag(panel, 244, 60, 200, 150);
            assertEquals("C", panel.answer());
        });
    }

    private static void drag(DragPuzzlePanel panel, int fromX, int fromY, int toX, int toY) {
        long time = System.currentTimeMillis();
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, time,
                InputEvent.BUTTON1_DOWN_MASK, fromX, fromY, 1, false, MouseEvent.BUTTON1));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, time,
                InputEvent.BUTTON1_DOWN_MASK, toX, toY, 0, false, MouseEvent.NOBUTTON));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, time,
                0, toX, toY, 1, false, MouseEvent.BUTTON1));
    }
}
