package coderoom.widgets.inventory;

import coderoom.entities.game.ItemId;
import coderoom.shared.ui.ItemSprite;
import coderoom.shared.ui.PixelTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.text.JTextComponent;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * 담당: F
 * 역할: 게임 화면 하단에 메모·열쇠 인벤토리를 표시한다.
 * 게임 스냅샷의 아이템과 선택 상태를 표시하고 선택 콜백을 전달한다.
 */
public class InventoryPanel extends JPanel {
    private static final int SLOT_COUNT = 5;
    private final JButton[] slots = new JButton[SLOT_COUNT];
    private final ItemId[] itemIds = new ItemId[SLOT_COUNT];
    private ItemId selected;
    private Consumer<ItemId> onItemSelected;

    public InventoryPanel() {
        setLayout(new GridLayout(1, SLOT_COUNT, 2, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setPreferredSize(new Dimension(390, 78));
        for (int i = 0; i < SLOT_COUNT; i++) {
            final int index = i;
            JButton slot = new JButton() {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    int w = getWidth();
                    int h = getHeight();
                    g.setColor(new Color(43, 39, 35));
                    g.fillRect(0, 0, w, h);
                    g.setColor(new Color(129, 119, 101));
                    g.fillRect(0, 0, w, 3);
                    g.fillRect(0, 0, 3, h);
                    g.setColor(new Color(22, 20, 18));
                    g.fillRect(0, h - 3, w, 3);
                    g.fillRect(w - 3, 0, 3, h);
                    g.setColor(new Color(68, 61, 51));
                    g.fillRect(4, 4, w - 8, h - 8);
                    if (itemIds[index] != null && itemIds[index] == selected) {
                        g.setColor(new Color(239, 232, 205));
                        g.drawRect(0, 0, w - 1, h - 1);
                        g.drawRect(1, 1, w - 3, h - 3);
                        g.drawRect(3, 3, w - 7, h - 7);
                    }
                    if (getIcon() != null) {
                        getIcon().paintIcon(this, g, (w - getIcon().getIconWidth()) / 2,
                                (h - getIcon().getIconHeight()) / 2 + 3);
                    }
                    g.setColor(PixelTheme.LIGHT);
                    g.setFont(PixelTheme.font(12));
                    g.drawString(Integer.toString(index + 1), 7, 15);
                    g.dispose();

                }
            };
            slot.setFont(PixelTheme.font(13));
            slot.setForeground(PixelTheme.LIGHT);
            slot.setContentAreaFilled(false);
            slot.setBorderPainted(false);
            slot.setFocusPainted(false);

            slot.setEnabled(false);
            slot.addActionListener(event -> selectSlot(index));
            String action = "inventorySlot" + (i + 1);
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0), action);
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1 + i, 0), action);
            getActionMap().put(action, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) {
                    if (!(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent)) {
                        selectSlot(index);
                    }
                }
            });
            slots[i] = slot;
            add(slot);
        }
    }

    private void selectSlot(int index) {
        if (isVisible() && itemIds[index] != null) {
            setSelectedItem(itemIds[index]);
            if (onItemSelected != null) { onItemSelected.accept(itemIds[index]); }
        }
    }

    public void setOnItemSelected(Consumer<ItemId> callback) { onItemSelected = callback; }
    public void setSelectedItem(ItemId id) { selected = id; repaint(); }
    public void setItem(int index, ItemId id, String name) {
        if (index < 0 || index >= SLOT_COUNT) { return; }
        if (itemIds[index] != id) { slots[index].setIcon(id == null ? null : new ItemSprite(id)); }
        itemIds[index] = id;
        slots[index].setText(id == null ? "" : name);
        slots[index].setToolTipText(id == null ? null : name + " 살펴보기");
        slots[index].setEnabled(id != null);
    }
    public void setItems(List<ItemId> ids, List<String> names) {
        if (ids.size() != names.size() || ids.size() > SLOT_COUNT) {
            throw new IllegalArgumentException("아이템 ID와 이름 개수를 확인하세요.");
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            setItem(i, i < ids.size() ? ids.get(i) : null, i < names.size() ? names.get(i) : "");
        }
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(new Color(20, 18, 16));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(148, 137, 114));
        g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
        g.dispose();
    }
}
