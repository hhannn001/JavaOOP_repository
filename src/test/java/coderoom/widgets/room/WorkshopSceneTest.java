package coderoom.widgets.room;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.GameStage;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.game.WorkshopRoom;
import coderoom.features.gameplay.GameView;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkshopSceneTest {
    @Test void rackHasOneContourWithTheGapBetweenItsFeet() {
        WorkshopScene scene = new WorkshopScene(WorkshopRoom.ENGINE);
        var rack = scene.spots().stream().filter(spot -> spot.id() == RoomObjectId.DRAWER)
                .findFirst().orElseThrow();
        var silhouette = new java.awt.geom.Area(scene.silhouette(rack));
        assertTrue(silhouette.isSingular());
        assertTrue(silhouette.contains(300, 305));
        assertFalse(silhouette.contains(335, 665));
    }

    @Test void spriteHoverOmitsInternalHolesAndKeepsSeparateParts() {
        var sprite = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        var graphics = sprite.createGraphics();
        graphics.fillRect(0, 0, 6, 6);
        graphics.fillRect(8, 0, 2, 2);
        graphics.setComposite(java.awt.AlphaComposite.Clear);
        graphics.fillRect(2, 2, 2, 2);
        graphics.dispose();
        var outline = SpriteOutline.fromAlpha(sprite, new Rectangle(0, 0, 10, 10));
        var visible = SpriteOutline.exterior(outline);
        assertFalse(outline.contains(3, 3));
        assertTrue(visible.contains(3, 3));
        assertTrue(visible.contains(9, 1));
        assertFalse(visible.contains(7, 1));
    }

    @Test void thresholdAndSolvedPressureAffectTheIllustrationIndependently() {
        assertEquals(0, WorkshopScene.phase(149));
        assertEquals(1, WorkshopScene.phase(150));
        assertEquals(2, WorkshopScene.phase(300));
        assertEquals(3, WorkshopScene.phase(450));
        assertEquals(4, WorkshopScene.phase(600));

        WorkshopScene scene = settledScene(WorkshopRoom.ENGINE);
        Rectangle gauge = new Rectangle(546, 730, 62, 61);
        assertFalse(Arrays.equals(region(scene, 50, Set.of(), gauge), region(scene, 150, Set.of(), gauge)));
        assertArrayEquals(region(scene, 55, Set.of(3), gauge), region(scene, 150, Set.of(3), gauge));
        Rectangle shutter = new Rectangle(1350, 470, 120, 40);
        assertFalse(Arrays.equals(region(scene, 599, Set.of(), shutter), region(scene, 600, Set.of(), shutter)));
    }

    @Test void drainingStopsIndoorFloodingButOutsideFloatStillRises() {
        WorkshopScene scene = settledScene(WorkshopRoom.GREENHOUSE);
        Rectangle water = new Rectangle(918, 684, 90, 180);
        assertFalse(Arrays.equals(region(scene, 0, Set.of(), water), region(scene, 450, Set.of(), water)));
        assertFalse(Arrays.equals(region(scene, 450, Set.of(), water), region(scene, 450, Set.of(1), water)));
        assertArrayEquals(region(scene, 150, Set.of(1), water), region(scene, 450, Set.of(1), water));
        Rectangle floatArea = new Rectangle(1185, 210, 100, 100);
        assertFalse(Arrays.equals(region(scene, 150, Set.of(1), floatArea), region(scene, 450, Set.of(1), floatArea)));
    }

    @Test void drainingTransitionsOnlyTheBasinAndKeepsTheSpareGutterOnItsShelf() {
        var clock = new java.util.concurrent.atomic.AtomicLong();
        var scene = new WorkshopScene(WorkshopRoom.GREENHOUSE, clock::get);
        Rectangle fixtures = new Rectangle(1391, 375, 114, 292);
        Rectangle basin    = new Rectangle(930, 690, 60, 90);
        int[] before = region(scene, 0, Set.of(), fixtures);
        int[] wet    = region(scene, 0, Set.of(), basin);

        assertArrayEquals(before, region(scene, 0, Set.of(1), fixtures));
        clock.set(240_000_000);
        int[] drying = region(scene, 0, Set.of(1), basin);
        assertArrayEquals(before, region(scene, 0, Set.of(1), fixtures));
        clock.set(1_100_000_000);
        int[] dry = region(scene, 0, Set.of(1), basin);
        assertArrayEquals(before, region(scene, 0, Set.of(1), fixtures));
        assertFalse(Arrays.equals(wet, drying));
        assertFalse(Arrays.equals(drying, dry));
    }

    @Test void onlyThreeCodingRecordsHaveIndependentHotspotsInEachRoom() {
        for (WorkshopRoom room : WorkshopRoom.values()) {
            WorkshopScene scene = new WorkshopScene(room);
            assertEquals(3, scene.spots().stream().filter(spot -> spot.id().isClue()).count());
            assertEquals(9, scene.spots().size());
        }
    }

    @Test void solvingTheGutterDrainsWaterFromTheBridgeAndRails() {
        var clock = new java.util.concurrent.atomic.AtomicLong();
        var scene = new WorkshopScene(WorkshopRoom.GREENHOUSE, clock::get);
        Rectangle bridge = new Rectangle(700, 640, 125, 100);
        Rectangle rail   = new Rectangle(606, 730, 60, 75);
        Rectangle floor  = new Rectangle(431, 740, 35, 44);
        int[] floodedBridge = region(scene, 0, Set.of(), bridge);
        int[] floodedRail   = paintedRegion(scene, rail, 0);
        int[] dryFloor      = paintedRegion(scene, floor, 0);
        assertArrayEquals(floodedBridge, region(scene, 0, Set.of(1), bridge));

        clock.set(230_000_000L);
        int[] drainingBridge = paintedRegion(scene, bridge, 0);
        assertFalse(Arrays.equals(floodedBridge, drainingBridge));
        clock.set(WorkshopScene.TRANSITION_MS * 1_000_000L);
        int[] exposedBridge = paintedRegion(scene, bridge, 0);
        assertFalse(Arrays.equals(drainingBridge, exposedBridge));
        assertFalse(Arrays.equals(floodedRail, paintedRegion(scene, rail, 0)));
        assertArrayEquals(dryFloor, paintedRegion(scene, floor, 0));
    }

    @Test void bridgeFollowsPartialAndCompletedPuzzlePlacement() {
        WorkshopScene scene = settledScene(WorkshopRoom.GREENHOUSE);
        Rectangle surround  = new Rectangle(1000, 638, 4, 2);
        Rectangle tray      = new Rectangle(592, 668, 24, 19);
        Rectangle gap       = new Rectangle(810, 741, 22, 12);
        int[] unchanged     = region(scene, 0, Set.of(1), surround);
        int[] looseCenter   = paintedRegion(scene, tray, 0);
        scene.updateBridge(new coderoom.shared.ui.BridgeArt.Placement(1, 1, false, 1, -1));
        int[] emptyGap = paintedRegion(scene, gap, 0);
        assertArrayEquals(looseCenter, paintedRegion(scene, tray, 0));
        scene.updateBridge(coderoom.shared.ui.BridgeArt.Placement.SOLVED);
        int[] joinedDeck = paintedRegion(scene, gap, 0);
        assertFalse(Arrays.equals(emptyGap, joinedDeck));
        assertFalse(Arrays.equals(looseCenter, paintedRegion(scene, tray, 0)));
        assertArrayEquals(unchanged, paintedRegion(scene, surround, 0));
        assertArrayEquals(joinedDeck, region(scene, 0, Set.of(1, 2), gap));
    }

    @Test void trolleyOutlineAvoidsTransparentCornersAndFollowsTheMovedAsset() {
        WorkshopScene scene = new WorkshopScene(WorkshopRoom.GREENHOUSE);
        WorkshopScene.Spot trolley = scene.spots().stream()
                .filter(spot -> spot.id() == RoomObjectId.FOURTH_DEVICE).findFirst().orElseThrow();
        assertTrue(scene.outline(trolley).contains(800, 851));
        assertFalse(scene.outline(trolley).contains(636, 787));

        scene.update(new GameView(GameStage.SAFE_OPEN, List.of(), null, 24, 5, 5,
                false, null, Set.of(0, 1, 2, 3, 4), Set.of()));
        assertFalse(scene.outline(trolley).contains(800, 851));
        assertTrue(scene.outline(trolley).contains(905, 727));
    }

    @Test void trolleyPassesThroughTheCrossingAndTurnsRightAlongTheRail() {
        var start = BridgeProjection.trolleyTrackPoint(0);
        var joint = BridgeProjection.trolleyTrackPoint(BridgeProjection.CART_TURN);
        var end   = BridgeProjection.trolleyTrackPoint(1);
        assertTrue(start.getY() > joint.getY());
        assertTrue(Math.abs(start.getX() - joint.getX()) < 3);
        assertEquals(joint.getY(), end.getY(), 0.01);
        assertTrue(end.getX() > joint.getX());
    }

    @Test void solvedPlatformsStayInsideThePitAndLeaveTheCartClear() {
        WorkshopScene scene = settledScene(WorkshopRoom.GREENHOUSE);
        WorkshopScene.Spot platforms = scene.spots().stream()
                .filter(spot -> spot.id() == RoomObjectId.THIRD_DEVICE).findFirst().orElseThrow();
        scene.update(new GameView(GameStage.START, List.of(), null, 18, 2, 5,
                false, null, Set.of(1, 2), Set.of()));
        var outline = scene.outline(platforms);
        assertTrue(outline.contains(810, 742));
        assertFalse(outline.contains(600, 650));
        assertFalse(outline.contains(800, 864));
        var trolley = scene.spots().stream().filter(spot -> spot.id() == RoomObjectId.FOURTH_DEVICE)
                .findFirst().orElseThrow();
        var overlap = new java.awt.geom.Area(outline);
        overlap.intersect(new java.awt.geom.Area(scene.outline(trolley)));
        assertTrue(overlap.isEmpty());
    }

    @Test void ambienceMovesInTheGlassAndSteamStopsAtTheRepairedPipe() {
        Rectangle window = new Rectangle(420, 218, 650, 219);
        Rectangle sill   = new Rectangle(405, 451, 685, 81);
        assertFalse(Arrays.equals(ambience(window, 0, false, false), ambience(window, 1.3, false, false)));
        assertArrayEquals(ambience(sill, 0, false, false), ambience(sill, 1.3, false, false));
        Rectangle pipe = new Rectangle(1255, 610, 240, 161);
        assertFalse(Arrays.equals(ambience(pipe, 0, true, false), ambience(pipe, 1.3, true, false)));
        assertArrayEquals(ambience(pipe, 0, true, true), ambience(pipe, 1.3, true, true));
    }

    @Test void engineRoomDarkensAndFlickersUntilPowerIsRestored() {
        WorkshopScene scene    = settledScene(WorkshopRoom.ENGINE);
        Rectangle overheadLamp = new Rectangle(811, 168, 1, 1);
        Rectangle doorLamp     = new Rectangle(1380, 168, 1, 1);
        Rectangle floor        = new Rectangle(295, 820, 1, 1);
        for (Rectangle sample : List.of(overheadLamp, doorLamp, floor)) {
            int dark = region(scene, 0, Set.of(), sample, 0)[0];
            int lit  = region(scene, 0, Set.of(0), sample, 0)[0];

            assertTrue(((lit >> 16) & 255) - ((dark >> 16) & 255) > 45);
            assertFalse(dark == region(scene, 0, Set.of(), sample, 2)[0]);
            assertEquals(dark, region(scene, 0, Set.of(), sample, 2.2)[0]);
            assertEquals(lit, region(scene, 0, Set.of(), sample, 2.42)[0]);
            assertEquals(dark, region(scene, 0, Set.of(), sample, 3)[0]);
            assertEquals(lit, region(scene, 0, Set.of(0), sample, 2.2)[0]);
        }
    }

    @Test void bothWorkshopDoorsAreStillOpeningAtOneSecond() {
        for (WorkshopRoom room : WorkshopRoom.values()) {
            var clock = new java.util.concurrent.atomic.AtomicLong();
            var scene = new WorkshopScene(room, clock::get);
            scene.update(new GameView(GameStage.ESCAPED, List.of(), null, 30, 5, 5,
                    false, null, Set.of(0, 1, 2, 3, 4), Set.of()));
            Rectangle door = room == WorkshopRoom.ENGINE ? new Rectangle(1285, 250, 220, 250)
                    : new Rectangle(1130, 200, 170, 250);

            int[] closed = paintedRegion(scene, door, 0);
            clock.set(1_000_000_000L);
            int[] midway = paintedRegion(scene, door, 0);
            clock.set(2_000_000_000L);
            int[] open = paintedRegion(scene, door, 0);
            assertFalse(Arrays.equals(closed, midway), room.name());
            assertFalse(Arrays.equals(midway, open), room.name());
            clock.set(3_000_000_000L);
            assertArrayEquals(open, paintedRegion(scene, door, 0), room.name());
        }
    }

    private static int[] ambience(Rectangle area, double seconds, boolean engine, boolean sealed) {
        var image = new BufferedImage(1586, 992, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        if (engine) { RoomAmbience.engine(g, seconds, sealed, 0); }
        else { RoomAmbience.greenhouse(g, seconds); }
        g.dispose();
        return image.getRGB(area.x, area.y, area.width, area.height, null, 0, area.width);
    }

    private static WorkshopScene settledScene(WorkshopRoom room) {
        var clock = new java.util.concurrent.atomic.AtomicLong();
        return new WorkshopScene(room, () -> clock.getAndAdd(2_000_000_000L));
    }

    private static int[] region(WorkshopScene scene, long seconds, Set<Integer> solved, Rectangle area) {
        return region(scene, seconds, solved, area, 0);
    }

    private static int[] region(WorkshopScene scene, long seconds, Set<Integer> solved,
            Rectangle area, double ambientSeconds) {
        scene.update(new GameView(GameStage.START, List.of(), null, seconds, solved.size(), 5,
                false, null, solved, Set.of()));
        return paintedRegion(scene, area, ambientSeconds);
    }

    private static int[] paintedRegion(WorkshopScene scene, Rectangle area, double ambientSeconds) {
        var image    = new BufferedImage(1586, 992, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        scene.paint(graphics, ambientSeconds);
        graphics.dispose();
        return image.getRGB(area.x, area.y, area.width, area.height, null, 0, area.width);
    }
}
