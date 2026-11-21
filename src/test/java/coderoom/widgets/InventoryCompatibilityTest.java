package coderoom.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import coderoom.entities.game.ItemId;
import coderoom.shared.ui.ItemSprite;
import coderoom.widgets.inventory.InventoryPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class InventoryCompatibilityTest {
    @Test void inventoryShowsIconsAndSupportsSelectionWithoutEmittingRenderCallbacks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InventoryPanel panel = new InventoryPanel();
            AtomicReference<ItemId> selected = new AtomicReference<>();
            panel.setOnItemSelected(selected::set);
            panel.setItems(List.of(ItemId.NOTE1, ItemId.KEY), List.of("메모 1", "열쇠"));
            panel.setSelectedItem(ItemId.KEY);
            JButton key = (JButton) panel.getComponent(1);
            assertEquals(new Dimension(390, 70), panel.getPreferredSize());
            assertEquals(new Color(30, 45, 36), panel.getBackground());
            assertEquals(new Color(70, 87, 65), key.getBackground());
            assertEquals("열쇠", key.getText());
            assertInstanceOf(ItemSprite.class, key.getIcon());
            assertNull(((JButton) panel.getComponent(2)).getIcon());
            assertNull(selected.get());
            key.doClick();
            assertEquals(ItemId.KEY, selected.get());
            panel.setItems(List.of(ItemId.NOTE1, ItemId.KEY), List.of("메모 1", "열쇠"));
            panel.setSelectedItem(ItemId.KEY);
            assertEquals(new Color(70, 87, 65), key.getBackground());
        });
    }
}
