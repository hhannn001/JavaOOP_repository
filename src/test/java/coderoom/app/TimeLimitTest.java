package coderoom.app;

import coderoom.entities.game.GameSession;
import coderoom.entities.game.GameStage;
import coderoom.entities.game.ItemId;
import coderoom.features.gameplay.GameView;
import coderoom.pages.game.GamePage;
import coderoom.widgets.result.ResultPanel;
import coderoom.widgets.result.TimeLimitBar;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeLimitTest {
    @Test void starsFollowThirdsOfConfiguredLimit() {
        for (long limit : new long[] {180, 300, 90}) {
            assertEquals(3, ResultPanel.starCount(0, limit, true));
            assertEquals(3, ResultPanel.starCount(limit / 3, limit, true));
            assertEquals(2, ResultPanel.starCount(limit / 3 + 1, limit, true));
            assertEquals(2, ResultPanel.starCount(limit * 2 / 3, limit, true));
            assertEquals(1, ResultPanel.starCount(limit * 2 / 3 + 1, limit, true));
            assertEquals(1, ResultPanel.starCount(limit - 1, limit, true));
            assertEquals(0, ResultPanel.starCount(limit, limit, true));
            assertEquals(0, ResultPanel.starCount(0, limit, false));
        }
    }

    @Test void escapeMustPrecedeDeadline() {
        GameSession early = readySession();
        GameSession late  = readySession();
        assertTrue(early.escape(599_999_999_999L));
        assertTrue(early.escape(610_000_000_000L));
        assertFalse(late.escape(600_000_000_000L));
    }

    private GameSession readySession() {
        GameSession session = new GameSession(1, 1, 0);
        session.unlockDrawer();
        session.unlockSafe();
        session.select(ItemId.KEY);
        return session;
    }

    @Test void timeoutShowsFailureWithoutSaving() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
            AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
            GamePage page                    = new GamePage(game, widgets);
            page.render(new GameView(GameStage.START, List.of(), null, 599));
            assertNull(widgets.escape);
            page.render(new GameView(GameStage.START, List.of(), null, 600));
            assertNotNull(widgets.escape);
            assertNull(widgets.escape.record());
            assertFalse(page.isTimerRunning());
            page.useKey();
            page.retrySave();
            assertTrue(game.saved.isEmpty());
            page.disposeGame();
        });
    }

    @Test void exhaustedAttemptsShowFailureWithoutSaving() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AIntegrationStubs.Game game       = new AIntegrationStubs.Game();
            AIntegrationStubs.Widgets widgets = new AIntegrationStubs.Widgets();
            GamePage page                     = new GamePage(game, widgets);
            page.render(new GameView(GameStage.START, List.of(), null, 18, 0, 2,
                    false, null, Set.of(), Set.of(), Set.of(), 'B', 1, 0));
            assertNotNull(widgets.escape);
            assertNull(widgets.escape.record());
            assertTrue(widgets.escape.message().contains("탈락"));
            assertEquals(GamePage.SaveState.NONE, widgets.state);
            assertFalse(page.isTimerRunning());
            page.retrySave();
            assertTrue(game.saved.isEmpty());
            page.disposeGame();
        });
    }

    @Test void resultAndTimerPaintInBothStates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ResultPanel result = new ResultPanel();
            result.setSize(620, 350);
            result.render(97, ResultPanel.SaveState.SAVED);
            paint(result);
            result.renderFailure();
            paint(result);
            TimeLimitBar timer = new TimeLimitBar();
            timer.setSize(360, 48);
            for (long elapsed : new long[] {0, 300, 600, 620}) {
                timer.setElapsedSeconds(elapsed);
                paint(timer);
            }
        });
    }

    private void paint(javax.swing.JComponent component) {
        component.doLayout();
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        component.paint(graphics);
        graphics.dispose();
    }
}
