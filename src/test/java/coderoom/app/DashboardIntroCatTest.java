package coderoom.app;

import coderoom.entities.user.LoginUser;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardIntroCatTest {
    @Test void catIsVisibleAndClickableOnTopOfThePressEnterScreen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DashboardPage page   = new DashboardPage(new LoginUser(1, "test"), () -> {}, () -> {}, true, () -> {});
            BufferedImage actual = new BufferedImage(1585, 992, BufferedImage.TYPE_INT_ARGB);
            BufferedImage cat    = new BufferedImage(1585, 992, BufferedImage.TYPE_INT_ARGB);
            page.setSize(1585, 992);
            page.doLayout();
            Graphics2D sceneGraphics = actual.createGraphics();
            Graphics2D catGraphics   = cat.createGraphics();
            page.paint(sceneGraphics);
            new DashboardCat().paint(catGraphics);
            sceneGraphics.dispose();
            catGraphics.dispose();
            int checked = 0;
            for (int y = 760; y < 890; y++) {
                for (int x = 580; x < 700; x++) {
                    if ((cat.getRGB(x, y) >>> 24) >= 250) {
                        int expected = cat.getRGB(x, y);
                        int shown    = actual.getRGB(x, y);
                        for (int shift = 0; shift <= 16; shift += 8) {
                            assertTrue(Math.abs((expected >> shift & 255) - (shown >> shift & 255)) <= 5,
                                    "Intro must not hide or dim the cat");
                        }
                        checked++;
                    }
                }
            }
            assertTrue(checked > 2000, "Opaque cat pixels checked: " + checked);
            boolean clickable = false;
            for (var component : page.getComponents()) {
                if (component instanceof JButton button && "고양이 쓰다듬기".equals(button.getText())) {
                    clickable = button.isVisible() && button.contains(640, 860);
                }
            }
            assertTrue(clickable);
        });
    }
}
