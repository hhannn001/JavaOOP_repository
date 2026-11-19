package coderoom.app;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatReactionFramesTest {
    @Test void changingPoseHeightNeverResizesTheHeadOrBodyPixels() {
        BufferedImage seated   = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        BufferedImage standing = new BufferedImage(24, 40, BufferedImage.TYPE_INT_ARGB);
        for (BufferedImage pose : new BufferedImage[]{seated, standing}) {
            Graphics2D g = pose.createGraphics();
            g.setColor(java.awt.Color.RED);
            g.fillRect(6, 0, 8, 8);
            g.setColor(java.awt.Color.BLUE);
            g.fillRect(6, pose.getHeight() - 4, 8, 4);
            g.dispose();
        }
        for (int step = 0; step <= 12; step++) {
            BufferedImage frame = CatReactionFrames.tween(seated, standing, step / 12.0);
            int headPixels      = 0;
            int pawPixels       = 0;
            for (int y = 0; y < frame.getHeight(); y++) {
                for (int x = 0; x < frame.getWidth(); x++) {
                    if (frame.getRGB(x, y) == java.awt.Color.RED.getRGB()) headPixels++;
                    if (frame.getRGB(x, y) == java.awt.Color.BLUE.getRGB()) pawPixels++;
                }
            }
            assertEquals(64, headPixels, "Pose transition must not stretch or shrink the head");
            assertEquals(32, pawPixels, "Pose transition must not scale the paws");
            assertEquals(java.awt.Color.BLUE.getRGB(), frame.getRGB(frame.getWidth() / 2, frame.getHeight() - 1));
        }
    }

    @Test void standingActuallyRisesAndEveryEmotionReturnsToTheSameIdlePose() {
        BufferedImage idle       = DashboardCat.loadFrames("cat-atlas.png", 3)[0];
        CatReactionFrames frames = new CatReactionFrames(idle);
        BufferedImage start      = frames.frame(DashboardCat.Reaction.STAND, 0);
        BufferedImage upright    = frames.frame(DashboardCat.Reaction.STAND, 1.0);
        assertTrue(upright.getHeight() > start.getHeight() + 10, "Standing must lift the body, not wave a paw");
        for (DashboardCat.Reaction reaction : DashboardCat.Reaction.values()) {
            if (reaction == DashboardCat.Reaction.NONE) continue;
            BufferedImage end = frames.frame(reaction, 2.4);
            assertEquals(start.getWidth(), end.getWidth());
            assertEquals(start.getHeight(), end.getHeight());
            for (int y = 0; y < start.getHeight(); y++) {
                for (int x = 0; x < start.getWidth(); x++) assertEquals(start.getRGB(x, y), end.getRGB(x, y));
            }
        }
    }

    @Test void interpolatedEmotionsKeepPawsGroundedAndAvoidLargeFrameFlashes() {
        BufferedImage idle       = DashboardCat.loadFrames("cat-atlas.png", 3)[0];
        CatReactionFrames frames = new CatReactionFrames(idle);
        for (DashboardCat.Reaction reaction : DashboardCat.Reaction.values()) {
            if (reaction == DashboardCat.Reaction.NONE) continue;
            BufferedImage previous = aligned(frames.frame(reaction, 0));
            for (int tick = 1; tick <= 144; tick++) {
                BufferedImage next = aligned(frames.frame(reaction, tick / 60.0));
                long difference    = 0;
                int lowest         = 0;
                int opaque         = 0;
                for (int y = 0; y < 160; y++) {
                    for (int x = 0; x < 160; x++) {
                        int a = previous.getRGB(x, y);
                        int b = next.getRGB(x, y);
                        for (int shift = 0; shift <= 24; shift += 8) {
                            difference += Math.abs((a >> shift & 255) - (b >> shift & 255));
                        }
                        if ((b >>> 24) > 128) {
                            lowest = Math.max(lowest, y);
                            opaque++;
                        }
                    }
                }
                assertTrue(opaque > 2000, reaction + " must not fade away");
                assertTrue(lowest >= 157, reaction + " paws must stay on the baseline");
                assertTrue(difference / (160.0 * 160 * 4 * 255) < 0.08,
                        reaction + " has an abrupt frame change at " + tick + ": " + difference / (160.0 * 160 * 4 * 255));
                previous = next;
            }
        }
    }

    private static BufferedImage aligned(BufferedImage sprite) {
        BufferedImage result = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g         = result.createGraphics();
        g.drawImage(sprite, 80 - sprite.getWidth() / 2, 160 - sprite.getHeight(), null);
        g.dispose();
        return result;
    }
}
