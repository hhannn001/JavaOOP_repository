package coderoom.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.RoomObjectId;
import coderoom.entities.user.LoginUser;
import coderoom.pages.game.GamePage;
import coderoom.shared.db.Database;
import coderoom.widgets.dialogue.DialoguePanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import javax.imageio.ImageIO;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameDialogueTest {
    @TempDir Path directory;

    @Test void storyAndReactionsFollowGameplayAndTimerDoesNotResetDialogue() throws Exception {
        Database database = new Database(directory.resolve("story.db").toString());
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
                DialoguePanel dialogue = find(page[0], DialoguePanel.class);
                JTextArea text         = find(dialogue, JTextArea.class);
                assertTrue(dialogue.isVisible());
                assertTrue(text.getText().contains("서재"));
                for (int i = 0; i < 4; i++) { dialogue.advance(); }
                assertFalse(dialogue.isVisible());
                page[0].inspect(RoomObjectId.WINDOW);
                assertTrue(text.getText().contains("못 나갈"));
                page[0].inspect(RoomObjectId.DRAWER);
                var answer = find(page[0], coderoom.features.gameplay.ui.AnswerInputPanel.class);
                assertFalse(dialogue.isVisible());
                page[0].setSize(1280, 800);
                layout(page[0]);
                var lock = find(answer, javax.swing.JTextField.class).getParent();
                var pull = java.util.Arrays.stream(lock.getComponents())
                        .filter(child -> "studyLockLatch".equals(child.getName())).findFirst().orElseThrow();
                var before = pull.getLocation();
                page[0].submit(RoomObjectId.DRAWER, "0000");
                assertFalse(dialogue.isVisible());
                assertFalse(before.equals(pull.getLocation()));
                page[0].submit(RoomObjectId.DRAWER, "2312");
                assertTrue(text.getText().contains("두 번째 메모"));
                dialogue.advance();
                assertTrue(text.getText().contains("벽의 금고"));
            });
            Thread.sleep(350);
            SwingUtilities.invokeAndWait(() -> {
                DialoguePanel dialogue = find(page[0], DialoguePanel.class);
                JTextArea text         = find(dialogue, JTextArea.class);
                assertTrue(text.getText().contains("벽의 금고"));
                dialogue.advance();
                assertFalse(dialogue.isVisible());
                page[0].useKey();
                assertTrue(text.getText().contains("열쇠"));
                page[0].inspect(RoomObjectId.WALL_DIAL);
                var clue   = find(page[0], coderoom.widgets.clue.CluePanel.class);
                var puzzle = find(page[0], coderoom.features.gameplay.ui.AnswerInputPanel.class);
                assertTrue(clue.isVisible());
                assertFalse(dialogue.isVisible());
                for (int width : new int[] {900, 1280, 2512}) {
                    page[0].setSize(width, 800);
                    dialogue.close();
                    layout(page[0]);
                    var initialClueBounds = clue.getBounds();
                    dialogue.showLines(new DialoguePanel.Line(DialoguePanel.Speaker.SYSTEM, "조작을 확인하세요."));
                    layout(page[0]);
                    org.junit.jupiter.api.Assertions.assertEquals(initialClueBounds.x, clue.getX());
                    org.junit.jupiter.api.Assertions.assertEquals(initialClueBounds.width, clue.getWidth());
                    org.junit.jupiter.api.Assertions.assertEquals(puzzle.getY() + puzzle.puzzleContentTop(),
                            clue.getY() + clue.paperTopInset(clue.getHeight()));
                    assertFalse(clue.getBounds().intersects(dialogue.getBounds()));
                    if (width == 900) {
                        var memoScroll = find(clue, javax.swing.JScrollPane.class);
                        assertTrue(memoScroll.getVerticalScrollBar().isVisible());
                        assertTrue(memoScroll.getVerticalScrollBar().getMaximum()
                                > memoScroll.getVerticalScrollBar().getVisibleAmount());
                    }
                    var inventory = find(page[0], coderoom.widgets.inventory.InventoryPanel.class);
                    var inventoryBounds = inventory.getParent().getBounds();
                    assertFalse(inventory.getParent().isVisible());
                    assertFalse(inventory.isVisible());
                    assertTrue(dialogue.getBounds().contains(inventoryBounds));
                    assertTrue(dialogue.getParent().getComponentZOrder(dialogue)
                            < dialogue.getParent().getComponentZOrder(inventory.getParent()));
                    assertTrue(dialogue.getHeight() <= 200);
                    assertTrue(text.getY() >= 60);
                    assertTrue(text.getHeight() >= text.getFontMetrics(text.getFont()).getHeight() * 2);
                    assertTrue(text.getWidth() > 600);
                    assertFalse(puzzle.getBounds().intersects(dialogue.getBounds()));
                    assertTrue(clue.isVisible());
                    assertTrue(puzzle.getX() > clue.getX() + clue.getWidth());
                    assertFalse(find(puzzle, javax.swing.JTextField.class).isEnabled());
                    BufferedImage image = new BufferedImage(width, 800, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = image.createGraphics();
                    page[0].paint(graphics);
                    graphics.dispose();
                    try {
                        ImageIO.write(image, "png", Path.of("target/dialogue-preview-" + width + ".png").toFile());
                    } catch (java.io.IOException exception) {
                        throw new java.io.UncheckedIOException(exception);
                    }
                }
                javax.swing.JPanel surface = (javax.swing.JPanel) java.util.Arrays.stream(dialogue.getParent().getComponents())
                        .filter(child -> "dialogueClickSurface".equals(child.getName())).findFirst().orElseThrow();
                surface.dispatchEvent(new java.awt.event.MouseEvent(surface, java.awt.event.MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(), 0, 5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1));
                assertFalse(dialogue.isVisible());
                assertFalse(surface.isVisible());
                assertTrue(puzzle.isVisible());
                assertTrue(find(puzzle, javax.swing.JTextField.class).isEnabled());
                assertTrue(find(puzzle, javax.swing.JTextField.class).getText().equals("0"));
                dialogue.showLines(new DialoguePanel.Line(DialoguePanel.Speaker.SYSTEM,
                        "출입문 위 장치가 켜진다. ‘보안 시험 시작. 제한시간은 10분입니다. 메모의 코드로 서랍과 금고를 열고, 열쇠를 찾아 탈출하십시오.’"));
                page[0].setSize(900, 650);
                layout(page[0]);
                assertTrue(text.getPreferredSize().height <= text.getHeight(), "긴 대사도 본문 영역 안에 표시되어야 한다.");
            });
        } finally {
            SwingUtilities.invokeAndWait(page[0]::disposeGame);
        }
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) { layout(container); }
        }
    }

    private static <T> T find(Component component, Class<T> type) {
        if (type.isInstance(component)) { return type.cast(component); }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = find(child, type);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
