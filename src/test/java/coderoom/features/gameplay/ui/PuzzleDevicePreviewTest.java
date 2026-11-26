package coderoom.features.gameplay.ui;

import coderoom.entities.game.WorkshopRoom;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleDevicePreviewTest {
    @Test void everyPuzzleShowsEquipmentAboveItsBackground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (long id : new long[] {1, 2, 311, 312, 313, 314, 315, 401, 402, 403, 404, 405}) {
                BufferedImage preview = PuzzleDevicePreview.render(id, "", "AB").orElseThrow();
                PropBoard bare = id < 3
                        ? new PropBoard("/images/game-ui/study/" + (id == 1 ? "drawer.jpg" : "wall.jpg"))
                        : new PropBoard(WorkshopRoom.forMap(id / 100), (int) (id % 10) - 1);
                bare.setSize(preview.getWidth(), preview.getHeight());
                var background = new BufferedImage(preview.getWidth(), preview.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = background.createGraphics();
                bare.printAll(graphics);
                graphics.dispose();

                int changed = 0;
                for (int y = 0; y < preview.getHeight(); y += 3) {
                    for (int x = 0; x < preview.getWidth(); x += 3) {
                        if (preview.getRGB(x, y) != background.getRGB(x, y)) { changed++; }
                    }
                }
                assertTrue(changed > 100, "visible controls for puzzle " + id + ": " + changed);
            }
        });
    }

    @Test void savedBbcGuttersShowTheConnectedWaterPath() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var preview = PuzzleDevicePreview.render(402, "물이 높은 유입구에서 낮은 왼쪽 화분으로 흐른다.", "BBC")
                    .orElseThrow();
            assertTrue(waterAt(preview, 300, 409));
            assertTrue(waterAt(preview, 500, 409));
            assertTrue(waterAt(preview, 790, 461));
        });
    }

    private static boolean waterAt(BufferedImage preview, int x, int y) {
        Color color = new Color(preview.getRGB(x, y));
        return color.getBlue() > 180 && color.getGreen() > 160 && color.getRed() < 150;
    }
}
