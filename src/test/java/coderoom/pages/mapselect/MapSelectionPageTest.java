package coderoom.pages.mapselect;

import coderoom.features.mapcatalog.MapCard;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MapSelectionPageTest {
    @Test void postersOpenDetailsBeforeStartingAndComingSoonCannotStart() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicLong selected   = new AtomicLong(-1);
            MapSelectionPage page = new MapSelectionPage(selected::set, () -> selected.set(0));
            page.render(List.of(card(1, true), card(2, false)));
            assertEquals(6, buttons(page).stream().filter(button -> !button.isEnabled()).count());
            poster(page, "방 1").doClick();
            assertEquals(-1, selected.get());
            assertFalse(poster(page, "방 2").isEnabled());
            named(page, "이 방 시작하기").doClick();
            assertEquals(1, selected.get());
            poster(page, "방 2").doClick();
            assertFalse(named(page, "아직 준비 중이에요").isEnabled());
            named(page, "아직 준비 중이에요").doClick();
            assertEquals(1, selected.get());
            page.getActionMap().get("back").actionPerformed(null);
            poster(page, "방 2").doClick();
            named(page, "닫기").doClick();
            named(page, "로비로 돌아가기").doClick();
            assertEquals(0, selected.get());
        });
    }

    @Test void emptyCatalogShowsEightInactiveSlots() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MapSelectionPage page = new MapSelectionPage();
            page.render(null);
            assertEquals(8, buttons(page).size());
            assertEquals(8, buttons(page).stream().filter(button -> !button.isEnabled()).count());
        });
    }

    private static MapCard card(int id, boolean playable) {
        return new MapCard(id, "방 " + id, "이야기", "Java", "보통", "/missing.png", playable, false, null);
    }

    private static JButton named(Container root, String name) {
        return buttons(root).stream().filter(button -> name.equals(button.getText())).findFirst().orElseThrow();
    }

    private static JButton poster(Container root, String name) {
        return buttons(root).stream().filter(button -> name.equals(button.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow();
    }

    private static List<JButton> buttons(Container root) {
        List<JButton> result = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button) {
                if (button.getText() != null && !button.getText().isBlank()) { result.add(button); }
            }
            else if (component instanceof Container container) { result.addAll(buttons(container)); }
        }
        return result;
    }
}
