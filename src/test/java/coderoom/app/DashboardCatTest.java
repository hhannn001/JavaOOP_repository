package coderoom.app;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardCatTest {
    @Test void movementAcceleratesBrakesAndReversesWithoutTeleporting() {
        DashboardCat cat = new DashboardCat(new Random(1));
        double start     = cat.x();
        cat.key(KeyEvent.VK_RIGHT, true);
        cat.advance(1.0 / 60);
        assertTrue(cat.velocityX() > 0 && cat.velocityX() < 190);
        assertTrue(cat.x() > start && cat.x() - start < 1);
        advance(cat, 0.5);
        assertEquals(190, cat.velocityX(), 0.001);
        cat.key(KeyEvent.VK_RIGHT, false);
        cat.advance(1.0 / 60);
        assertTrue(cat.velocityX() > 0 && cat.velocityX() < 190);
        advance(cat, 0.2);
        assertEquals(0, cat.velocityX(), 0.001);
        cat.key(KeyEvent.VK_LEFT, true);
        advance(cat, 0.5);
        assertFalse(cat.facingRight());
        assertEquals(-190, cat.velocityX(), 0.001);
    }

    @Test void heldJumpCannotBunnyHopAndLandingReturnsExactlyToFloor() {
        DashboardCat cat = new DashboardCat(new Random(2));
        cat.key(KeyEvent.VK_UP, true);
        assertEquals(8, cat.frameIndex());
        advance(cat, 0.3);
        assertTrue(cat.height() > 50);
        assertEquals(9, cat.frameIndex());
        double highest = cat.height();
        for (int frame = 0; frame < 180; frame++) {
            cat.key(KeyEvent.VK_UP, true); // OS auto-repeat must not start another jump.
            cat.advance(1.0 / 60);
            highest = Math.max(highest, cat.height());
            assertTrue(cat.height() >= 0);
        }
        assertTrue(highest > 85 && highest < 100);
        assertEquals(0, cat.height(), 0.001);
        cat.key(KeyEvent.VK_UP, false);
        cat.key(KeyEvent.VK_UP, true);
        advance(cat, 0.3);
        assertTrue(cat.height() > 50);
    }

    @Test void boundariesAndOpposingKeysAreSafe() {
        DashboardCat cat = new DashboardCat(new Random(3));
        cat.key(KeyEvent.VK_RIGHT, true);
        advance(cat, 15);
        assertEquals(DashboardCat.RIGHT_EDGE, cat.x(), 0.001);
        cat.key(KeyEvent.VK_RIGHT, false);
        cat.key(KeyEvent.VK_LEFT, true);
        advance(cat, 15);
        assertEquals(DashboardCat.LEFT_EDGE, cat.x(), 0.001);
        cat.key(KeyEvent.VK_RIGHT, true);
        advance(cat, 1);
        assertEquals(0, cat.velocityX(), 0.001);
        assertEquals(DashboardCat.LEFT_EDGE, cat.x(), 0.001);
    }

    @Test void lostFocusClearsHeldKeysAndIdleResumesAfterFourSeconds() {
        DashboardCat cat = new DashboardCat(new Random(4));
        cat.key(KeyEvent.VK_RIGHT, true);
        advance(cat, 0.5);
        cat.releaseKeys();
        advance(cat, 0.5);
        double stopped = cat.x();
        advance(cat, 2);
        assertTrue(cat.isManual());
        assertEquals(stopped, cat.x(), 0.001);
        advance(cat, 2);
        assertFalse(cat.isManual());
        boolean moved = false;
        for (int frame = 0; frame < 3600; frame++) {
            cat.advance(1.0 / 60);
            moved |= Math.abs(cat.x() - stopped) > 10;
            assertTrue(cat.x() >= DashboardCat.LEFT_EDGE && cat.x() <= DashboardCat.RIGHT_EDGE);
        }
        assertTrue(moved);
    }

    @Test void movementIsConsistentAcrossRefreshRates() {
        DashboardCat sixty = new DashboardCat(new Random(5));
        DashboardCat high  = new DashboardCat(new Random(5));
        sixty.key(KeyEvent.VK_RIGHT, true);
        high.key(KeyEvent.VK_RIGHT, true);
        for (int frame = 0; frame < 120; frame++) sixty.advance(1.0 / 60);
        for (int frame = 0; frame < 288; frame++) high.advance(1.0 / 144);
        assertEquals(sixty.x(), high.x(), 0.2);
    }

    @Test void actualSpritePixelsShowBothPairsOfPawsCrossing() {
        // Check the artwork, not just the frame counter: pale near paws and brown far paws
        // must exchange horizontal ordering in BOTH the front and rear pair.
        BufferedImage contact  = DashboardCat.walkingFrame(0);
        BufferedImage opposite = DashboardCat.walkingFrame(8);
        assertTrue(pawCenter(contact, true, true) > pawCenter(contact, true, false) + 15);
        assertTrue(pawCenter(opposite, true, true) < pawCenter(opposite, true, false) - 15);
        assertTrue(pawCenter(contact, false, true) < pawCenter(contact, false, false) - 15);
        assertTrue(pawCenter(opposite, false, true) > pawCenter(opposite, false, false) + 15);
        for (int index = 0; index < 12; index++) {
            BufferedImage frame = DashboardCat.walkingFrame(index);
            assertTrue(frame.getColorModel().hasAlpha());
            assertTrue((frame.getRGB(0, 0) >>> 24) <= 16);
        }
    }

    @Test void wanderingFinishesLongTripsBeforeChangingDirection() {
        for (int seed = 0; seed < 20; seed++) {
            DashboardCat cat = new DashboardCat(new Random(seed));
            double tripStart = cat.x();
            int previous     = 0;
            int turns        = 0;
            for (int frame = 0; frame < 10800; frame++) {
                cat.advance(1.0 / 60);
                int heading = Math.abs(cat.velocityX()) < 1 ? 0 : cat.facingRight() ? 1 : -1;
                if (heading != 0 && heading != previous) {
                    if (previous != 0) {
                        assertTrue(Math.abs(cat.x() - tripStart) >= 350, "Wander must finish a long trip");
                        turns++;
                    }
                    tripStart = cat.x();
                    previous  = heading;
                }
                assertTrue(cat.x() >= DashboardCat.LEFT_EDGE && cat.x() <= DashboardCat.RIGHT_EDGE);
            }
            assertTrue(turns >= 3);
        }
    }

    @Test void bellOverridesManualInputAndStopsAtCounterWithGreeting() {
        DashboardCat cat = new DashboardCat(new Random(7));
        cat.key(KeyEvent.VK_LEFT, true);
        advance(cat, 0.5);
        cat.callToBell();
        cat.key(KeyEvent.VK_LEFT, false); // A late release must not cancel the bell.
        assertTrue(cat.isCalled());
        assertFalse(cat.isManual());
        advance(cat, 3.2);
        assertEquals(DashboardCat.BELL_X, cat.x(), 0.01);
        assertFalse(cat.isCalled());
        assertEquals(DashboardCat.Reaction.STAND, cat.reaction());
        assertEquals(0, cat.velocityX(), 0.001);
        cat.callToBell();
        advance(cat, 0.1);
        assertEquals(DashboardCat.BELL_X, cat.x(), 0.01);
        cat.key(KeyEvent.VK_LEFT, true);
        advance(cat, 0.5);
        assertEquals(DashboardCat.Reaction.NONE, cat.reaction());
        assertTrue(cat.x() < DashboardCat.BELL_X - 20);
    }

    @Test void clickingCyclesEightEmotionsWithoutJumpingOrRestartingOnSpam() {
        DashboardCat cat = new DashboardCat(new Random(8));
        for (DashboardCat.Reaction expected : new DashboardCat.Reaction[]{DashboardCat.Reaction.PURR,
                DashboardCat.Reaction.STAND, DashboardCat.Reaction.STRETCH, DashboardCat.Reaction.GRUMPY,
                DashboardCat.Reaction.CURIOUS, DashboardCat.Reaction.SLEEPY, DashboardCat.Reaction.SHY,
                DashboardCat.Reaction.SURPRISED}) {
            cat.interact();
            assertEquals(expected, cat.reaction());
            advance(cat, 1);
            assertEquals(0, cat.height(), 0.001, "Click emotions must keep paws on the floor");
            cat.interact();
            assertEquals(expected, cat.reaction());
            advance(cat, 1.6);
            assertEquals(DashboardCat.Reaction.NONE, cat.reaction());
            assertEquals(0, cat.height(), 0.001);
        }
        cat.interact();
        assertEquals(DashboardCat.Reaction.PURR, cat.reaction());
        cat.callToBell();
        assertEquals(DashboardCat.Reaction.NONE, cat.reaction());
        assertTrue(cat.isCalled());
    }

    @Test void catHitAreaFollowsItsActualSpriteAndJump() {
        DashboardCat cat = new DashboardCat(new Random(9));
        double groundY   = cat.bounds().getY();
        assertTrue(cat.contains(cat.x(), DashboardCat.FLOOR_Y - 30));
        assertFalse(cat.contains(cat.x() - 200, DashboardCat.FLOOR_Y - 30));
        cat.key(KeyEvent.VK_UP, true);
        advance(cat, 0.4);
        assertTrue(cat.bounds().getY() < groundY - 40);
        assertFalse(cat.contains(cat.x(), DashboardCat.FLOOR_Y - 5));
    }

    @Test void ambientCatBalancesShortRestsWithWalkingAndStretchesWithoutInput() {
        int quietFrames = 0;
        int stretches   = 0;
        for (int seed = 0; seed < 20; seed++) {
            DashboardCat cat = new DashboardCat(new Random(seed));
            assertTrue(cat.isResting());
            double initial = cat.x();
            advance(cat, 3);
            assertEquals(initial, cat.x(), 0.001, "The initial rest must not turn into a walk");
            DashboardCat.Reaction previous = cat.reaction();
            for (int tick = 0; tick < 7200; tick++) {
                cat.advance(1.0 / 60);
                if (Math.abs(cat.velocityX()) < 1) quietFrames++;
                if (cat.reaction() == DashboardCat.Reaction.STRETCH && previous != cat.reaction()) stretches++;
                previous = cat.reaction();
            }
        }
        double quietRatio = quietFrames / (20.0 * 7200);
        assertTrue(quietRatio > 0.30 && quietRatio < 0.60, "Rest and walking should stay balanced: " + quietRatio);
        assertTrue(stretches >= 15, "Idle stretching must actually occur without clicks");
    }

    @Test void automaticJumpsOccurButNeverInterruptRestOrIgnoreCooldown() {
        int jumps = 0;
        for (int seed = 0; seed < 20; seed++) {
            DashboardCat cat = new DashboardCat(new Random(seed));
            double previousHeight = 0;
            int lastJump          = -10000;
            for (int tick = 0; tick < 18000; tick++) {
                boolean resting = cat.isResting();
                cat.advance(1.0 / 60);
                if (cat.height() > 0 && previousHeight == 0) {
                    assertFalse(resting, "Automatic jumps must not interrupt a rest");
                    assertTrue(tick - lastJump >= 600, "Automatic jumps need a ten-second cooldown");
                    lastJump = tick;
                    jumps++;
                }
                previousHeight = cat.height();
            }
        }
        assertTrue(jumps >= 25 && jumps <= 200, "Ambient jump path must be active but not spam: " + jumps);
    }

    private static double pawCenter(BufferedImage frame, boolean front, boolean near) {
        int split   = (int) (frame.getWidth() * 0.55);
        int start   = front ? split : 0;
        int end     = front ? frame.getWidth() : split;
        double sum  = 0;
        int samples = 0;
        for (int y = (int) (frame.getHeight() * 0.83); y < frame.getHeight(); y++) {
            for (int x = start; x < end; x++) {
                int pixel = frame.getRGB(x, y);
                int red   = pixel >> 16 & 255;
                int green = pixel >> 8 & 255;
                int blue  = pixel & 255;
                boolean color = near ? red > 220 && green > 165 && blue > 110
                        : red > 100 && red < 210 && green > 55 && green < 150 && blue < 110;
                if ((pixel >>> 24) >= 128 && color) {
                    sum += x;
                    samples++;
                }
            }
        }
        assertTrue(samples > 100, "Each paw must be visible in the actual artwork");
        return sum / samples;
    }

    private static void advance(DashboardCat cat, double seconds) {
        for (int frame = 0; frame < (int) Math.round(seconds * 60); frame++) cat.advance(1.0 / 60);
    }
}
