package coderoom.shared.ui;

import coderoom.entities.game.ItemId;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;

/** 인벤토리와 다른 화면에서 재사용하는 작은 물체 픽셀 아이콘. */
public final class ItemSprite implements Icon {
    private final ItemId item;
    public ItemSprite(ItemId item) { this.item = item; }
    @Override public int getIconWidth() { return 54; }
    @Override public int getIconHeight() { return 46; }
    @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.scale(2, 2);
        PixelTheme.pixels(g);
        if (item == ItemId.KEY) {
            g.setColor(new Color(66, 38, 15));
            g.fillRect(4, 6, 8, 9);
            g.fillRect(10, 10, 15, 4);
            g.setColor(new Color(177, 117, 35));
            g.fillRect(3, 5, 8, 9);
            g.fillRect(10, 9, 14, 3);
            g.fillRect(19, 11, 3, 5);
            g.fillRect(23, 11, 2, 4);
            g.setColor(new Color(245, 198, 91));
            g.fillRect(4, 4, 6, 2);
            g.fillRect(2, 6, 2, 6);
            g.fillRect(10, 9, 14, 1);
            g.setColor(new Color(37, 28, 20));
            g.fillRect(5, 7, 4, 4);
        } else {
            g.setColor(new Color(104, 75, 40));
            g.fillRect(7, 3, 16, 19);
            g.setColor(new Color(192, 157, 98));
            g.fillRect(5, 2, 16, 19);
            g.setColor(PixelTheme.PAPER);
            g.fillRect(4, 1, 15, 19);
            g.setColor(new Color(243, 224, 179));
            g.fillRect(5, 2, 12, 16);
            g.setColor(new Color(170, 133, 77));
            g.fillRect(15, 1, 4, 4);
            g.setColor(new Color(106, 83, 53));
            for (int line = 0; line < 4; line++) {
                g.fillRect(7, 7 + line * 2, line % 2 == 0 ? 8 : 6, 1);
            }
            if (item == ItemId.NOTE2) {
                g.setColor(new Color(121, 52, 34));
                g.fillRect(14, 15, 4, 3);
            }
        }
        g.dispose();
    }
}
