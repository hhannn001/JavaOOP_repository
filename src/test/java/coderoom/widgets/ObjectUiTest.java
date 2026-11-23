package coderoom.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.game.WorkshopRoom;
import coderoom.entities.map.JdbcMapRepository;
import coderoom.entities.puzzle.JdbcPuzzleRepository;
import coderoom.entities.record.JdbcClearRecordRepository;
import coderoom.features.gameplay.GameFacade;
import coderoom.features.gameplay.ItemView;
import coderoom.features.gameplay.PuzzleVariant;
import coderoom.features.gameplay.ui.AnswerInputPanel;
import coderoom.shared.db.Database;
import coderoom.shared.ui.GameOverlayLayout;
import coderoom.widgets.clue.CluePanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.View;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ObjectUiTest {
    @Test void puzzleInputsSurviveClosingAndSwitchingButResetForNewSession() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AnswerInputPanel panel = new AnswerInputPanel();
            List<JTextField> fields = new ArrayList<>();
            collect(panel, fields);
            fields.get(0).setText("12");
            panel.setVisible(false);
            panel.showFourDigits();
            fields.clear();
            collect(panel, fields);
            assertEquals("12", fields.get(0).getText());
            panel.showGrid3x3();
            fields.clear();
            collect(panel, fields);
            for (int i = 0; i < 9; i++) { fields.get(i).setText(Integer.toString(i)); }
            fields.get(4).setText("");
            panel.setVisible(false);
            panel.showGrid3x3();
            panel.showFourDigits();
            fields.clear();
            collect(panel, fields);
            assertEquals("12", fields.get(0).getText());
            panel.showGrid3x3();
            fields.clear();
            collect(panel, fields);
            for (int i = 0; i < 9; i++) {
                assertEquals(i == 4 ? "" : Integer.toString(i), fields.get(i).getText());
            }
            AnswerInputPanel fresh = new AnswerInputPanel();
            fields.clear();
            collect(fresh, fields);
            assertEquals("0000", fields.get(0).getText());
            fresh.showGrid3x3();
            fields.clear();
            collect(fresh, fields);
            assertTrue(fields.stream().allMatch(field -> "0".equals(field.getText())));
        });
    }

    @Test void everyPlayablePuzzleMemoRemainsReadableWithoutShowingHints(@TempDir Path temp) throws Exception {
        var database = new Database(temp.resolve("memo.db").toString());
        database.initialize();
        var puzzles = new JdbcPuzzleRepository(database);
        var game = new GameFacade(new JdbcMapRepository(database),
                puzzles, new JdbcClearRecordRepository(database));
        var study = game.start(4, game.loadMap(1), 0);
        var notes = new ArrayList<ItemView>(study.items());
        game.submit(RoomObjectId.DRAWER, "2312");
        notes.add(game.view(0).items().get(1));
        for (int map : new int[] {3, 4}) {
            for (var puzzle : puzzles.findByMapId(map)) {
                var task = WorkshopRoom.forMap(map).task(puzzle.stepNo() - 1);
                ItemView.Diagram diagram = ItemView.Diagram.forPuzzle(puzzle.puzzleId());
                notes.add(new ItemView(ItemId.NOTE1, task.title(), puzzle.code(), "숨길 힌트", diagram));
            }
        }
        assertEquals(12, notes.size());

        SwingUtilities.invokeAndWait(() -> {
            JPanel room            = new JPanel(new GameOverlayLayout(true));
            CluePanel clue         = new CluePanel();
            JPanel dialogue        = new JPanel();
            JPanel progress        = new JPanel();
            JPanel attempts        = new JPanel();
            AnswerInputPanel input = new AnswerInputPanel();
            room.add(clue, "clue");
            room.add(dialogue, "dialogue");
            room.add(progress, "progress");
            room.add(attempts, "attempts");
            room.add(input, "input");
            clue.openClue();
            assertTrue(descendants(clue).stream().noneMatch(JScrollPane.class::isInstance));
            assertTrue(descendants(clue).stream().noneMatch(child -> "revealHint".equals(child.getName())));
            var reader = descendants(clue).stream().filter(JEditorPane.class::isInstance)
                    .map(JEditorPane.class::cast).findFirst().orElseThrow();
            for (var size : List.of(new Dimension(900, 650), new Dimension(1280, 800), new Dimension(1600, 900))) {
                room.setSize(size);
                for (boolean speaking : new boolean[] {false, true}) {
                    dialogue.setVisible(speaking);
                    input.setVisible(!speaking);
                    for (int index = 0; index < notes.size(); index++) {
                        ItemView note = notes.get(index);
                        clue.setClueData(note);
                        if (index == 0) {
                            input.showFourDigits();
                        } else if (index == 1) {
                            input.showGrid3x3();
                        } else {
                            WorkshopRoom workshop = index < 7 ? WorkshopRoom.ENGINE : WorkshopRoom.GREENHOUSE;
                            input.showWorkshop(workshop, (index - 2) % 5, PuzzleVariant.STANDARD, 1);
                        }
                        JButton toggle = descendants(clue).stream().filter(child -> "clueViewToggle".equals(child.getName()))
                                .map(JButton.class::cast).findFirst().orElseThrow();
                        boolean hasDiagram = note.diagram() != ItemView.Diagram.NONE;
                        assertEquals(hasDiagram, toggle.isVisible(), note.title());
                        if (hasDiagram) {
                            boolean startsWithDiagram = note.diagram() == ItemView.Diagram.BALANCE;
                            if (!startsWithDiagram) { toggle.doClick(0); }
                            layoutTree(room);
                            String diagramName = startsWithDiagram ? "balanceDiagram" : "puzzleDiagram";
                            var diagram = descendants(clue).stream().filter(child -> diagramName.equals(child.getName()))
                                    .map(JPanel.class::cast).findFirst().orElseThrow();
                            assertTrue(diagram.isVisible(), note.title());
                            assertTrue(firstOpaqueRow(diagram) < diagram.getHeight(), note.title());
                            if (input.isVisible()) { assertPuzzleEdgesMatch(clue, input, note.title()); }
                            toggle.doClick(0);
                            assertFalse(diagram.isVisible());
                        }
                        layoutTree(room);
                        assertFalse(clue.getBounds().intersects(progress.getBounds()));
                        assertFalse(clue.getBounds().intersects(attempts.getBounds()));
                        if (input.isVisible()) {
                            assertPuzzleEdgesMatch(clue, input, note.title());
                        }
                        var insets = reader.getInsets();
                        int width  = reader.getWidth() - insets.left - insets.right;
                        int height = reader.getHeight() - insets.top - insets.bottom;
                        View root  = reader.getUI().getRootView(reader);
                        root.setSize(width, height);
                        assertTrue(root.getMinimumSpan(View.X_AXIS) <= width,
                                () -> note.title() + " width " + root.getMinimumSpan(View.X_AXIS) + " > " + width);
                        boolean scrollable = reader.getParent() instanceof javax.swing.JViewport viewport
                                && viewport.getParent() instanceof JScrollPane pane && pane.getVerticalScrollBar().isVisible();
                        assertTrue(root.getPreferredSpan(View.Y_AXIS) <= height || scrollable,
                                () -> note.title() + " height " + root.getPreferredSpan(View.Y_AXIS) + " > " + height);
                    }
                }
            }
        });
    }

    @Test void variantDiagramsFollowTheCurrentClueData(@TempDir Path temp) throws Exception {
        Database database = new Database(temp.resolve("variants.db").toString());
        database.initialize();
        List<ItemView> clues = new ArrayList<>();
        String[] drawerCodes = {"2312", "3241", "1425"};
        for (PuzzleVariant variant : PuzzleVariant.values()) {
            GameFacade game = new GameFacade(new JdbcMapRepository(database), new JdbcPuzzleRepository(database),
                    new JdbcClearRecordRepository(database), ignored -> variant);
            game.start(4, game.loadMap(1), 0);
            game.submit(RoomObjectId.DRAWER, drawerCodes[variant.ordinal()]);
            clues.add(game.view(0).items().get(1));
            game.start(4, game.loadMap(3), 0);
            game.inspect(RoomObjectId.EVIDENCE_3);
            clues.add(game.view(0).items().get(0));
            game.start(4, game.loadMap(4), 0);
            game.inspect(RoomObjectId.EVIDENCE_1);
            game.inspect(RoomObjectId.EVIDENCE_4);
            clues.addAll(game.view(0).items());
        }
        SwingUtilities.invokeAndWait(() -> {
            var drawings = new java.util.EnumMap<ItemView.Diagram, java.util.Set<Integer>>(ItemView.Diagram.class);
            CluePanel clue = new CluePanel();
            for (ItemView note : clues) {
                clue.setClueData(note);
                boolean balance = note.diagram() == ItemView.Diagram.BALANCE;
                if (!balance) {
                    descendants(clue).stream().filter(child -> "clueViewToggle".equals(child.getName()))
                            .map(JButton.class::cast).findFirst().orElseThrow().doClick(0);
                }
                String name       = balance ? "balanceDiagram" : "puzzleDiagram";
                Component diagram = descendants(clue).stream().filter(child -> name.equals(child.getName()))
                        .findFirst().orElseThrow();
                diagram.setSize(320, 240);
                BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
                var graphics        = image.createGraphics();
                diagram.printAll(graphics);
                graphics.dispose();
                int hash = java.util.Arrays.hashCode(image.getRGB(0, 0, 320, 240, null, 0, 320));
                drawings.computeIfAbsent(note.diagram(), ignored -> new java.util.HashSet<>()).add(hash);
            }
            assertEquals(4, drawings.size());
            assertEquals(3, drawings.get(ItemView.Diagram.GRID).size());
            assertEquals(3, drawings.get(ItemView.Diagram.DISTRIBUTION).size());
            assertEquals(3, drawings.get(ItemView.Diagram.BALANCE).size());
            assertEquals(3, drawings.get(ItemView.Diagram.SEEDS).size());
        });
    }

    private static void assertPuzzleEdgesMatch(CluePanel clue, AnswerInputPanel input, String title) {
        int paperTop  = clue.getY() + firstOpaqueRow(clue);
        int deviceTop = input.getY() + firstOpaqueRow(input);
        assertTrue(Math.abs(paperTop - deviceTop) <= 3,
                () -> title + " paper top " + paperTop + ", device top " + deviceTop);
    }

    private static int firstOpaqueRow(JPanel panel) {
        int width           = panel.getWidth();
        BufferedImage image = new BufferedImage(width, panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics        = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        for (int y = 0; y < image.getHeight(); y++) {
            int opaque = 0;
            for (int x = width / 4; x < width * 3 / 4; x++) {
                if ((image.getRGB(x, y) >>> 24) >= 128) { opaque++; }
            }
            if (opaque >= width / 8) { return y; }
        }
        throw new AssertionError("패널 위쪽 테두리를 찾을 수 없습니다.");
    }

    private static List<Component> descendants(Container root) {
        List<Component> components = new ArrayList<>();
        for (Component child : root.getComponents()) {
            components.add(child);
            if (child instanceof Container container) { components.addAll(descendants(container)); }
        }
        return components;
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) { layoutTree(container); }
        }
    }

    @Test void physicalDialsAcceptClicksAndKeyboardWithoutChangingAnswerContract() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AnswerInputPanel panel = new AnswerInputPanel();
            panel.showGrid3x3();
            List<JTextField> fields = new ArrayList<>();
            collect(panel, fields);
            assertEquals(9, fields.size());
            fields.get(0).setText("9");
            fields.get(0).dispatchEvent(new MouseEvent(fields.get(0), MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1));
            assertEquals("0", fields.get(0).getText());
            fields.get(0).dispatchEvent(new MouseEvent(fields.get(0), MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON3));
            assertEquals("9", fields.get(0).getText());
            AtomicReference<String> submitted = new AtomicReference<>();
            panel.setOnSubmit(submitted::set);
            for (int i = 0; i < 9; i++) { fields.get(i).setText(Integer.toString(i)); }
            fields.get(8).postActionEvent();
            assertEquals("012345678", submitted.get());
            fields.get(0).setText("x");
            assertEquals("0", fields.get(0).getText());
        });
    }

    @Test void draggingOneDrawerWheelLeavesTheOtherThreeInPlace() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AnswerInputPanel panel = new AnswerInputPanel();
            List<JTextField> fields = new ArrayList<>();
            collect(panel, fields);
            JTextField wheels = fields.get(0);
            wheels.setSize(400, 70);
            long time = System.currentTimeMillis();
            wheels.dispatchEvent(new MouseEvent(wheels, MouseEvent.MOUSE_PRESSED, time,
                    MouseEvent.BUTTON1_DOWN_MASK, 150, 50, 1, false, MouseEvent.BUTTON1));
            wheels.dispatchEvent(new MouseEvent(wheels, MouseEvent.MOUSE_DRAGGED, time,
                    MouseEvent.BUTTON1_DOWN_MASK, 150, 20, 0, false, MouseEvent.NOBUTTON));
            wheels.dispatchEvent(new MouseEvent(wheels, MouseEvent.MOUSE_RELEASED, time,
                    0, 150, 20, 1, false, MouseEvent.BUTTON1));
            assertEquals("0300", wheels.getText());
        });
    }

    @Test void overlaysStayInsetAndDoNotOverlapAtBothWindowSizes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel room            = new JPanel(new GameOverlayLayout());
            CluePanel clue         = new CluePanel();
            AnswerInputPanel input = new AnswerInputPanel();
            JPanel inventory       = new JPanel();
            clue.openClue();
            input.showGrid3x3();
            room.add(clue, "clue");
            room.add(input, "input");
            room.add(inventory, "inventory");
            for (int width : new int[] {960, 1280}) {
                room.setSize(width, width == 960 ? 680 : 800);
                room.doLayout();
                assertTrue(clue.getX() >= 16);
                assertFalse(clue.getBounds().intersects(input.getBounds()));
                assertFalse(input.getBounds().intersects(inventory.getBounds()));
                assertTrue(input.getX() + input.getWidth() <= width - 16);
            }
        });
    }

    @Test void inventoryShortcutsSelectOccupiedSlotsButNeverStealTextInput() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            coderoom.widgets.inventory.InventoryPanel inventory = new coderoom.widgets.inventory.InventoryPanel();
            inventory.setItems(List.of(ItemId.NOTE1, ItemId.NOTE2), List.of("메모 1", "메모 2"));
            AtomicReference<ItemId> selected = new AtomicReference<>();
            inventory.setOnItemSelected(selected::set);
            JTextField focusedInput = new JTextField();
            java.awt.KeyboardFocusManager original = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager();
            try {
                java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager(new java.awt.DefaultKeyboardFocusManager() {
                    @Override public Component getFocusOwner() { return inventory; }
                });
                inventory.getActionMap().get("inventorySlot2").actionPerformed(null);
                assertEquals(ItemId.NOTE2, selected.get());
                inventory.getActionMap().get("inventorySlot5").actionPerformed(null);
                assertEquals(ItemId.NOTE2, selected.get());
                java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager(new java.awt.DefaultKeyboardFocusManager() {
                    @Override public Component getFocusOwner() { return focusedInput; }
                });
                inventory.getActionMap().get("inventorySlot1").actionPerformed(null);
                assertEquals(ItemId.NOTE2, selected.get());
            } finally {
                java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager(original);
            }
        });
    }

    @Test void hidingMemoOrSwitchingDevicesNeverMovesOverlaySlots() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel room            = new JPanel(new GameOverlayLayout());
            CluePanel clue         = new CluePanel();
            AnswerInputPanel input = new AnswerInputPanel();
            room.add(clue, "clue");
            room.add(input, "input");
            room.setSize(1280, 800);
            clue.openClue();
            room.doLayout();
            java.awt.Rectangle bounds = input.getBounds();
            clue.closeClue();
            room.doLayout();
            assertEquals(bounds, input.getBounds());
            input.showGrid3x3();
            room.doLayout();
            assertEquals(bounds, input.getBounds());
        });
    }

    private static void collect(Container root, List<JTextField> fields) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTextField field) { fields.add(field); }
            if (child instanceof Container container) { collect(container, fields); }
        }
    }
}
