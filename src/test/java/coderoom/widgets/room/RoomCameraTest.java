package coderoom.widgets.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import coderoom.entities.game.RoomObjectId;
import coderoom.shared.ui.GameOverlayLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.zip.CRC32;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class RoomCameraTest {
    @Test
    void cameraOnlyApproachesAnOpenedPuzzleAndReturnsAfterClose() throws Exception {
        RoomPanel[] room = new RoomPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            room[0] = new RoomPanel(object -> room[0].setPuzzleOpen(object == RoomObjectId.DRAWER));
            room[0].initialize();
            room[0].setSize(793, 496);
        });
        try {
            long wide = frame(room[0]);
            SwingUtilities.invokeAndWait(() -> click(room[0], 510, 180));
            assertEquals(wide, frame(room[0]));

            SwingUtilities.invokeAndWait(() -> click(room[0], 335, 300));
            Thread.sleep(550);
            long near = frame(room[0]);
            assertNotEquals(wide, near);
            Thread.sleep(100);
            assertEquals(near, frame(room[0]));

            SwingUtilities.invokeAndWait(() -> room[0].setPuzzleOpen(false));
            assertNotEquals(wide, frame(room[0]));
            Thread.sleep(550);
            assertEquals(wide, frame(room[0]));
        } finally {
            SwingUtilities.invokeAndWait(room[0]::removeNotify);
        }
    }

    @Test
    void wideAndSmallWindowsKeepRoomClicksAboveInventory() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<RoomObjectId> clicks = new ArrayList<>();
            RoomPanel room = new RoomPanel(clicks::add, true);
            room.initialize();
            room.setLayout(new GameOverlayLayout());
            room.setSceneBottomInset(GameOverlayLayout.roomBottomInset());
            JPanel inventory = new JPanel();
            room.add(inventory, "inventory");

            for (int[] size : new int[][] {{900, 650}, {1512, 919}, {2512, 1108}, {900, 1000}}) {
                room.setSize(size[0], size[1]);
                room.doLayout();
                int sceneHeight = size[1] - GameOverlayLayout.roomBottomInset();
                java.awt.Point drawer = room.screenPoint(668, 620);
                java.awt.Point right  = room.screenPoint(768, 620);
                java.awt.Point down   = room.screenPoint(668, 720);
                assertEquals(right.x - drawer.x, down.y - drawer.y, 1);
                assertEquals(sceneHeight + 8, inventory.getY());
                BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                room.paint(graphics);
                graphics.dispose();
                java.awt.Point imageOrigin = room.screenPoint(0, 0);
                assertNotEquals(0x15271E, image.getRGB(drawer.x, drawer.y) & 0xFFFFFF);
                if (imageOrigin.x > 2) {
                    assertNotEquals(0x15271E, image.getRGB(2, sceneHeight / 2) & 0xFFFFFF);
                    click(room, 2, sceneHeight / 2);
                }
                if (imageOrigin.y > 2) {
                    assertNotEquals(0x15271E, image.getRGB(size[0] / 2, 2) & 0xFFFFFF);
                    click(room, size[0] / 2, 2);
                }
                assertNotEquals(0x15271E, image.getRGB(size[0] / 2, size[1] - 2) & 0xFFFFFF);
                click(room, drawer.x, drawer.y);
                click(room, drawer.x, inventory.getY() + 12);
            }
            assertEquals(java.util.Collections.nCopies(4, RoomObjectId.DRAWER), clicks);
        });
    }

    @Test
    void workshopArtworkContinuesUnderHudAndInventoryAtDifferentWindowSizes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            verifyWorkshopFraming(3, 331, 303);
            verifyWorkshopFraming(4, 145, 550);
        });
    }

    private static void verifyWorkshopFraming(long mapId, int deviceX, int deviceY) {
        List<RoomObjectId> clicks = new ArrayList<>();
        RoomPanel room            = new RoomPanel(clicks::add, true, mapId);
        room.initialize();
        room.setSceneBottomInset(GameOverlayLayout.roomBottomInset());
        for (int[] size : new int[][] {{900, 650}, {1280, 800}, {2512, 1108}, {900, 1000}}) {
            int width  = size[0];
            int height = size[1];
            room.setSize(width, height);
            var image    = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            room.paint(graphics);
            graphics.dispose();
            assertNotEquals(0x15271E, image.getRGB(2, 120) & 0xFFFFFF);
            assertNotEquals(0x15271E, image.getRGB(width - 3, height - 3) & 0xFFFFFF);
            assertNotEquals(0x15271E, image.getRGB(width / 2, height - 3) & 0xFFFFFF);
            var rack = room.screenPoint(deviceX, deviceY);
            click(room, rack.x, rack.y);
            click(room, width / 2, height - 3);
        }
        assertEquals(java.util.Collections.nCopies(4, RoomObjectId.DRAWER), clicks);
    }

    private static void click(RoomPanel panel, int x, int y) {
        for (int event : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED}) {
            panel.dispatchEvent(new MouseEvent(panel, event, System.currentTimeMillis(), 0,
                    x, y, 1, false, MouseEvent.BUTTON1));
        }
    }

    private static long frame(RoomPanel room) throws Exception {
        FutureTask<Long> snapshot = new FutureTask<>(() -> {
            BufferedImage image = new BufferedImage(room.getWidth(), room.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            room.paint(graphics);
            graphics.dispose();
            CRC32 checksum = new CRC32();
            for (int pixel : image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth())) {
                checksum.update(pixel >> 16);
                checksum.update(pixel >> 8);
                checksum.update(pixel);
            }
            return checksum.getValue();
        });
        SwingUtilities.invokeAndWait(snapshot);
        return snapshot.get();
    }
}
