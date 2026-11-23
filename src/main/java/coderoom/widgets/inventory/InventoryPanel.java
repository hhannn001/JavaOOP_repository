package coderoom.widgets.inventory;

import coderoom.shared.ui.HandCursor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import coderoom.entities.game.ItemId;
import coderoom.shared.ui.ItemSprite;

/**
 * 담당: F
 * 역할: 게임 화면 하단 인벤토리 UI
 *
 * Noyoungeun의 41b432f UI를 유지하며 GameView 선택 상태와 콜백을 연결한다.
 */
public final class InventoryPanel extends JPanel {

    private static final int SLOT_COUNT = 6;

    //인벤토리 색상
    private static final Color BG =
            new Color(30, 45, 36);

    private static final Color SLOT_BG =
            new Color(39, 56, 45);

    private static final Color GOLD =
            new Color(223, 200, 145);

    private static final Color SELECTED_BG =
            new Color(70, 87, 65);

    private static final Color WALNUT = new Color(63, 35, 19);
    private static final Color WOOD_HIGHLIGHT = new Color(106, 62, 37);
    private static final Color FRAME_SHADOW = new Color(29, 15, 7);
    private static final Color BRASS = new Color(174, 120, 54);
    private static final Color FELT = new Color(19, 44, 29);
    private static final Color FELT_SHADOW = new Color(12, 29, 20);

    private final JButton[] slots =
            new JButton[SLOT_COUNT];

    private int selectedIndex = -1;
    private final ItemId[] itemIds = new ItemId[SLOT_COUNT];
    private Consumer<ItemId> onItemSelected;

    /** 외부 위젯 조립 코드에서 GamePage.selectItem에 연결한다. */
    public void setOnItemSelected(Consumer<ItemId> listener) {
        this.onItemSelected = listener;
    }

    public InventoryPanel() {

        setLayout(new GridLayout(1, SLOT_COUNT, 5, 0));
        setBackground(BG);

        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        setPreferredSize(new Dimension(390, 70));

        for (int i = 0; i < SLOT_COUNT; i++) {

            final int index = i;

            JButton button = new JButton() {
                private final String slotNumber = String.format("%02d", index + 1);
                private final Font numberFont = new Font("Dialog", Font.PLAIN, 9);

                @Override
                protected void paintComponent(Graphics g) {
                    paintSlot(g, getWidth(), getHeight(), index == selectedIndex);
                    super.paintComponent(g);

                    Graphics numberGraphics = g.create();
                    try {
                        numberGraphics.setFont(numberFont);
                        numberGraphics.setColor(GOLD);
                        numberGraphics.drawString(
                                slotNumber, 5,
                                4 + numberGraphics.getFontMetrics().getAscent()
                        );
                    } finally {
                        numberGraphics.dispose();
                    }
                }
            };

            button.setFont(
                    new Font("Dialog", Font.BOLD, 11)
            );

            button.setForeground(GOLD);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setIconTextGap(2);
            button.setBackground(SLOT_BG);
            button.setFocusPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            HandCursor.install(button);
            button.setEnabled(false);

            button.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

            slots[i] = button;
            add(button);

            button.addActionListener(e -> selectSlot(index));
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
        }

    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int width = getWidth();
        int height = getHeight();
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(FRAME_SHADOW);
            g.fillRect(0, 0, width, height);
            g.setPaint(new GradientPaint(0, 0, WOOD_HIGHLIGHT, 0, height / 2f, WALNUT, true));
            g.fillRect(2, 2, width - 4, height - 4);
            g.setColor(WOOD_HIGHLIGHT);
            g.drawRect(3, 3, width - 7, height - 7);
            g.setColor(FRAME_SHADOW);
            g.fillRect(7, 6, width - 14, height - 12);

            paintBrassCorner(g, 2, 2, 1, 1);
            paintBrassCorner(g, width - 3, 2, -1, 1);
            paintBrassCorner(g, 2, height - 3, 1, -1);
            paintBrassCorner(g, width - 3, height - 3, -1, -1);
        } finally {
            g.dispose();
        }
    }

    private static void paintBrassCorner(Graphics2D graphics, int x, int y, int dx, int dy) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.scale(dx, dy);
            g.setColor(BRASS);
            g.fillRect(0, 0, 9, 3);
            g.fillRect(0, 0, 3, 9);
            g.setColor(GOLD);
            g.drawLine(0, 0, 8, 0);
            g.drawLine(0, 0, 0, 8);
            g.setColor(FRAME_SHADOW);
            g.fillRect(1, 1, 1, 1);
        } finally {
            g.dispose();
        }
    }

    private static void paintSlot(Graphics graphics, int width, int height, boolean selected) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setPaint(new GradientPaint(0, 0, FELT_SHADOW, 0, height, FELT));
            g.fillRect(0, 0, width, height);
            g.setColor(BRASS);
            g.drawRect(0, 0, width - 1, height - 1);
            g.setColor(selected ? GOLD : WALNUT);
            g.drawRect(1, 1, width - 3, height - 3);
        } finally {
            g.dispose();
        }
    }

    /**
     * 슬롯에 표시할 아이템 설정
     */
    public void setItem(int index, ItemId id, String name) {

        if (index < 0 || index >= SLOT_COUNT) {
            return;
        }

        itemIds[index] = id;
        slots[index].setIcon(id != null ? new ItemSprite(id, 1) : null);
        slots[index].setText(id != null ? name : "");
        slots[index].setEnabled(id != null);
    }

    /**
     * 아이템 선택 시 테두리 강조
     */
    private void selectSlot(int index) {

        if (!isVisible() || itemIds[index] == null) {
            return;
        }

        setSelectedItem(itemIds[index]);
        if (onItemSelected != null) {
            onItemSelected.accept(itemIds[index]);
        }
    }

    /** 타이머 재표시에도 게임의 선택 상태를 유지한다. */
    public void setSelectedItem(ItemId id) {
        selectedIndex = -1;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (id != null && itemIds[i] == id) { selectedIndex = i; }
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            boolean selected = (i == selectedIndex);
            slots[i].setBackground(
                    selected ? SELECTED_BG : SLOT_BG
            );
            slots[i].repaint();
        }

    }

    public void setItems(List<ItemId> ids, List<String> names) {
        if (ids.size() != names.size() || ids.size() > SLOT_COUNT) {
            throw new IllegalArgumentException("아이템 ID와 이름 개수를 확인하세요.");
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            boolean hasItem = i < ids.size();
            setItem(i, hasItem ? ids.get(i) : null, hasItem ? names.get(i) : "");
        }
        setSelectedItem(null);
    }

}
