package coderoom.features.gameplay.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.WorkshopRoom;
import coderoom.features.gameplay.PuzzleVariant;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class WorkshopVariantTest {
    @Test void everyVariantUsesTheSamePlatformSequence() throws Exception {
        edt(() -> {
            for (var variant : PuzzleVariant.values()) {
                var panel = new WorkshopPanel(WorkshopRoom.GREENHOUSE, 2, variant, 0);
                var rails = (RailPlatformPanel) find(panel, "railPlatform");
                key(rails, KeyEvent.VK_RIGHT);
                assertEquals("", panel.answer());
                key(rails, KeyEvent.VK_LEFT);
                key(rails, KeyEvent.VK_SPACE);
                key(rails, KeyEvent.VK_DOWN);
                assertEquals("", panel.answer());
                key(rails, KeyEvent.VK_SPACE);
                key(rails, KeyEvent.VK_RIGHT);
                assertEquals("", panel.answer());
                key(rails, KeyEvent.VK_RIGHT);
                assertEquals("", panel.answer());
                var horizontal = (AbstractButton) find(panel, "adjustHorizontalRail");
                var vertical   = (AbstractButton) find(panel, "adjustVerticalRail");
                horizontal.doClick(0);
                vertical.doClick(0);
                assertEquals("", panel.answer());
                vertical.doClick(0);
                assertEquals("AB", panel.answer());
                panel.reset();
                assertEquals("", panel.answer());
            }
            return null;
        });
    }

    @Test void cartCannotLeaveTheDockWithoutTheConnectedCrossing() throws Exception {
        edt(() -> {
            var panel = new WorkshopPanel(WorkshopRoom.GREENHOUSE, 4, PuzzleVariant.STANDARD, 0);
            assertFalse(find(panel, "pushTrolley").isEnabled());
            assertFalse(find(panel, "workshopStart").isEnabled());
            assertEquals("", panel.answer());
            return null;
        });
    }

    @Test void emptyCartTestsDifferentRoutesBeforeUsingTheClearedMainLine() throws Exception {
        for (var variant : new PuzzleVariant[] {PuzzleVariant.COPPER, PuzzleVariant.BRASS}) {
            var panel = edt(() -> new WorkshopPanel(WorkshopRoom.GREENHOUSE, 4, variant, 1));
            try {
                if (variant == PuzzleVariant.BRASS) {
                    click(panel, "firstTurn");
                    click(panel, "secondTurn");
                }
                pushAndWait(panel);
                pushAndWait(panel);
                assertEquals("", edt(panel::answer));
                click(panel, variant == PuzzleVariant.COPPER ? "secondTurn" : "firstTurn");
                pushAndWait(panel);
                pushAndWait(panel);
                assertEquals(variant == PuzzleVariant.COPPER ? "AAAB" : "BBAB", edt(panel::answer));
            } finally {
                edt(() -> { panel.cancel(); return null; });
            }
        }
    }

    private static void pushAndWait(WorkshopPanel panel) throws Exception {
        click(panel, "pushTrolley");
        long deadline = System.nanoTime() + 4_000_000_000L;
        while (!edt(() -> find(panel, "pushTrolley").isEnabled())) {
            assertTrue(System.nanoTime() < deadline, "Cart did not finish moving");
            Thread.sleep(20);
        }
    }

    private static void click(Container parent, String name) throws Exception {
        edt(() -> { ((AbstractButton) find(parent, name)).doClick(0); return null; });
    }

    private static void key(Component component, int code) {
        var event = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED);
        for (var listener : component.getKeyListeners()) { listener.keyPressed(event); }
    }

    private static Component find(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) { return child; }
            if (child instanceof Container container) {
                Component nested = find(container, name);
                if (nested != null) { return nested; }
            }
        }
        return null;
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
