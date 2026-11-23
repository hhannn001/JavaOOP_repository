package coderoom.shared.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class HandCursorTest {
    @Test void heldCursorFollowsDragAndReturnsToOpenHandOnRelease() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = panelWithTarget();
            Cursor open  = HandCursor.openHand();
            assertEquals(open, panel.getCursor());
            send(panel, MouseEvent.MOUSE_ENTERED, 10, MouseEvent.NOBUTTON);
            assertEquals(open, panel.getCursor());
            send(panel, MouseEvent.MOUSE_PRESSED, 10, MouseEvent.BUTTON1);
            Cursor closed = panel.getCursor();
            assertNotEquals(open, closed);
            send(panel, MouseEvent.MOUSE_DRAGGED, 60, MouseEvent.NOBUTTON);
            assertEquals(closed, panel.getCursor());
            send(panel, MouseEvent.MOUSE_RELEASED, 60, MouseEvent.BUTTON1);
            assertEquals(open, panel.getCursor());
            send(panel, MouseEvent.MOUSE_MOVED, 10, MouseEvent.NOBUTTON);
            send(panel, MouseEvent.MOUSE_PRESSED, 10, MouseEvent.BUTTON1);
            send(panel, MouseEvent.MOUSE_RELEASED, 10, MouseEvent.BUTTON1);
            assertEquals(open, panel.getCursor());
        });
    }

    @Test void disabledAndEmptyTargetsDoNotShowTheGrabCursor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = panelWithTarget();
            Cursor open  = HandCursor.openHand();
            send(panel, MouseEvent.MOUSE_PRESSED, 60, MouseEvent.BUTTON1);
            assertEquals(open, panel.getCursor());
            send(panel, MouseEvent.MOUSE_RELEASED, 60, MouseEvent.BUTTON1);
            send(panel, MouseEvent.MOUSE_PRESSED, 10, MouseEvent.BUTTON1);
            panel.setEnabled(false);
            assertEquals(open, panel.getCursor());
            send(panel, MouseEvent.MOUSE_MOVED, 10, MouseEvent.NOBUTTON);
            assertEquals(open, panel.getCursor());
        });
    }

    private static JPanel panelWithTarget() {
        JPanel panel = new JPanel();
        panel.setSize(100, 100);
        HandCursor.install(panel, point -> point.x < 40);
        return panel;
    }

    private static void send(JPanel panel, int event, int x, int button) {
        panel.dispatchEvent(new MouseEvent(panel, event, 0, 0, x, 10, 1, false, button));
    }
}
