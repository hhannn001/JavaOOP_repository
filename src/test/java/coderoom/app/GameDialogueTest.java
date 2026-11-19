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
                page[0].submit(RoomObjectId.DRAWER, "0000");
                assertTrue(text.getText().contains("아닌 것"));
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
                for (int width : new int[] {900, 1280}) {
                    page[0].setSize(width, 800);
                    layout(page[0]);
                    assertTrue(dialogue.getHeight() <= 200);
                    assertTrue(text.getY() >= 60);
                    assertTrue(text.getHeight() >= text.getFontMetrics(text.getFont()).getHeight() * 2);
                    assertTrue(text.getWidth() > 600);
                    coderoom.features.gameplay.ui.AnswerInputPanel puzzle = find(page[0], coderoom.features.gameplay.ui.AnswerInputPanel.class);
                    assertFalse(puzzle.getBounds().intersects(dialogue.getBounds()));
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
                coderoom.features.gameplay.ui.AnswerInputPanel puzzle = find(page[0], coderoom.features.gameplay.ui.AnswerInputPanel.class);
                assertTrue(puzzle.isVisible());
                assertTrue(find(puzzle, javax.swing.JTextField.class).isEnabled());
                assertTrue(find(puzzle, javax.swing.JTextField.class).getText().equals("0"));
                dialogue.showLines(new DialoguePanel.Line(DialoguePanel.Speaker.SYSTEM,
                        "출입문 위 장치가 켜진다. ‘보안 시험 시작. 제한시간은 3분입니다. 메모의 코드로 서랍과 금고를 열고, 열쇠를 찾아 탈출하십시오.’"));
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
