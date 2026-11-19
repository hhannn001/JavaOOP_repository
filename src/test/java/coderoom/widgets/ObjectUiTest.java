package coderoom.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.ItemId;
import coderoom.features.gameplay.ItemView;
import coderoom.features.gameplay.ui.AnswerInputPanel;
import coderoom.shared.ui.GameOverlayLayout;
import coderoom.widgets.clue.CluePanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

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

    @Test void hintRemainsRevealedOnlyInThisGameAndForThisNote() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemView first  = new ItemView(ItemId.NOTE1, "메모 1", "code", "첫 힌트");
            ItemView second = new ItemView(ItemId.NOTE2, "메모 2", "code", "둘째 힌트");
            CluePanel clue  = new CluePanel();
            clue.setClueData(first);
            assertFalse(clue.isHintRevealed(ItemId.NOTE1));
            for (Component child : clue.getComponents()) {
                if ("revealHint".equals(child.getName())) { ((JButton) child).doClick(); }
            }
            clue.closeClue();
            clue.setClueData(second);
            assertFalse(clue.isHintRevealed(ItemId.NOTE2));
            clue.setClueData(first);
            assertTrue(clue.isHintRevealed(ItemId.NOTE1));
            CluePanel fresh = new CluePanel();
            fresh.setClueData(first);
            assertFalse(fresh.isHintRevealed(ItemId.NOTE1));
        });
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
