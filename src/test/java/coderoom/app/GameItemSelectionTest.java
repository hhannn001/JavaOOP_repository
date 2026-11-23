package coderoom.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.user.LoginUser;
import coderoom.pages.game.GamePage;
import coderoom.shared.db.Database;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameItemSelectionTest {
    @TempDir Path directory;

    @Test void selectingKeyClosesMemoAndNeverReopensIt() throws Exception {
        Database database = new Database(directory.resolve("items.db").toString());
        database.initialize();
        GamePage[] page = new GamePage[1];
        SwingUtilities.invokeAndWait(() -> {
            page[0] = Main.createScreens(database).game(new LoginUser(4, "test"), 1, () -> {}, () -> {});
            page[0].load(new LoginUser(4, "test"), 1);
        });
        try {
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (true) {
                FutureTask<Boolean> ready = new FutureTask<>(page[0]::isTimerRunning);
                SwingUtilities.invokeAndWait(ready);
                if (ready.get()) { break; }
                assertTrue(System.nanoTime() < deadline);
                Thread.sleep(10);
            }
            SwingUtilities.invokeAndWait(() -> {
                page[0].submit(RoomObjectId.DRAWER, "2312");
                page[0].submit(RoomObjectId.WALL_DIAL, "423312133");
                ((coderoom.widgets.dialogue.DialoguePanel) find(page[0], "gameDialogue")).close();
                page[0].selectItem(ItemId.NOTE2);
                Component clue = find(page[0], "gameClue");
                assertTrue(clue.isVisible());
                Component progress = find(page[0], "puzzleProgress");
                Component attempts = find(page[0], "wrongAttempts");
                var room = clue.getParent();
                room.setSize(900, 650);
                room.doLayout();
                assertTrue(progress.isVisible());
                assertTrue(attempts.isVisible());
                assertFalse(clue.getBounds().intersects(progress.getBounds()));
                assertFalse(clue.getBounds().intersects(attempts.getBounds()));
                assertTrue(attempts.getY() - progress.getY() - progress.getHeight() >= 18);
                JButton key = (JButton) find(page[0], "열쇠");
                key.doClick();
                assertFalse(clue.isVisible());
                key.doClick();
                assertFalse(clue.isVisible());
                ((JButton) find(page[0], "메모 1")).doClick();
                assertTrue(clue.isVisible());
                page[0].selectItem(ItemId.KEY);
                assertFalse(clue.isVisible());
            });
        } finally {
            SwingUtilities.invokeAndWait(page[0]::disposeGame);
        }
    }

    private static Component find(Component component, String name) {
        if (name.equals(component.getName()) || component instanceof JButton button && name.equals(button.getText())) {
            return component;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = find(child, name);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
