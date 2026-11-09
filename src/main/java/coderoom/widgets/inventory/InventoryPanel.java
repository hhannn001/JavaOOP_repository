package coderoom.widgets.inventory;

import coderoom.entities.game.ItemId;
import java.util.List;
import java.util.function.Consumer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * 담당: F
 * 역할: 게임 화면 하단 인벤토리 UI
 *
 * 현재는 UI 프로토타입만 구현
 * GameView 및 GameFacade 연동은 추후 진행
 */
public class InventoryPanel extends JPanel {

    private static final int SLOT_COUNT = 5;

    //인벤토리 색상
    private static final Color BG =
            new Color(30, 45, 36);

    private static final Color SLOT_BG =
            new Color(39, 56, 45);

    private static final Color BORDER =
            new Color(119, 133, 105);

    private static final Color GOLD =
            new Color(223, 200, 145);

    private static final Color SELECTED_BG =
            new Color(70, 87, 65);

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

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 2),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));

        setPreferredSize(new Dimension(390, 70));

        for (int i = 0; i < SLOT_COUNT; i++) {

            final int index = i;

            JButton button = new JButton() {
                private final String slotNumber = String.format("%02d", index + 1);
                private final Font numberFont = new Font("Dialog", Font.PLAIN, 9);

                @Override
                protected void paintComponent(Graphics g) {
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
            button.setBackground(SLOT_BG);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setEnabled(false);

            button.setBorder(
                    BorderFactory.createLineBorder(BORDER, 1)
            );

            slots[i] = button;
            add(button);

            button.addActionListener(e -> selectSlot(index));
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
        slots[index].setText(id != null ? name : "");
        slots[index].setEnabled(id != null);
    }

    /**
     * 아이템 선택 시 테두리 강조
     */
    private void selectSlot(int index) {

        if (itemIds[index] == null) {
            return;
        }

        selectedIndex = index;

        for (int i = 0; i < SLOT_COUNT; i++) {
            boolean selected = (i == selectedIndex);
            slots[i].setBackground(
                    selected ? SELECTED_BG : SLOT_BG
            );
            slots[i].setBorder(
                    BorderFactory.createLineBorder(
                            selected ? GOLD : BORDER,
                            selected ? 3 : 1
                    )
            );
        }

        if (onItemSelected != null) {
            onItemSelected.accept(itemIds[index]);
        }
    }

    public void setItems(List<ItemId> ids, List<String> names) {
        if (ids.size() != names.size() || ids.size() > SLOT_COUNT) {
            throw new IllegalArgumentException("아이템 ID와 이름 개수를 확인하세요.");
        }

        selectedIndex = -1;

        for (int i = 0; i < SLOT_COUNT; i++) {
            boolean hasItem = i < ids.size();
            setItem(i, hasItem ? ids.get(i) : null, hasItem ? names.get(i) : "");
            slots[i].setBackground(SLOT_BG);
            slots[i].setBorder(
                    BorderFactory.createLineBorder(BORDER, 1)
            );
        }
    }

}
