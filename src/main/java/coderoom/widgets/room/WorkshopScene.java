package coderoom.widgets.room;

import coderoom.entities.game.GameSession;
import coderoom.entities.game.GameStage;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.game.WorkshopRoom;
import coderoom.features.gameplay.GameView;
import coderoom.shared.ui.BridgeArt;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import javax.imageio.ImageIO;

/** Draws the installed room with the positions captured in the master art. */
final class WorkshopScene {
    record Spot(RoomObjectId id, String title, Rectangle area) { }
    private record Layer(int x, int y, BufferedImage registered, BufferedImage movable, Shape outline) { }

    static final int TRANSITION_MS = 1030;
    private static final Shape DRAIN_BASIN = SpriteOutline.polygon(
            617, 603, 964, 603, 1159, 891, 407, 891);
    private static final Shape FLOOD_SURFACE = SpriteOutline.polygon(
            622, 610, 962, 610, 1149, 888, 420, 888);
    private static final List<RoomObjectId> DEVICE_IDS = List.of(RoomObjectId.DRAWER, RoomObjectId.WALL_DIAL,
            RoomObjectId.THIRD_DEVICE, RoomObjectId.FOURTH_DEVICE, RoomObjectId.FIFTH_DEVICE);
    private static final List<RoomObjectId> EVIDENCE_IDS = List.of(RoomObjectId.EVIDENCE_1, RoomObjectId.EVIDENCE_2,
            RoomObjectId.EVIDENCE_3, RoomObjectId.EVIDENCE_4, RoomObjectId.EVIDENCE_5);
    private static final int[][] ENGINE_DEVICES = {
        {249, 221, 219, 430}, {1170, 681, 335, 153}, {662, 211, 289, 335},
        {516, 690, 422, 142}, {1091, 329, 147, 130}, {1285, 221, 223, 351}
    };
    private static final int[][] GREENHOUSE_DEVICES = {
        {90, 503, 133, 90}, {1346, 302, 131, 267}, {599, 625, 382, 173},
        {640, 797, 312, 130}, {982, 743, 135, 154}, {1124, 153, 195, 366}
    };
    private static final int[][] ENGINE_EVIDENCE = {
        {203, 188, 40, 50}, {}, {736, 468, 67, 38}, {}, {1240, 357, 44, 38}
    };
    private static final int[][] GREENHOUSE_EVIDENCE = {
        {217, 510, 161, 49}, {}, {}, {420, 497, 57, 33}, {1117, 531, 53, 38}
    };

    private final WorkshopRoom room;
    private final LongSupplier clock;
    private final String path;
    private final BufferedImage base;
    private final BufferedImage drainedBed;
    private final BridgeProjection bridge;
    private final BufferedImage repairedPipe;
    private final BufferedImage openEnvelope;
    private final Map<String, Layer> layers = new LinkedHashMap<>();
    private final List<Spot> spots;
    private final Map<RoomObjectId, Shape> outlines = new java.util.EnumMap<>(RoomObjectId.class);
    private final Map<RoomObjectId, Shape> silhouettes = new java.util.EnumMap<>(RoomObjectId.class);
    private Set<Integer> outlinedSteps = Set.of();
    private GameView state;

    WorkshopScene(WorkshopRoom room) { this(room, System::nanoTime); }

    WorkshopScene(WorkshopRoom room, LongSupplier clock) {
        this.room  = room;
        this.clock = clock;
        this.path = "/images/room/workshop/" + room.name().toLowerCase(Locale.ROOT) + "/";
        base    = image("background.jpg");
        boolean greenhouse = room == WorkshopRoom.GREENHOUSE;
        drainedBed = greenhouse ? image("drained-bed.jpg") : null;
        bridge     = greenhouse ? new BridgeProjection(
                readResource("/images/game-ui/workshop/parts/cross-platform.png"),
                readResource("/images/game-ui/workshop/parts/long-platform.png")) : null;
        repairedPipe = room == WorkshopRoom.ENGINE ? image("pipe-repaired.png") : null;
        spots   = buildSpots(room);
        openEnvelope = room == WorkshopRoom.GREENHOUSE
                ? readResource("/images/game-ui/workshop/parts/seed-open.png") : null;
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(
                stream("layers.txt"), StandardCharsets.UTF_8))) {
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                String[] fields = line.split(" ");
                String id             = fields[0];
                int x                 = Integer.parseInt(fields[1]);
                int y                 = Integer.parseInt(fields[2]);
                BufferedImage movable = image("layers/" + id + "-movable.png");
                Shape outline         = SpriteOutline.fromAlpha(movable,
                        new Rectangle(x, y, movable.getWidth(), movable.getHeight()));
                layers.put(id, new Layer(x, y, image("layers/" + id + ".png"), movable, outline));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("방의 오브젝트를 불러오지 못했습니다.", exception);
        }
    }

    private static List<Spot> buildSpots(WorkshopRoom room) {
        boolean engine = room == WorkshopRoom.ENGINE;
        int[][] devices = engine ? ENGINE_DEVICES : GREENHOUSE_DEVICES;
        int[][] records = engine ? ENGINE_EVIDENCE : GREENHOUSE_EVIDENCE;
        java.util.ArrayList<Spot> result = new java.util.ArrayList<>();
        for (int index = 0; index < 5; index++) {
            if (room.task(index).hasEvidence()) {
                result.add(new Spot(EVIDENCE_IDS.get(index), room.task(index).evidence(), rectangle(records[index])));
            }
        }
        for (int index = 0; index < 5; index++) {
            result.add(new Spot(DEVICE_IDS.get(index), room.task(index).title(), rectangle(devices[index])));
        }
        result.add(new Spot(RoomObjectId.DOOR, "출구", rectangle(devices[5])));
        return List.copyOf(result);
    }

    private static Rectangle rectangle(int[] values) {
        return new Rectangle(values[0], values[1], values[2], values[3]);
    }

    BufferedImage background() { return base; }

    List<Spot> spots() { return spots; }

    private int movingStep = -1;
    private long movementStarted;
    private long doorStarted;

    boolean update(GameView view) {
        boolean moved = false;
        if (state != null && state.solvedCount() < view.solvedCount()) {
            for (int step : view.solvedSteps()) {
                if (!state.solvedSteps().contains(step)) { movingStep = step; }
            }
            movementStarted = clock.getAsLong();
            moved = true;
        }
        if (escaped(view) && !escaped(state)) {
            doorStarted = clock.getAsLong();
            moved = true;
        }
        state = view;
        if (bridge != null && view.solvedSteps().contains(2)) { updateBridge(BridgeArt.Placement.SOLVED); }
        if (!outlinedSteps.equals(view.solvedSteps())) {
            outlinedSteps = view.solvedSteps();
            outlines.clear();
            silhouettes.clear();
        }
        return moved;
    }

    private static boolean escaped(GameView view) {
        return view != null && view.stage() == GameStage.ESCAPED;
    }

    private double movementProgress(int step) {
        if (step != movingStep) { return 1; }
        double time = Math.min(1, (clock.getAsLong() - movementStarted) / (TRANSITION_MS * 1_000_000.0));
        return 1 - Math.pow(1 - time, 3);
    }

    private int slide(int target, int step) {
        return (int) Math.round(target * movementProgress(step));
    }

    void updateBridge(BridgeArt.Placement placement) {
        if (bridge != null && bridge.update(placement)) {
            outlines.remove(RoomObjectId.THIRD_DEVICE);
            silhouettes.remove(RoomObjectId.THIRD_DEVICE);
        }
    }

    Shape outline(Spot spot) {
        return outlines.computeIfAbsent(spot.id(), ignored -> buildOutline(spot));
    }

    Shape silhouette(Spot spot) {
        return silhouettes.computeIfAbsent(spot.id(), ignored -> SpriteOutline.exterior(outline(spot)));
    }

    private Shape buildOutline(Spot spot) {
        int index = spot.id().evidenceIndex();
        if (index >= 0) {
            if (room == WorkshopRoom.GREENHOUSE && index == 0) {
                return partOutline("specimen-cards");
            }
            Rectangle bounds = spot.area();
            int x = bounds.x + bounds.width / 5;
            int y = bounds.y + bounds.height / 5;
            int w = bounds.width * 3 / 5;
            int h = bounds.height * 3 / 5;
            if (room == WorkshopRoom.ENGINE && index != 0) {
                return new RoundRectangle2D.Double(x, y, w, h, 3, 3);
            }
            return SpriteOutline.polygon(x, y + 4, x + w, y, x + w - 3, y + h - 3, x + 1, y + h);
        }
        return room == WorkshopRoom.ENGINE ? engineOutline(spot.id()) : greenhouseOutline(spot.id());
    }

    private Shape engineOutline(RoomObjectId id) {
        return switch (id) {
            case DRAWER -> SpriteOutline.polygon(
                    210, 156, 245, 153, 318, 195, 419, 195, 420, 181, 456, 183, 476, 230,
                    474, 653, 487, 667, 472, 682, 413, 681, 413, 647, 247, 647, 242, 679,
                    201, 680, 195, 666, 212, 617);
            case WALL_DIAL -> {
                Area pipe = new Area(SpriteOutline.polygon(
                        1106, 683, 1130, 658, 1184, 663, 1234, 690, 1355, 718,
                        1468, 751, 1537, 765, 1586, 794, 1586, 893,
                        1530, 871, 1470, 838, 1360, 795, 1250, 758, 1162, 732, 1112, 716));
                for (String sleeve : List.of("sleeve-large", "sleeve-medium", "sleeve-small")) {
                    if (!installedSleeve(sleeve)) { pipe.add(new Area(partOutline(sleeve))); }
                }
                yield pipe;
            }
            case THIRD_DEVICE -> SpriteOutline.polygon(664, 227, 685, 208, 716, 208, 724, 226,
                    900, 226, 909, 208, 930, 208, 944, 226, 955, 238, 950, 544,
                    957, 545, 957, 557, 654, 557, 654, 544, 663, 542);
            case FOURTH_DEVICE -> {
                Area controls = new Area(new Ellipse2D.Double(516, 700, 120, 121));
                controls.add(new Area(SpriteOutline.polygon(
                        642, 775, 907, 774, 935, 799, 934, 831, 640, 831)));
                for (String name : List.of("lever-1", "lever-2", "lever-3", "lever-4")) {
                    controls.add(new Area(solved(3) ? movedOutline(name, 0, 11, -.13, 13, 74) : partOutline(name)));
                }
                yield controls;
            }
            case FIFTH_DEVICE -> new RoundRectangle2D.Double(1091, 330, 147, 125, 6, 6);
            case DOOR -> SpriteOutline.polygon(1257, 201, 1535, 201, 1535, 635, 1503, 635,
                    1503, 597, 1257, 580);
            default -> throw new IllegalArgumentException("Unknown machine room object " + id);
        };
    }

    private Shape greenhouseOutline(RoomObjectId id) {
        return switch (id) {
            case DRAWER -> partOutline("seed-chest");
            case WALL_DIAL -> {
                Area tank = new Area(SpriteOutline.polygon(1347, 390, 1370, 377, 1390, 373,
                        1397, 313, 1425, 313, 1415, 374, 1467, 382, 1480, 394, 1476, 547,
                        1467, 570, 1379, 574, 1352, 562, 1343, 522));
                yield tank;
            }
            case THIRD_DEVICE -> {
                Area deck  = new Area(bridge.outline());
                Shape cart = greenhouseOutline(RoomObjectId.FOURTH_DEVICE);
                deck.subtract(new Area(cart));
                deck.subtract(new Area(new BasicStroke(9).createStrokedShape(cart)));
                yield deck;
            }
            case FOURTH_DEVICE -> solved(4) ? arrivedTrolley().createTransformedShape(partOutline("trolley"))
                    : partOutline("trolley");
            case FIFTH_DEVICE -> SpriteOutline.polygon(933, 735, 953, 733, 1028, 885,
                    1025, 901, 974, 901, 976, 889);
            case DOOR -> SpriteOutline.polygon(1122, 151, 1325, 151, 1325, 169, 1317, 169,
                    1317, 520, 1108, 520, 1108, 170, 1122, 170);
            default -> throw new IllegalArgumentException("Unknown greenhouse object " + id);
        };
    }

    private Shape partOutline(String name) { return layers.get(name).outline(); }

    private Shape movedOutline(String name, int dx, int dy, double radians, int pivotX, int pivotY) {
        Layer part = layers.get(name);
        return movement(dx, dy, radians, part.x() + pivotX, part.y() + pivotY)
                .createTransformedShape(part.outline());
    }

    private AffineTransform arrivedTrolley() { return trolleyPosition(1); }

    private AffineTransform trolleyPosition(double progress) {
        Layer part = layers.get("trolley");
        double x = part.x() + (727 - part.x()) * progress;
        double y = part.y() + (593 - part.y()) * progress;
        double scale = 1 - .5 * progress;
        AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
        transform.scale(scale, scale);
        transform.translate(-part.x(), -part.y());
        return transform;
    }

    void paint(Graphics2D graphics, double ambientSeconds) {
        paint(graphics, ambientSeconds, null);
    }

    void paint(Graphics2D graphics, double ambientSeconds, Rectangle surroundBounds) {
        boolean engine = room == WorkshopRoom.ENGINE;
        long elapsed = state == null ? 0 : state.elapsedSeconds();
        int phase = phase(elapsed);
        if (surroundBounds == null) { graphics.drawImage(base, 0, 0, null); }
        if (!engine) {
            Graphics2D floor = (Graphics2D) graphics.create();
            floor.clip(DRAIN_BASIN);
            floor.drawImage(drainedBed, 0, 0, null);
            bridge.paint(floor);
            floor.dispose();
        }
        if (engine) {
            paintEngine(graphics, phase, elapsed);
            RoomAmbience.engine(graphics, ambientSeconds, solved(1), phase);
        } else {
            RoomAmbience.greenhouse(graphics, ambientSeconds);
            paintGreenhouse(graphics, phase, ambientSeconds);
        }
        paintEvidence(graphics);
        if (engine) {
            Rectangle bounds = surroundBounds == null
                    ? new Rectangle(0, 0, base.getWidth(), base.getHeight()) : surroundBounds;
            RoomAmbience.engineLighting(graphics, bounds, ambientSeconds, solved(0));
        }
    }

    static int phase(long seconds) {
        long bounded = Math.min(GameSession.TIME_LIMIT_SECONDS, Math.max(0, seconds));
        return (int) (bounded * 4 / GameSession.TIME_LIMIT_SECONDS);
    }

    private void paintEngine(Graphics2D graphics, int phase, long elapsed) {
        float repairProgress = solved(1) ? (float) movementProgress(1) : 0;
        if (repairProgress > 0) {
            var repair = (Graphics2D) graphics.create();
            repair.setComposite(java.awt.AlphaComposite.SrcOver.derive(repairProgress));
            repair.drawImage(repairedPipe, 1095, 580, null);
            repair.dispose();
        }
        for (Map.Entry<String, Layer> entry : layers.entrySet()) {
            String id = entry.getKey();
            Layer part = entry.getValue();
            if (id.equals("door-bolt") && escaped(state)) { continue; }
            if (id.equals("pressure-needle")) {
                if (phase == 0 && !solved(3)) { at(graphics, part); }
                else { needle(graphics, solved(3) ? -60 - slide(58, 3) : -60 + (int) Math.min(150, elapsed * 150 / GameSession.TIME_LIMIT_SECONDS)); }
            } else if (id.startsWith("lever-") && solved(3)) {
                move(graphics, part, 0, slide(11, 3), -.13 * movementProgress(3), part.x() + 13, part.y() + 74);
            } else if (installedSleeve(id)) {
                if (repairProgress < 1) {
                    var rack = (Graphics2D) graphics.create();
                    rack.setComposite(java.awt.AlphaComposite.SrcOver.derive(1 - repairProgress));
                    at(rack, part);
                    rack.dispose();
                }
            } else if (id.equals("door-bolt") && phase < 4 && solved(4)) {
                move(graphics, part, slide(27, 4), 0, 0, 0, 0);
            } else {
                at(graphics, part);
            }
        }
        if (solved(0)) {
            var rack = (Graphics2D) graphics.create();
            rack.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) movementProgress(0)));
            for (int index : state.poweredModules()) { glow(rack, 274, 246 + index * 85, new Color(173, 216, 117)); }
            rack.dispose();
        } else if (phase >= 1) {
            glow(graphics, 450, 326, Color.ORANGE);
        }
        if (solved(2)) {
            var distribution = (Graphics2D) graphics.create();
            distribution.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) movementProgress(2)));
            glow(distribution, 852, 357, new Color(147, 221, 120));
            distribution.dispose();
        }
        if (!solved(3) && phase >= 2) {
            for (int index = 0; index < phase + 1; index++) {
                graphics.setColor(new Color(219, 231, 233, 24 + index * 6));
                graphics.fillOval(627 - index * 9, 780 - 21 * index, 39 + index * 7, 12 + 3 * index);
            }
        }
        if (phase >= 3) {
            glow(graphics, 1372, 168, new Color(255, 121, 51));
            graphics.setColor(new Color(255, 135, 58, 45));
            graphics.fillPolygon(new int[] {1371, 1202, 1510}, new int[] {167, 305, 315}, 3);
        }
        paintEngineMarkings(graphics);
        if (escaped(state)) { paintOpeningDoor(graphics, true); }
        else if (phase == 4) { shutter(graphics, 1305, 252, 201, 287); }
    }

    private void paintGreenhouse(Graphics2D graphics, int phase, double ambientSeconds) {
        for (Map.Entry<String, Layer> item : layers.entrySet()) {
            String id = item.getKey();
            Layer part = item.getValue();
            switch (id) {
                case "seed-chest" -> {
                    if (!solved(3)) { at(graphics, part); }
                }
                case "trolley" -> { } // The cart stays in front of the water and platforms.
                case "catchment-float" -> {
                    if (phase < 2) { at(graphics, part); }
                    else { move(graphics, part, 0, -5 * (phase - 1), -.07 * phase, 1266, 241); }
                }
                default -> at(graphics, part);
            }
        }
        double drainedFraction = solved(1) ? movementProgress(1) : 0;
        paintFloodSurface(graphics, phase, ambientSeconds, drainedFraction);
        Layer cart = layers.get("trolley");
        if (solved(4)) {
            AffineTransform transform = trolleyPosition(movementProgress(4));
            transform.translate(cart.x(), cart.y());
            graphics.drawImage(cart.movable(), transform, null);
        } else {
            Graphics2D foreground = (Graphics2D) graphics.create();
            foreground.clip(cart.outline());
            at(foreground, cart);
            foreground.dispose();
        }
        if (solved(0) && !solved(3)) {
            var seeds = (Graphics2D) graphics.create();
            seeds.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) movementProgress(0)));
            seeds.rotate(-.23, 190, 557);
            seeds.drawImage(openEnvelope, 172, 542, 32, 28, null);
            seeds.dispose();
        }
        if (solved(3)) {
            Layer chest   = layers.get("seed-chest");
            Layer trolley = layers.get("trolley");
            int x = chest.x() + slide(trolley.x() + 63 - chest.x(), 3);
            int y = chest.y() + slide(trolley.y() - 18 - chest.y(), 3);
            int width  = chest.movable().getWidth();
            int height = chest.movable().getHeight();
            var placement = (Graphics2D) graphics.create();
            if (solved(4)) { placement.transform(trolleyPosition(movementProgress(4))); }
            placement.drawImage(chest.movable(), x, y, width, height, null);
            placement.dispose();
        }
        paintGreenhouseMarkings(graphics);
        if (escaped(state)) { paintOpeningDoor(graphics, false); }
        else if (phase == 4) { shutter(graphics, 1140, 177, 158, 336); }
        else if (solved(4)) {
            int alpha = slide(156, 4);
            graphics.setColor(new Color(239, 190, 111, alpha));
            graphics.fillRect(1165, 506, 112, 3);
            graphics.setColor(new Color(229, 170, 87, alpha / 4));
            graphics.fillPolygon(new int[] {1165, 1277, 1319, 1109}, new int[] {509, 509, 572, 571}, 4);
        }
    }

    private void paintOpeningDoor(Graphics2D graphics, boolean engine) {
        double time     = Math.min(1, Math.max(0, (clock.getAsLong() - doorStarted)
                / (RoomPanel.DOOR_OPEN_MS * 1_000_000.0)));
        double angle    = (time * time * (3 - 2 * time)) * 1.32;
        Shape opening   = engine ? SpriteOutline.polygon(1283, 222, 1510, 222, 1510, 598, 1283, 582)
                : new Rectangle(1128, 174, 178, 340);
        Rectangle frame = opening.getBounds();
        int hinge        = frame.x + frame.width;

        Graphics2D doorway = (Graphics2D) graphics.create();
        try {
            doorway.clip(opening);
            doorway.setPaint(new GradientPaint(frame.x, frame.y, new Color(10, 13, 14),
                    frame.x, frame.y + frame.height, new Color(65, 55, 38)));
            doorway.fill(opening);
            doorway.translate(hinge, 0);
            doorway.scale(Math.cos(angle), 1);
            doorway.translate(-hinge, 0);
            doorway.clip(opening);
            doorway.drawImage(base, 0, 0, null);
            if (engine) { move(doorway, layers.get("door-bolt"), slide(27, 4), 0, 0, 0, 0); }
        } finally {
            doorway.dispose();
        }
    }

    private static void at(Graphics2D graphics, Layer part) {
        graphics.drawImage(part.registered(), part.x(), part.y(), null);
    }

    private static void move(Graphics2D graphics, Layer part, int dx, int dy, double radians, int x, int y) {
        AffineTransform transform = movement(dx, dy, radians, x, y);
        transform.translate(part.x(), part.y());
        graphics.drawImage(part.movable(), transform, null);
    }

    private static AffineTransform movement(int dx, int dy, double radians, int x, int y) {
        AffineTransform transform = AffineTransform.getRotateInstance(radians, x + dx, y + dy);
        transform.translate(dx, dy);
        return transform;
    }

    private static void needle(Graphics2D graphics, int degrees) {
        double angle = Math.toRadians(degrees);
        int x = 576 + (int) (31 * Math.cos(angle));
        int y = 761 + (int) (31 * Math.sin(angle));
        graphics.setColor(new Color(52, 30, 16));
        java.awt.Stroke stroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(4));
        graphics.drawLine(576, 761, x, y);
        graphics.setStroke(stroke);
        graphics.fillOval(570, 755, 12, 12);
    }

    private static void glow(Graphics2D graphics, int x, int y, Color color) {
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 58));
        graphics.fillOval(x - 10, y - 10, 21, 21);
        graphics.setColor(color);
        graphics.fillOval(x - 4, y - 4, 9, 9);
    }

    private static void shutter(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setColor(new Color(42, 49, 43));
        graphics.fillRect(x, y, width, height);
        graphics.setColor(new Color(113, 108, 78));
        for (int row = y + 8; row < y + height; row += 12) {
            graphics.drawLine(x + 5, row, x + width - 5, row);
        }
    }

    private void paintEngineMarkings(Graphics2D graphics) {
        if (!solved(1)) {
            layerMark(graphics, 1261, 638, 1);
            layerMark(graphics, 1363, 657, 2);
            layerMark(graphics, 1447, 676, 3);
            layerMark(graphics, 1356, 761, 2);
        }
        if (!solved(3)) {
            // A bare pull tab and a linked set of pin heads belong to the interlock assembly.
            graphics.setColor(new Color(165, 110, 51));
            graphics.drawLine(711, 801, 873, 801);
            for (int x : new int[] {712, 769, 826, 873}) {
                graphics.setColor(x == 873 ? new Color(225, 178, 95) : new Color(92, 58, 35));
                graphics.fillOval(x - 4, 797, 8, 8);
            }
        }
    }

    private static void layerMark(Graphics2D graphics, int x, int y, int count) {
        graphics.setColor(new Color(224, 191, 132, 207));
        for (int line = 0; line < count; line++) {
            int offset = line * 5;
            graphics.drawLine(x + offset, y, x + offset + 4, y + 13);
        }
    }

    private void paintFloodSurface(Graphics2D graphics, int phase, double seconds, double drainedFraction) {
        if (drainedFraction >= 1) { return; }

        int edgeY = (int) Math.round(888 - 278 * drainedFraction);
        Graphics2D water = (Graphics2D) graphics.create();
        try {
            water.clip(FLOOD_SURFACE);
            water.clipRect(420, 610, 729, edgeY - 609);
            water.setComposite(AlphaComposite.SrcOver.derive((float) (1 - .5 * drainedFraction)));
            water.setPaint(new GradientPaint(0, 610, new Color(20, 53, 71, 206),
                    0, 888, new Color(17, 71, 92, 222)));
            water.fill(FLOOD_SURFACE);
            // Sample the undisturbed reflection between the rails in the base art.
            water.setComposite(AlphaComposite.SrcOver.derive((float) (.31 * (1 - drainedFraction))));
            water.drawImage(base, 420, 610, 1149, 888, 719, 620, 869, 812, null);
            water.setComposite(AlphaComposite.SrcOver.derive((float) (1 - .5 * drainedFraction)));

            // Broken streaks carry the window reflection across the submerged objects.
            for (int i = 0; i < 48; i++) {
                int y         = 615 + i * 41 % 273;
                double spread = (y - 610) / 278.0;
                int x         = 558 + i * 83 % 450;
                double drift  = Math.sin(seconds * 1.3 + i * .92) * (3 + 7 * spread);
                double width  = 15 + i * 13 % 52 + 33 * spread;
                var glint = new Path2D.Double();
                glint.moveTo(x + drift, y);
                glint.quadTo(x + drift + width * .44, y - 2 - spread * 3, x + drift + width, y + 1);
                water.setColor(new Color(121, 184, 212, 25 + i % 4 * 8));
                water.draw(glint);
            }
            RoomAmbience.basinRipples(water, seconds, phase);
            water.setColor(new Color(139, 195, 215, drainedFraction > 0 ? 95 : 36));
            water.drawLine(420, edgeY - 1, 1149, edgeY - 1);
        } finally {
            water.dispose();
        }
    }

    private void paintGreenhouseMarkings(Graphics2D graphics) {
        if (!solved(1)) {
            // The arrow and the outlet share the same metal fitting; the arrow is not a collectible.
            graphics.setColor(new Color(170, 207, 198, 186));
            graphics.drawLine(1388, 533, 1367, 533);
            graphics.drawLine(1367, 533, 1375, 529);
            graphics.drawLine(1367, 533, 1375, 537);
        }
    }

    private void paintEvidence(Graphics2D graphics) {
        for (Spot spot : spots) {
            int index = spot.id().evidenceIndex();
            if (index < 0) { continue; }
            // The specimen cards are already part of the master scene.
            if (room == WorkshopRoom.GREENHOUSE && index == 0) { continue; }
            Rectangle bounds = spot.area();
            boolean found = state != null && state.evidence().contains(index);
            int x = bounds.x + bounds.width / 5;
            int y = bounds.y + bounds.height / 5;
            int w = bounds.width * 3 / 5;
            int h = bounds.height * 3 / 5;
            if (room == WorkshopRoom.ENGINE && index != 0) {
                paintPlate(graphics, index, x, y, w, h, found);
            } else {
                paintCard(graphics, index, x, y, w, h, found);
            }
        }
    }

    private static void paintPlate(Graphics2D graphics, int index, int x, int y, int w, int h, boolean found) {
        graphics.setColor(new Color(47, 32, 20));
        graphics.fillRoundRect(x - 1, y + 2, w + 2, h, 3, 3);
        graphics.setColor(found ? new Color(112, 93, 60) : new Color(167, 134, 77));
        graphics.fillRoundRect(x, y, w, h, 3, 3);
        graphics.setColor(new Color(46, 32, 19));
        graphics.fillOval(x + 3, y + h / 2 - 1, 3, 3);
        graphics.fillOval(x + w - 6, y + h / 2 - 1, 3, 3);
        int center = x + w / 2;
        if (index == 2) {
            for (int bar = 0; bar < 3; bar++) {
                int height = 4 + bar * 3;
                graphics.fillRect(center - 7 + 5 * bar, y + h - height - 4, 2, height);
            }
        } else {
            graphics.drawLine(center - 6, y + h / 2, center - 2, y + h / 2 + 4);
            graphics.drawLine(center - 2, y + h / 2 + 4, center + 5, y + h / 2 - 4);
        }
    }

    private static void paintCard(Graphics2D graphics, int index, int x, int y, int w, int h, boolean found) {
        int[] xs = {x, x + w, x + w - 3, x + 1};
        int[] ys = {y + 4, y, y + h - 3, y + h};
        graphics.setColor(new Color(31, 24, 16, 171));
        graphics.fillPolygon(xs, new int[] {ys[0] + 3, ys[1] + 3, ys[2] + 3, ys[3] + 3}, 4);
        graphics.setColor(found ? new Color(175, 149, 102) : new Color(217, 184, 125));
        graphics.fillPolygon(xs, ys, 4);
        graphics.setColor(new Color(97, 68, 43));
        int center = x + w / 2;
        if (index == 4) {
            graphics.drawLine(x + 5, y + h - 5, center, y + h / 2);
            graphics.drawLine(center, y + h / 2, x + w - 5, y + 4);
            graphics.drawLine(center, y + h / 2, x + w - 6, y + h - 7);
        } else {
            graphics.drawLine(x + 5, y + 8, x + w - 7, y + 6);
            graphics.drawLine(x + 5, y + 13, x + w - 10, y + 11);
        }
    }

    private boolean installedSleeve(String id) {
        if (!solved(1)) { return false; }
        return switch (state.installedSleeve()) {
            case 'A' -> id.equals("sleeve-large");
            case 'C' -> id.equals("sleeve-small");
            default -> id.equals("sleeve-medium");
        };
    }

    private boolean solved(int step) { return state != null && state.solvedSteps().contains(step); }

    private BufferedImage image(String name) { return readResource(path + name); }

    private static BufferedImage readResource(String name) {
        try (InputStream input = WorkshopScene.class.getResourceAsStream(name)) {
            if (input == null) { throw new IOException(name); }
            return ImageIO.read(input);
        } catch (IOException exception) {
            throw new IllegalStateException("방 그림을 읽을 수 없습니다: " + name, exception);
        }
    }

    private InputStream stream(String name) throws IOException {
        InputStream input = WorkshopScene.class.getResourceAsStream(path + name);
        if (input == null) { throw new IOException(path + name); }
        return input;
    }
}
