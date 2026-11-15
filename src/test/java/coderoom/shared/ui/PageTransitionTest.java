package coderoom.shared.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.FutureTask;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class PageTransitionTest {
    @Test
    void preservesRemovedPageAndReleasesOverlayAfterAnimation() throws Exception {
        PageTransition transition = new PageTransition();
        SwingUtilities.invokeAndWait(() -> {
            transition.setSize(20, 20);
            transition.play();
            assertFalse(transition.isVisible());
            JPanel page = new JPanel();
            page.setSize(20, 20);
            page.setBackground(Color.RED);
            transition.capture(page);
            page.setBackground(Color.BLUE);
            transition.capture(page);
            transition.play();
            assertTrue(transition.isVisible());
            BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            transition.paint(graphics);
            graphics.dispose();
            assertEquals(Color.RED.getRGB(), image.getRGB(10, 10));
        });
        try {
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (true) {
                FutureTask<Boolean> visible = new FutureTask<>(transition::isVisible);
                SwingUtilities.invokeAndWait(visible);
                if (!visible.get()) {
                    break;
                }
                assertTrue(System.nanoTime() < deadline, "전환 오버레이가 해제되어야 합니다.");
                Thread.sleep(20);
            }
            SwingUtilities.invokeAndWait(() -> {
                transition.play();
                assertFalse(transition.isVisible(), "완료 후 이전 화면을 보관하지 않아야 합니다.");
            });
        } finally {
            SwingUtilities.invokeAndWait(transition::stop);
        }
    }
}
