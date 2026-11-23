package coderoom.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.RoomObjectId;
import coderoom.entities.record.JdbcClearRecordRepository;
import coderoom.entities.user.LoginUser;
import coderoom.pages.game.GamePage;
import coderoom.shared.db.Database;
import coderoom.widgets.clue.CluePanel;
import coderoom.widgets.dialogue.DialoguePanel;
import coderoom.widgets.inventory.InventoryPanel;
import coderoom.widgets.room.RoomPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkshopFlowTest {
    @TempDir Path directory;

    @Test void engineDevicesCanBeSolvedThroughTheirActualControls() throws Exception {
        verifyRoom(3, page -> {
            edt(() -> {
                RoomPanel room = find(page, RoomPanel.class);
                var point      = room.screenPoint(303, 305);
                room.dispatchEvent(new MouseEvent(room, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                        0, point.x, point.y, 0, false));
                assertEquals("비상 전력 랙", room.getToolTipText());
                snapshot(page, "engine-rack-hover");
                return null;
            });
            evidence(page, RoomObjectId.EVIDENCE_1);
            evidence(page, RoomObjectId.EVIDENCE_3);
            evidence(page, RoomObjectId.EVIDENCE_5);
            edt(() -> {
                page.inspect(RoomObjectId.THIRD_DEVICE);
                assertFalse(dialogue(page).isVisible());
                assertTrue(find(page, InventoryPanel.class).isVisible());
                roomClick(page, 450, 345);
                assertFalse(dialogue(page).isVisible());
                assertPuzzle(page);
                return null;
            });
            edt(() -> {
                button(page, "module0").doClick();
                button(page, "module1").doClick();
                button(page, "module2").doClick();
                button(page, "module3").doClick();
                button(page, "workshopStart").doClick();
                assertPuzzle(page);
                button(page, "module1").doClick();
                snapshot(page, "engine-power-device");
                submit(page);
                startDevice(page, RoomObjectId.THIRD_DEVICE);
                JLabel feedback = (JLabel) named(page, "workshopFeedback");
                snapshot(page, "engine-distribution-start");
                var actuator = button(page, "breakerTrial").getModel();
                actuator.setArmed(true);
                actuator.setPressed(true);
                snapshot(page, "engine-distribution-pressed");
                actuator.setArmed(false);
                actuator.setPressed(false);
                button(page, "breakerB").doClick();
                button(page, "breakerTrial").doClick();
                assertEquals("냉각기에서 시험 중지 · 고장 장비", feedback.getText());
                assertAttempts(page, 3);
                button(page, "breakerB").doClick();
                for (String key : new String[] {"C", "A", "E"}) { button(page, "breaker" + key).doClick(); }
                snapshot(page, "engine-distribution-device");
                button(page, "breakerTrial").doClick();
                assertEquals("가동 시험 완료 · 남은 전력 0W", feedback.getText());
                assertSolvedReveal(page);
                startDevice(page, RoomObjectId.WALL_DIAL);
                assertTrue(find(page, CluePanel.class).isVisible());
                feedback = (JLabel) named(page, "workshopFeedback");
                snapshot(page, "engine-probe-start");
                button(page, "joint1").doClick();
                assertEquals("이음쇠 B · 닫힘 29", feedback.getText());
                button(page, "joint2").doClick();
                assertEquals("이음쇠 C · 닫힘 48", feedback.getText());
                button(page, "joint3").doClick();
                button(page, "probeValve").doClick();
                assertEquals("이음쇠 D · 열림 93", feedback.getText());
                button(page, "joint2").doClick();
                assertEquals("이음쇠 C · 열림 81", feedback.getText());
                Component board = named(page, "sleeveBoard");
                int width = board.getWidth();
                int tile = Math.max(24, Math.min(80, (width - 48) / 4));
                int sourceX = (width - 4 * (tile + 8)) / 2 + (tile + 8) + 4 + tile / 2;
                drag(board, sourceX, (int) (board.getHeight() * .21) + 9,
                        width / 2, (int) (board.getHeight() * .62) + 9);
                snapshot(page, "engine-pipe-device");
                button(page, "joint3").doClick();
                button(page, "workshopStart").doClick();
                assertPuzzle(page);
                assertAttempts(page, 2);
                button(page, "joint2").doClick();
                submit(page);
                assertAttempts(page, 2);
                startDevice(page, RoomObjectId.FOURTH_DEVICE);
                button(page, "leverA").doClick();
                button(page, "pressureTrial").doClick();
                var pressureFeedback = (JLabel) named(page, "workshopFeedback");
                assertEquals("레버 네 개를 모두 예약하세요.", pressureFeedback.getText());
                for (String key : new String[] {"B", "C", "D"}) {
                    button(page, "lever" + key).doClick();
                }
                button(page, "pressureTrial").doClick();
                assertEquals("안전 조건 불일치 · 예약 순서를 확인하세요.", pressureFeedback.getText());
                assertPuzzle(page);
                assertAttempts(page, 1);
                for (String key : new String[] {"A", "B", "C", "D", "D", "C", "A", "B"}) {
                    button(page, "lever" + key).doClick();
                }
                snapshot(page, "engine-pressure-device");
                if (GraphicsEnvironment.isHeadless()) {
                    page.setSize(900, 650);
                    snapshot(page, "engine-pressure-compact");
                    for (char lever : "ABCD".toCharArray()) {
                        var reading = (JLabel) named(page, "leverReading" + lever);
                        assertTrue(reading.getFontMetrics(reading.getFont()).stringWidth(reading.getText())
                                <= reading.getWidth(), reading.getText());
                    }
                    page.setSize(1280, 800);
                    layout(page);
                }
                button(page, "pressureTrial").doClick();
                assertSolvedReveal(page);
                startDevice(page, RoomObjectId.FIFTH_DEVICE);
                assertTrue(find(page, CluePanel.class).isVisible());
                wireInterlock(page, 2, 1, 0, 3);
                JLabel interlockFeedback = (JLabel) named(page, "workshopFeedback");
                assertEquals("가동 꺼짐 · 안전 꺼짐", interlockFeedback.getText());
                button(page, "faultTest").doClick();
                assertEquals("압력 신호 차단 · 가동 꺼짐 · 안전 꺼짐", interlockFeedback.getText());
                assertAttempts(page, 1);
                wireInterlock(page, 2, 0, 3, 1);
                assertEquals("가동 켜짐 · 안전 켜짐", interlockFeedback.getText());
                dragControl(page, "port3", "port1");
                assertEquals("가동 꺼짐 · 안전 꺼짐", interlockFeedback.getText());
                dragControl(page, "port1", "port3");
                assertEquals("가동 켜짐 · 안전 켜짐", interlockFeedback.getText());
                dragControl(page, "port3", "signal1");
                assertEquals(" ", interlockFeedback.getText());
                dragControl(page, "signal1", "port3");
                assertEquals("가동 켜짐 · 안전 켜짐", interlockFeedback.getText());
                snapshot(page, "engine-interlock");
                submit(page);
                return null;
            });
        });
    }

    @Test void greenhouseCanDrainAndConnectTheRailsBeforeCollectingTheSeed() throws Exception {
        verifyRoom(4, page -> {
            evidence(page, RoomObjectId.EVIDENCE_1);
            evidence(page, RoomObjectId.EVIDENCE_5);
            evidence(page, RoomObjectId.EVIDENCE_4);
            edt(() -> {
                Component diagram = named(page, "balanceDiagram");
                assertTrue(diagram.isShowing() || diagram.isVisible());
                snapshot(page, "greenhouse-balance-diagram");
                button(page, "clueViewToggle").doClick();
                assertFalse(diagram.isVisible());
                button(page, "clueViewToggle").doClick();
                assertTrue(diagram.isVisible());
                return null;
            });
            edt(() -> {
                startDevice(page, RoomObjectId.WALL_DIAL);
                assertTrue(find(page, CluePanel.class).isVisible());
                button(page, "flowTest").doClick();
                return null;
            });
            Thread.sleep(260);
            edt(() -> {
                assertPuzzle(page);
                assertAttempts(page, 4);
                snapshot(page, "greenhouse-gutter-leak");
                button(page, "gutter0").doClick();
                button(page, "gutter0").doClick();
                button(page, "gutter1").doClick();
                button(page, "gutter1").doClick();
                button(page, "gutter2").doClick();
                snapshot(page, "greenhouse-gutter-device");
                button(page, "flowTest").doClick();
                return null;
            });
            await(() -> edt(() -> dialogue(page).isVisible()));
            Thread.sleep(650);
            edt(() -> {
                snapshot(page, "greenhouse-drain-reveal");
                assertSolvedReveal(page);
                startDevice(page, RoomObjectId.THIRD_DEVICE);
                Component rail = named(page, "railPlatform");
                int width  = rail.getWidth();
                int height = rail.getHeight();
                snapshot(page, "greenhouse-platform-start");
                assertRailHit(rail, .62, .47);
                drag(rail, (int) (width * .62), (int) (height * .47),
                        (int) (width * .86), (int) (height * .47));
                button(page, "workshopStart").doClick();
                assertPuzzle(page);
                drag(rail, (int) (width * .62), (int) (height * .47),
                        (int) (width * .34), (int) (height * .47));
                drag(rail, (int) (width * .677), (int) (height * .23),
                        (int) (width * .677), (int) (height * .76));
                drag(rail, (int) (width * .41), (int) (height * .47),
                        (int) (width * .87), (int) (height * .47));
                button(page, "workshopStart").doClick();
                assertPuzzle(page);
                assertAttempts(page, 4);
                var answer = find(page, coderoom.features.gameplay.ui.AnswerInputPanel.class);
                button(answer, "×").doClick();
                snapshot(page, "greenhouse-bridge-partial");
                startDevice(page, RoomObjectId.THIRD_DEVICE);
                drag(rail, (int) (width * .225), (int) (height * .24),
                        (int) (width * .795), (int) (height * .75));
                button(page, "workshopStart").doClick();
                assertAttempts(page, 4);
                drag(rail, (int) (width * .225), (int) (height * .24),
                        (int) (width * .676), (int) (height * .474));
                button(page, "workshopStart").doClick();
                assertPuzzle(page);
                assertAttempts(page, 3);
                AbstractButton horizontal = button(page, "adjustHorizontalRail");
                AbstractButton vertical   = button(page, "adjustVerticalRail");
                assertControlHit(horizontal);
                assertControlHit(vertical);
                assertControlHit(button(page, "workshopStart"));
                assertEquals("레일 위치 조정", horizontal.getText());
                assertEquals("레일 위치 조정", vertical.getText());
                horizontal.doClick();
                vertical.doClick();
                vertical.doClick();
                var railFeedback = (JLabel) named(page, "workshopFeedback");
                assertEquals("레일이 맞았습니다. 황동 걸쇠를 누르세요.", railFeedback.getText());
                snapshot(page, "greenhouse-rail-platform");
                submit(page);
                return null;
            });
            Thread.sleep(1100);
            edt(() -> {
                snapshot(page, "greenhouse-bridge-connected");
                startDevice(page, RoomObjectId.DRAWER);
                var specimenEditor = find(find(page, CluePanel.class), javax.swing.JEditorPane.class);
                String specimenCard = specimenEditor.getText();
                assertTrue(specimenCard.contains("specimen-marks/serrated.png"));
                assertTrue(specimenCard.contains("specimen-marks/triangle.png"));
                assertTrue(specimenCard.contains("specimen-marks/damaged.png"));
                assertTrue(specimenCard.contains("specimen-marks/wave.png"));
                assertTrue(specimenCard.contains("||"));
                var specimenDocument = specimenEditor.getDocument();
                assertTrue(specimenDocument.getText(0, specimenDocument.getLength()).contains("!제외"));
                snapshot(page, "greenhouse-symbol-card");
                button(page, "seed1").doClick();
                button(page, "seed2").doClick();
                snapshot(page, "greenhouse-seed-device");
                submit(page);
                startDevice(page, RoomObjectId.FOURTH_DEVICE);
                snapshot(page, "greenhouse-scale-empty");
                dragWeight(page, "balanceWeight0", "balanceSlot1");
                button(page, "balanceClasp").doClick();
                assertPuzzle(page);
                assertAttempts(page, 3);
                dragWeight(page, "balanceWeight1", "balanceSlot3");
                button(page, "balanceClasp").doClick();
                assertAttempts(page, 2);
                dragWeight(page, "balanceWeight0", "balanceSlot2");
                return null;
            });
            Thread.sleep(410);
            edt(() -> {
                snapshot(page, "greenhouse-scale-device");
                button(page, "balanceClasp").doClick();
                return null;
            });
            await(() -> edt(() -> dialogue(page).isVisible()));
            edt(() -> {
                assertSolvedReveal(page);
                startDevice(page, RoomObjectId.FIFTH_DEVICE);
                assertTrue(find(page, CluePanel.class).isVisible());
                snapshot(page, "greenhouse-railway-wet");
                return null;
            });
            pushAndWait(page);
            edt(() -> {
                assertAttempts(page, 1);
                button(page, "firstTurn").doClick();
                return null;
            });
            pushAndWait(page);
            pushAndWait(page);
            edt(() -> {
                button(page, "firstTurn").doClick();
                return null;
            });
            pushAndWait(page);
            edt(() -> {
                button(page, "secondTurn").doClick();
                button(page, "pushTrolley").doClick();
                button(page, "workshopStart").doClick();
                assertPuzzle(page);
                return null;
            });
            await(() -> edt(() -> button(page, "pushTrolley").isEnabled()));
            edt(() -> {
                snapshot(page, "greenhouse-trolley-device");
                submit(page);
                return null;
            });
        });
    }

    private static void assertRailHit(Component rail, double x, double y) {
        var point = SwingUtilities.convertPoint(rail, (int) (rail.getWidth() * x),
                (int) (rail.getHeight() * y), rail.getParent());
        assertEquals(rail, SwingUtilities.getDeepestComponentAt(rail.getParent(), point.x, point.y));
    }

    private static void assertControlHit(AbstractButton button) {
        var point = SwingUtilities.convertPoint(button, button.getWidth() / 2, button.getHeight() / 2, button.getParent());
        assertEquals(button, SwingUtilities.getDeepestComponentAt(button.getParent(), point.x, point.y), button.getName());
    }

    private static void assertAttempts(GamePage page, int remaining) {
        assertEquals("남은 오답 기회 " + remaining + "/5",
                ((javax.swing.JComponent) named(page, "wrongAttempts")).getAccessibleContext().getAccessibleName());
    }

    private static void pushAndWait(GamePage page) throws Exception {
        edt(() -> {
            button(page, "pushTrolley").doClick();
            return null;
        });
        await(() -> edt(() -> button(page, "pushTrolley").isEnabled()));
    }

    private void verifyRoom(long mapId, Scenario scenario) throws Exception {
        Database db = new Database(directory.resolve("workshop-" + mapId + ".db").toString());
        db.initialize();
        JFrame[] frame = new JFrame[1];
        GamePage page  = edt(() -> {
            var user = new LoginUser(4, "test");
            GamePage mounted = Main.createScreens(db).game(user, mapId, () -> {}, () -> {});
            if (!GraphicsEnvironment.isHeadless()) {
                frame[0] = new JFrame("CODE ROOM QA");
                frame[0].setContentPane(mounted);
                frame[0].setSize(1280, 800);
                frame[0].setVisible(true);
            }
            mounted.setSize(1280, 800);
            layout(mounted);
            mounted.load(user, mapId);
            return mounted;
        });
        try {
            await(() -> edt(page::isTimerRunning));
            edt(() -> {
                assertTrue(dialogue(page).isVisible());
                assertFalse(find(page, InventoryPanel.class).isVisible());
                snapshot(page, "room-" + mapId + "-dialogue");
                dialogue(page).advance();
                assertTrue(dialogue(page).isVisible());
                snapshot(page, "room-" + mapId + "-player");
                dialogue(page).advance();
                assertFalse(dialogue(page).isVisible());
                assertTrue(find(page, InventoryPanel.class).isVisible());
                snapshot(page, "room-" + mapId + "-start");
                return null;
            });
            scenario.run(page);
            edt(() -> {
                dialogue(page).close();
                snapshot(page, "room-" + mapId + "-ready");
                page.useKey();
                assertFalse(page.isTimerRunning());
                assertFalse(find(page, coderoom.widgets.result.ResultPanel.class).getParent().isVisible());
                return null;
            });
            JdbcClearRecordRepository records = new JdbcClearRecordRepository(db);
            await(() -> records.findByUserId(4).stream().anyMatch(record -> record.getMapId() == mapId));
            edt(() -> {
                assertTrue(find(page, coderoom.widgets.result.ResultPanel.class).getParent().isVisible());
                return null;
            });
        } finally {
            edt(() -> {
                page.disposeGame();
                if (frame[0] != null) { frame[0].dispose(); }
                return null;
            });
        }
    }

    private static void evidence(GamePage page, RoomObjectId clue) throws Exception {
        edt(() -> { page.inspect(clue); return null; });
        edt(() -> {
            assertTrue(find(page, CluePanel.class).isVisible());
            assertFalse(dialogue(page).isVisible());
            return null;
        });
    }

    private static void startDevice(GamePage page, RoomObjectId object) {
        dialogue(page).close();
        page.inspect(object);
        assertPuzzle(page);
    }

    private static void assertPuzzle(GamePage page) {
        layout(page);
        assertFalse(dialogue(page).isVisible());
        Component clue   = find(page, CluePanel.class);
        Component answer = find(page, coderoom.features.gameplay.ui.AnswerInputPanel.class);
        assertTrue(answer.isVisible());
        assertTrue(answer.getX() > clue.getX() + clue.getWidth());
        assertTrue(named(answer, "workshopPuzzle").isVisible());
    }

    private static void submit(GamePage page) {
        button(page, "workshopStart").doClick();
        assertSolvedReveal(page);
    }

    private static void assertSolvedReveal(GamePage page) {
        assertFalse(find(page, coderoom.features.gameplay.ui.AnswerInputPanel.class).isVisible());
        assertTrue(dialogue(page).isVisible());
        dialogue(page).close();
    }

    private static void wireInterlock(GamePage page, int... signals) {
        for (int port = 0; port < signals.length; port++) {
            dragControl(page, "signal" + signals[port], "port" + port);
        }
    }

    private static void dragControl(GamePage page, String source, String destination) {
        AbstractButton from = button(page, source);
        AbstractButton to   = button(page, destination);
        var center = SwingUtilities.convertPoint(to, to.getWidth() / 2, to.getHeight() / 2, from);
        drag(from, from.getWidth() / 2, from.getHeight() / 2, center.x, center.y);
    }

    private static void dragWeight(GamePage page, String weightKey, String hookKey) {
        Component weight = named(page, weightKey);
        Component hook   = named(page, hookKey);
        var end = SwingUtilities.convertPoint(hook, hook.getWidth() / 2, hook.getHeight() / 2, weight);
        drag(weight, weight.getWidth() / 2, weight.getHeight() / 2, end.x, end.y);
    }

    private static void roomClick(GamePage page, int x, int y) {
        RoomPanel room = find(page, RoomPanel.class);
        java.awt.Point point = room.screenPoint(x, y);
        drag(room, point.x, point.y, point.x, point.y);
    }

    private static void drag(Component target, int x, int y, int endX, int endY) {
        long time = System.currentTimeMillis();
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, time,
                InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_DRAGGED, time,
                InputEvent.BUTTON1_DOWN_MASK, endX, endY, 0, false, MouseEvent.NOBUTTON));
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, time,
                0, endX, endY, 1, false, MouseEvent.BUTTON1));
    }

    private static DialoguePanel dialogue(GamePage page) { return find(page, DialoguePanel.class); }

    private static AbstractButton button(Container root, String key) {
        Component result = locate(root, component -> component instanceof AbstractButton button
                && (key.equals(button.getName()) || key.equals(button.getText())));
        assertNotNull(result, key);
        return (AbstractButton) result;
    }

    private static Component named(Component root, String name) {
        Component result = locate(root, component -> name.equals(component.getName()));
        assertNotNull(result, name);
        return result;
    }

    private static <T> T find(Component root, Class<T> type) {
        return type.cast(locate(root, type::isInstance));
    }

    private static Component locate(Component root, java.util.function.Predicate<Component> match) {
        if (match.test(root)) { return root; }
        if (root instanceof Container parent) {
            for (Component child : parent.getComponents()) {
                Component result = locate(child, match);
                if (result != null) { return result; }
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) { layout(container); }
        }
    }

    private static void snapshot(GamePage page, String name) throws Exception {
        layout(page);
        BufferedImage image = new BufferedImage(page.getWidth(), page.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        page.paint(graphics);
        graphics.dispose();
        assertTrue(ImageIO.write(image, "png", Path.of("target", name + ".png").toFile()));
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void await(Callable<Boolean> ready) throws Exception {
        long deadline = System.nanoTime() + 6_000_000_000L;
        while (!ready.call() && System.nanoTime() < deadline) { Thread.sleep(20); }
        assertTrue(ready.call());
    }

    @FunctionalInterface
    private interface Scenario { void run(GamePage page) throws Exception; }
}
