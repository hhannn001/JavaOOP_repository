package coderoom.widgets.room;

import coderoom.entities.game.RoomObjectId;
import coderoom.entities.game.GameStage;
import coderoom.entities.game.WorkshopRoom;
import coderoom.features.gameplay.GameView;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * 담당: D
 * 역할: 방 그림과 물건 클릭 영역
 * 상위 타입: javax.swing.JPanel
 *
 * 구현 범위: 방 배경 그리기 + 창 크기에 맞춘 확대·축소 + 서랍·다이얼·문 등의 클릭 처리
 * + 진행 단계별 그림 겹치기(서랍 열림, 다이얼 해제, 문 열림). 겹침 그림은 배경과 같은 크기의 투명 PNG다.
 *
 * 핵심 아이디어: 클릭 영역을 "화면 좌표"가 아니라 "그림 원본 좌표(1586×992)"로 적어 두고,
 * 그리기와 클릭 판정이 항상 같은 변환(layout)을 쓰게 해서 그림과 클릭 영역이 어긋나지 않게 한다.
 */
public class RoomPanel extends JPanel {
    private static final Logger LOG = Logger.getLogger(RoomPanel.class.getName());

    /** 방 배경 그림의 위치. src/main/resources 아래를 기준으로 한 경로다. */
    private static final String BACKGROUND_PATH = "/images/room/study_locked.png";

    /** 단계 그림 3장. 해당 단계 "이상"이면 배경 위에 차례로 겹쳐 그린다. */
    private static final String[] OVERLAY_PATHS = {
        "/images/room/drawer_open.png", "/images/room/dial_solved.png", "/images/room/door_open.png"};
    private static final GameStage[] OVERLAY_FROM = {
        GameStage.DRAWER_OPEN, GameStage.SAFE_OPEN, GameStage.ESCAPED};

    /** 물건을 클릭하면 화면이 이 시간 동안 걷듯이 출렁이며 물건 쪽으로 다가가거나 원래 시점으로 돌아간다. */
    private static final int WALK_MS = 450;
    private static final int DEVICE_TRANSITION_MS = 240;
    static final int DOOR_OPEN_MS = 2000;
    private static final Rectangle STUDY_DOOR_LEAF = new Rectangle(1264, 149, 238, 579);
    private static final int OPEN_DOOR_HOLD_MS = 160;
    private static final double WALK_BOB = 6;    // 위아래로 흔들리는 폭(화면 픽셀)
    private static final double WALK_ZOOM = 0.30; // 다가가며 커지는 비율

    /** 그림 비율이 창과 다를 때 남는 빈 공간의 색. 프로토타입의 짙은 녹색(--deep)과 같다. */
    private static final Color LETTERBOX = new Color(0x15271E);
    private static final Color GOLD      = new Color(0xE5C286);
    private static final int SURROUND_SIDE = 260;
    private static final int SURROUND_TOP  = 60;

    /**
     * 클릭할 수 있는 물건 목록. Rectangle(x, y, 가로, 세로)이며 단위는 "그림 원본 픽셀"이다.
     * 겹치는 곳이 있으면 목록 앞쪽이 우선한다. 위치가 어긋나면 이 숫자만 고치면 된다.
     */
    private static final List<Hotspot> HOTSPOTS = List.of(
            new Hotspot(RoomObjectId.DRAWER,    "서랍장",      new Rectangle(522, 494, 290, 258)),
            new Hotspot(RoomObjectId.WALL_DIAL, "벽의 다이얼", new Rectangle(945, 285, 193, 195)),
            new Hotspot(RoomObjectId.DOOR,      "출입문",      new Rectangle(1233, 112, 289, 624)),
            new Hotspot(RoomObjectId.BOOKS,     "책장",        new Rectangle(520, 170, 332, 248)),
            new Hotspot(RoomObjectId.WINDOW,    "창문",        new Rectangle(118, 72, 280, 408)));

    /** 클릭 가능한 물건 하나: 무엇인지(id), 마우스를 올렸을 때 보일 이름, 차지하는 영역. */
    private record Hotspot(RoomObjectId id, String label, Rectangle area) { }

    /** 그림을 화면 어디에 얼마나 크게 그리는지. 그리기와 클릭 판정이 이 값을 함께 쓴다. */
    private record Layout(double scale, double offsetX, double offsetY) {
        /** 화면(패널) 좌표를 그림 원본 좌표로 되돌린다. */
        Point2D.Double toImage(Point screen) {
            return new Point2D.Double((screen.x - offsetX) / scale, (screen.y - offsetY) / scale);
        }
    }

    private static final Hotspot STUDY_NOTE =
            new Hotspot(RoomObjectId.HINT_NOTE, "접힌 메모", new Rectangle(675, 463, 102, 58));

    private static final List<Hotspot> ENGINE_DEVICES = List.of(
            new Hotspot(RoomObjectId.DRAWER, "비상 전력함", new Rectangle(298, 485, 155, 105)),
            new Hotspot(RoomObjectId.WALL_DIAL, "가스 밸브", new Rectangle(480, 485, 155, 105)),
            new Hotspot(RoomObjectId.THIRD_DEVICE, "전력 분배기", new Rectangle(662, 485, 155, 105)),
            new Hotspot(RoomObjectId.FOURTH_DEVICE, "압력 해제 레버", new Rectangle(844, 485, 155, 105)),
            new Hotspot(RoomObjectId.FIFTH_DEVICE, "중앙 제어판", new Rectangle(1026, 485, 155, 105)),
            new Hotspot(RoomObjectId.DOOR, "출입문", new Rectangle(1280, 240, 280, 522)));
    private static final List<Hotspot> ARCHIVE_DEVICES = List.of(
            new Hotspot(RoomObjectId.DRAWER, "서류함", new Rectangle(408, 445, 154, 100)),
            new Hotspot(RoomObjectId.WALL_DIAL, "기록 카드함", new Rectangle(620, 445, 154, 100)),
            new Hotspot(RoomObjectId.THIRD_DEVICE, "필름 봉투함", new Rectangle(835, 445, 154, 100)),
            new Hotspot(RoomObjectId.FOURTH_DEVICE, "네 번째 장치", new Rectangle(490, 610, 136, 92)),
            new Hotspot(RoomObjectId.FIFTH_DEVICE, "다섯 번째 장치", new Rectangle(850, 610, 136, 92)),
            new Hotspot(RoomObjectId.DOOR, "출입문", new Rectangle(1300, 158, 260, 586)));
    private static final List<Hotspot> STUDY_EXTRA = List.of(
            new Hotspot(RoomObjectId.THIRD_DEVICE, "세 번째 장치", new Rectangle(286, 498, 165, 112)),
            new Hotspot(RoomObjectId.FOURTH_DEVICE, "네 번째 장치", new Rectangle(852, 510, 165, 112)),
            new Hotspot(RoomObjectId.FIFTH_DEVICE, "다섯 번째 장치", new Rectangle(1018, 510, 165, 112)));
    private static final Hotspot ROOM_NOTE =
            new Hotspot(RoomObjectId.HINT_NOTE, "접힌 메모", new Rectangle(740, 558, 95, 51));

    private final long mapId;
    private final WorkshopScene workshop;
    private final List<Hotspot> devices;
    private final BufferedImage background;
    private final BufferedImage[] overlays = new BufferedImage[OVERLAY_PATHS.length];
    private BufferedImage scene;
    private BufferedImage previousScene;
    private Timer deviceTimer;
    private long deviceTransitionStarted;
    private int changedDevice = -1;
    private final BufferedImage note = load("/images/room/hint_note.png");
    private final java.util.Map<Hotspot, Shape> staticOutlines = new java.util.HashMap<>();
    private final BufferedImage[][] deviceImages = new BufferedImage[ENGINE_DEVICES.size()][2];
    private int solvedCount;
    private int puzzleCount;
    private boolean hintAvailable;
    private final Consumer<RoomObjectId> onObjectClick;
    private final boolean fillScreen;
    private final BufferedImage surround;
    private GameStage stage = GameStage.START;
    private boolean investigationOpen;
    private int sceneBottomInset;
    private Hotspot hovered;
    private Hotspot pressed;
    private boolean returning;
    private Hotspot focusedObject;
    private boolean puzzleOpen;
    private Timer walkTimer;
    private final Timer ambientTimer;
    private double ambientSeconds;
    private long walkStart;
    private boolean showHotspots = false;

    /** 클릭된 물건을 알려 줄 콜백을 받는다. 실제 게임 규칙은 이 패널이 아니라 콜백 쪽이 처리한다. */
    public RoomPanel(Consumer<RoomObjectId> onObjectClick) {
        this(onObjectClick, false);
    }

    /** 전체 화면 배치에서는 원본 비율을 유지하며 빈 공간 없이 채울 수 있다. */
    public RoomPanel(Consumer<RoomObjectId> onObjectClick, boolean fillScreen) {
        this(onObjectClick, fillScreen, 1);
    }

    public RoomPanel(Consumer<RoomObjectId> onObjectClick, boolean fillScreen, long mapId) {
        this.onObjectClick = Objects.requireNonNull(onObjectClick);
        this.mapId         = mapId;
        this.devices       = mapId == 2 ? ARCHIVE_DEVICES : ENGINE_DEVICES;
        WorkshopRoom configuration = WorkshopRoom.forMap(mapId);
        this.workshop = configuration == null ? null : new WorkshopScene(configuration);
        this.background    = workshop != null ? workshop.background() : load(mapId == 2 ? "/images/room/archive.png"
                : mapId == 3 ? "/images/room/engine.png" : BACKGROUND_PATH);
        if (mapId == 1) {
            for (int i = 0; i < overlays.length; i++) {
                overlays[i] = load(OVERLAY_PATHS[i]);
            }
        }
        for (int i = mapId == 1 ? 2 : 0; workshop == null && i < devices.size(); i++) {
            String sprite = spriteName(i);
            deviceImages[i][0] = load("/images/room/" + sprite + "_locked.png");
            deviceImages[i][1] = load("/images/room/" + sprite + "_solved.png");
        }
        this.fillScreen = fillScreen;
        surround = fillScreen ? switch ((int) mapId) {
            case 1 -> load("/images/room/study-hud-surround.jpg");
            case 3 -> load("/images/room/workshop/engine/hud-surround.jpg");
            case 4 -> load("/images/room/workshop/greenhouse/hud-surround.jpg");
            default -> null;
        } : null;
        ambientTimer = new Timer(RoomAmbience.FRAME_MS, event -> {
            ambientSeconds += RoomAmbience.FRAME_MS / 1000.0;
            repaint();
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                updateAmbience();
            }
        });
        if (workshop == null) { rebuildScene(); }

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                updateHover(hotspotAt(event.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                updateHover(null);
            }

            @Override
            public void mousePressed(MouseEvent event) {
                pressed = !puzzleOpen && focusedObject == null && event.getButton() == MouseEvent.BUTTON1
                        ? hotspotAt(event.getPoint()) : null;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                // 버튼처럼 동작한다: 누른 곳과 뗀 곳이 같은 물건일 때만 클릭으로 인정한다.
                Hotspot spot = hotspotAt(event.getPoint());
                if (event.getButton() == MouseEvent.BUTTON1 && spot != null && spot.equals(pressed)) {
                    updateHover(null);
                    onObjectClick.accept(spot.id());
                    if (puzzleOpen && background != null) {
                        focusedObject = spot;
                        animateCamera(false);
                    }
                }
                pressed = null;
            }
        };
        this.mouse = mouse;
    }

    private String spriteName(int index) {
        if (index == 5) { return "exit"; }
        if (mapId == 2) {
            return switch (index) {
                case 0 -> "archive_box";
                case 1 -> "archive_cards";
                case 2 -> "archive_film";
                default -> "panel";
            };
        }
        if (mapId == 3) {
            return switch (index) {
                case 1 -> "engine_valve";
                case 3 -> "engine_lever";
                default -> "engine_switch";
            };
        }
        return "panel";
    }

    private MouseAdapter mouse;

    /** 생성 후 화면 속성과 입력 리스너를 연결한다. */
    public void initialize() {
        setPreferredSize(new Dimension(960, 600));
        setBackground(LETTERBOX);
        setOpaque(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void animateCamera(boolean back) {
        double progress = cameraProgress();
        if (walkTimer != null) {
            walkTimer.stop();
        }
        returning = back;
        double elapsed = back ? 1 - progress : 0;
        walkStart = System.nanoTime() - (long) (elapsed * WALK_MS * 1_000_000);
        walkTimer = new Timer(15, event -> {
            if (elapsedMs() >= WALK_MS) {
                walkTimer.stop();
                walkTimer = null;
                if (returning) { focusedObject = null; }
            }
            repaint();
        });
        walkTimer.start();
    }

    private double cameraProgress() {
        if (walkTimer == null) { return 1; }
        double fraction = Math.min(1.0, elapsedMs() / (double) WALK_MS);
        return returning ? 1 - fraction : fraction;
    }

    private long elapsedMs() {
        return (System.nanoTime() - walkStart) / 1_000_000;
    }

    /** 화면이 사라질 때 걷는 효과를 멈춘다. 이때는 클릭을 전달하지 않는다. */
    @Override
    public void removeNotify() {
        ambientTimer.stop();
        if (walkTimer != null) {
            walkTimer.stop();
            walkTimer = null;
        }
        finishDeviceTransition();
        focusedObject = null;
        puzzleOpen = false;
        super.removeNotify();
    }

    private void updateAmbience() {
        if (isShowing() && stage != GameStage.ESCAPED && (mapId == 1 || workshop != null)) {
            ambientTimer.start();
        } else {
            ambientTimer.stop();
        }
    }

    /** 클릭 영역을 눈으로 확인하는 개발용 스위치. true면 모든 영역에 빨간 테두리를 그린다. */
    public void setShowHotspots(boolean show) {
        this.showHotspots = show;
        repaint();
    }

    public boolean hasSurround() { return surround != null; }

    /** 화면 하단 UI는 이 영역 아래에 배치하고 클릭 가능한 장치는 위에 맞춘다. */
    public void setSceneBottomInset(int inset) {
        sceneBottomInset = Math.max(0, inset);
        repaint();
    }

    private int sceneHeight() { return Math.max(1, getHeight() - sceneBottomInset); }

    public void setInvestigationOpen(boolean open) {
        if (investigationOpen != open) { investigationOpen = open; repaint(); }
    }

    public void setPuzzleOpen(boolean open) {
        if (puzzleOpen == open) { return; }
        puzzleOpen = open;
        if (!open && focusedObject != null) {
            animateCamera(true);
        }
        repaint();
    }

    public void renderWorkshop(GameView view) {
        if (workshop != null) {
            if (workshop.update(view)) { startDeviceTransition(-1); }
            repaint();
        }
    }

    public int escapeRevealDelay() {
        return DOOR_OPEN_MS + OPEN_DOOR_HOLD_MS;
    }

    public void setProgress(int solved, int total, boolean noteAvailable) {
        if (solvedCount == solved && puzzleCount == total && hintAvailable == noteAvailable) {
            return;
        }
        if (workshop == null && solved > solvedCount && (mapId != 1 || solved > 2)) {
            startDeviceTransition(solved - 1);
        }
        solvedCount   = solved;
        puzzleCount   = total;
        hintAvailable = noteAvailable;
        if (!hintAvailable && hovered != null && hovered.id() == RoomObjectId.HINT_NOTE) {
            updateHover(null);
        }
        repaint();
    }

    private static final Hotspot ARCHIVE_NOTE =
            new Hotspot(RoomObjectId.HINT_NOTE, "접힌 메모", new Rectangle(747, 510, 78, 49));

    private Hotspot noteSpot() {
        return switch ((int) mapId) {
            case 1 -> STUDY_NOTE;
            case 2 -> ARCHIVE_NOTE;
            default -> ROOM_NOTE;
        };
    }

    private java.util.List<Hotspot> visibleDevices() {
        if (workshop != null) {
            return workshop.spots().stream().map(spot -> new Hotspot(spot.id(), spot.title(), spot.area())).toList();
        }
        if (mapId != 1) {
            List<Hotspot> spots = new java.util.ArrayList<>(devices.subList(0, puzzleCount));
            spots.add(devices.get(devices.size() - 1));
            return spots;
        }
        if (puzzleCount <= 2) { return HOTSPOTS; }
        List<Hotspot> spots = new java.util.ArrayList<>(HOTSPOTS);
        spots.addAll(STUDY_EXTRA.subList(0, puzzleCount - 2));
        return spots;
    }

    public void setStage(GameStage stage) {
        if (this.stage != stage) {
            this.stage = Objects.requireNonNull(stage);
            updateAmbience();
            if (mapId == 1) {
                BufferedImage before = scene;
                rebuildScene();
                if (stage == GameStage.ESCAPED || changedDevice < 0) {
                    startDeviceTransition(-1);
                    previousScene = before;
                }
            } else if (stage == GameStage.ESCAPED && workshop == null) {
                startDeviceTransition(devices.size() - 1);
            }
            repaint();
        }
    }

    private void startDeviceTransition(int index) {
        finishDeviceTransition();
        changedDevice           = index;
        deviceTransitionStarted = System.nanoTime();
        deviceTimer = new Timer(15, event -> {
            if (deviceTransitionProgress() >= 1) {
                finishDeviceTransition();
            }
            repaint();
        });
        deviceTimer.start();
    }

    private void finishDeviceTransition() {
        if (deviceTimer != null) {
            deviceTimer.stop();
            deviceTimer = null;
        }
        previousScene = null;
        changedDevice = -1;
    }

    private float deviceTransitionProgress() {
        double elapsed = (System.nanoTime() - deviceTransitionStarted) / 1_000_000.0;
        int duration = stage == GameStage.ESCAPED ? DOOR_OPEN_MS
                : workshop == null ? DEVICE_TRANSITION_MS : WorkshopScene.TRANSITION_MS;
        return (float) Math.min(1.0, elapsed / duration);
    }

    /** 현재 단계에 맞는 그림을 한 장으로 미리 합쳐 둔다. 단계가 바뀔 때만 하므로 그릴 때 부담이 없다. */
    private void rebuildScene() {
        if (background == null) {
            scene = null;
            return;
        }
        scene = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scene.createGraphics();
        try {
            BufferedImage backdrop = surround == null ? background : surround.getSubimage(
                    SURROUND_SIDE, SURROUND_TOP, background.getWidth(), background.getHeight());
            g.drawImage(backdrop, 0, 0, null);
            for (int i = 0; i < overlays.length; i++) {
                if (overlays[i] != null && stage.compareTo(OVERLAY_FROM[i]) >= 0) {
                    g.drawImage(overlays[i], 0, 0, null);
                }
            }
        } finally {
            g.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(LETTERBOX);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (background == null) {
                drawMissingImageNotice(g);
                return;
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 이후의 모든 그리기를 "그림 원본 좌표"로 할 수 있도록 좌표계를 옮기고 늘린다.
            Layout layout = fitToPanel();
            if (surround == null) { g.clipRect(0, 0, getWidth(), sceneHeight()); }
            applyWalk(g, layout);
            g.translate(layout.offsetX(), layout.offsetY());
            g.scale(layout.scale(), layout.scale());
            if (surround != null) { paintSurround(g, layout); }
            if (workshop != null) {
                workshop.paint(g, ambientSeconds, surround == null);
                drawHighlights(g);
                return;
            }
            if (mapId == 1 && stage == GameStage.ESCAPED && previousScene != null) {
                paintStudyDoor(g);
            } else {
                if (previousScene != null) {
                    g.drawImage(previousScene, 0, 0, null);
                    g.setComposite(AlphaComposite.SrcOver.derive(deviceTransitionProgress()));
                }
                g.drawImage(scene, 0, 0, null);
                g.setComposite(AlphaComposite.SrcOver);
            }
            if (mapId == 1) { RoomAmbience.study(g, ambientSeconds); }
            for (Hotspot spot : visibleDevices()) {
                int index = mapId == 1 ? STUDY_EXTRA.indexOf(spot) + 2 : devices.indexOf(spot);
                if (index >= 0 && (mapId != 1 || index >= 2)) {
                    boolean solved = index < solvedCount || spot.id() == RoomObjectId.DOOR && stage == GameStage.ESCAPED;
                    Rectangle area = spot.area();
                    if (spot.id() == RoomObjectId.DOOR) {
                        area = new Rectangle(area.x + 58, area.y + area.height / 2 + 30, 58, 42);
                    }
                    if (solved && index == changedDevice) {
                        drawSprite(g, deviceImages[index][0], area);
                        g.setComposite(AlphaComposite.SrcOver.derive(deviceTransitionProgress()));
                    }
                    drawSprite(g, deviceImages[index][solved ? 1 : 0], area);
                    g.setComposite(AlphaComposite.SrcOver);
                }
            }
            if (hintAvailable) {
                drawSprite(g, note, noteSpot().area());
            }
            drawHighlights(g);
        } finally {
            g.dispose();
        }
    }

    private void paintStudyDoor(Graphics2D graphics) {
        float time     = deviceTransitionProgress();
        float open     = time * time * (3 - 2 * time);
        Rectangle leaf = STUDY_DOOR_LEAF;
        int hinge      = leaf.x + leaf.width;
        int bottom     = leaf.y + leaf.height;
        int width      = (int) Math.round(leaf.width * Math.cos(open * Math.PI / 2));

        graphics.drawImage(previousScene, 0, 0, null);
        graphics.setComposite(AlphaComposite.SrcOver.derive(open));
        graphics.drawImage(scene, 0, 0, null);
        graphics.setComposite(AlphaComposite.SrcOver);
        Graphics2D doorway = (Graphics2D) graphics.create();
        doorway.clip(leaf);
        doorway.drawImage(scene, 0, 0, null);
        if (width > 0) {
            doorway.drawImage(previousScene, hinge - width, leaf.y, hinge, bottom,
                    leaf.x, leaf.y, hinge, bottom, null);
        }
        doorway.dispose();
    }

    private void paintSurround(Graphics2D graphics, Layout layout) {
        int width  = background.getWidth();
        int height = background.getHeight();
        int left   = (int) Math.floor(-layout.offsetX() / layout.scale()) - 2;
        int top    = (int) Math.floor(-layout.offsetY() / layout.scale()) - 12;
        int right  = (int) Math.ceil((getWidth() - layout.offsetX()) / layout.scale()) + 2;
        int bottom = (int) Math.ceil((getHeight() - layout.offsetY()) / layout.scale()) + 12;
        int[] xs = {left, 0, width, right};
        int[] ys = {top, 0, height, bottom};
        int seamRight  = SURROUND_SIDE + width;
        int seamBottom = SURROUND_TOP + height;
        int[] sourceX  = {Math.max(0, SURROUND_SIDE + left), SURROUND_SIDE, seamRight,
            Math.min(surround.getWidth(), seamRight + right - width)};
        int[] sourceY  = {Math.max(0, SURROUND_TOP + top), SURROUND_TOP, seamBottom,
            Math.min(surround.getHeight(), seamBottom + bottom - height)};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (row == 1 && column == 1 && workshop == null) { continue; }
                graphics.drawImage(surround, xs[column], ys[row], xs[column + 1], ys[row + 1],
                        sourceX[column], sourceY[row], sourceX[column + 1], sourceY[row + 1], null);
            }
        }
    }

    private static void drawSprite(Graphics2D graphics, BufferedImage sprite, Rectangle area) {
        if (sprite != null) {
            graphics.drawImage(sprite, area.x, area.y, area.width, area.height, null);
        }
    }

    /** 걷는 중이면 좌표계를 위아래로 흔들고, 눌린 물건의 중심을 향해 조금 확대한다. */
    private void applyWalk(Graphics2D g, Layout layout) {
        if (focusedObject == null) {
            return;
        }
        double t    = cameraProgress();
        double zoom = 1 + WALK_ZOOM * (t * t * (3 - 2 * t));
        double bob  = Math.abs(Math.sin(t * Math.PI * 3)) * WALK_BOB * (1 - t) * t;
        Rectangle r = focusedObject.area();
        double cx = layout.offsetX() + (r.x + r.width / 2.0) * layout.scale();
        double cy = layout.offsetY() + (r.y + r.height / 2.0) * layout.scale();
        g.translate(0, -bob);
        g.translate(cx, cy);
        g.scale(zoom, zoom);
        g.translate(-cx, -cy);
    }

    /** 기본은 그림 전체를 보이고, 전체 화면 모드에서는 가장자리만 잘라 빈 공간을 없앤다. */
    private Layout fitToPanel() {
        int leftInset  = surround == null ? 0 : Math.min(176, getWidth() * 14 / 100);
        int rightInset = surround == null ? 0 : Math.min(56, getWidth() / 32);
        double horizontal = (double) Math.max(1, getWidth() - leftInset - rightInset) / background.getWidth();
        double vertical   = (double) sceneHeight() / background.getHeight();
        double scale      = fillScreen && sceneBottomInset == 0 && surround == null
                ? Math.max(horizontal, vertical) : Math.min(horizontal, vertical);
        double offsetX    = (getWidth() - background.getWidth() * scale + leftInset - rightInset) / 2.0;
        double offsetY    = surround != null ? 0 : (sceneHeight() - background.getHeight() * scale) / 2.0;
        return new Layout(scale, offsetX, offsetY);
    }

    public Point screenPoint(int imageX, int imageY) {
        Layout layout = fitToPanel();
        return new Point((int) Math.round(layout.offsetX() + imageX * layout.scale()),
                (int) Math.round(layout.offsetY() + imageY * layout.scale()));
    }

    /** 화면의 한 점 아래에 있는 물건을 찾는다. 없으면 null. */
    private Hotspot hotspotAt(Point screenPoint) {
        if (focusedObject != null || background == null || getWidth() <= 0 || getHeight() <= 0 || screenPoint.y >= sceneHeight()) {
            return null;
        }
        Point2D.Double p = fitToPanel().toImage(screenPoint);
        if (workshop != null) {
            for (WorkshopScene.Spot spot : workshop.spots()) {
                if (workshop.outline(spot).intersects(p.x - 4, p.y - 4, 8, 8)) {
                    return new Hotspot(spot.id(), spot.title(), spot.area());
                }
            }
            return null;
        }
        if (hintAvailable && hoverOutline(noteSpot()).contains(p)) {
            return noteSpot();
        }
        for (Hotspot spot : visibleDevices()) {
            if (hoverOutline(spot).contains(p)) {
                return spot;
            }
        }
        return null;
    }

    private void updateHover(Hotspot spot) {
        if (Objects.equals(spot, hovered)) {
            return;
        }
        hovered = spot;
        setCursor(Cursor.getPredefinedCursor(spot == null ? Cursor.CROSSHAIR_CURSOR : Cursor.HAND_CURSOR));
        setToolTipText(spot == null ? null : spot.label());
        repaint();
    }

    private void drawHighlights(Graphics2D g) {
        if (showHotspots) {
            g.setColor(new Color(0xFF3030));
            g.setStroke(new BasicStroke(3f));
            for (Hotspot spot : visibleDevices()) {
                Rectangle r = spot.area();
                g.drawRect(r.x, r.y, r.width, r.height);
            }
        }
        if (hovered != null) {
            Shape outline = hoverOutline(hovered);
            g.setColor(new Color(43, 26, 13, 190));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
        }
    }

    private Shape hoverOutline(Hotspot spot) {
        if (workshop != null) {
            return workshop.silhouette(new WorkshopScene.Spot(spot.id(), spot.label(), spot.area()));
        }
        return staticOutlines.computeIfAbsent(spot, key -> SpriteOutline.exterior(legacyOutline(key)));
    }

    private Shape legacyOutline(Hotspot spot) {
        Rectangle area = spot.area();
        if (spot.id() == RoomObjectId.HINT_NOTE && note != null) {
            return SpriteOutline.fromAlpha(note, area);
        }
        if (mapId == 1) {
            return switch (spot.id()) {
                case DRAWER -> SpriteOutline.polygon(
                        513, 498, 535, 483, 796, 484, 819, 505, 808, 522,
                        805, 719, 824, 749, 775, 753, 778, 736, 552, 736, 548, 754,
                        513, 750, 520, 722, 520, 519);
                case WALL_DIAL -> SpriteOutline.polygon(951, 282, 1128, 282, 1137, 291,
                        1137, 468, 1128, 477, 951, 477, 943, 468, 943, 291);
                case DOOR -> SpriteOutline.polygon(1221, 111, 1547, 111, 1547, 137,
                        1531, 139, 1532, 737, 1228, 737, 1228, 143, 1221, 138);
                case BOOKS -> SpriteOutline.polygon(516, 170, 852, 170, 852, 192,
                        832, 198, 836, 376, 851, 384, 849, 399, 820, 404, 820, 418,
                        548, 418, 545, 404, 516, 397, 516, 379, 531, 374, 531, 193, 516, 189);
                case WINDOW -> SpriteOutline.polygon(131, 62, 376, 62, 398, 79, 398, 483,
                        410, 490, 400, 504, 113, 504, 113, 491, 124, 482, 124, 81);
                default -> spriteOrBevel(spot);
            };
        }
        return spriteOrBevel(spot);
    }

    private Shape spriteOrBevel(Hotspot spot) {
        int index = mapId == 1 ? STUDY_EXTRA.indexOf(spot) + 2 : devices.indexOf(spot);
        if (index >= 0 && (mapId != 1 || index >= 2) && spot.id() != RoomObjectId.DOOR) {
            BufferedImage sprite = deviceImages[index][0];
            if (sprite != null) { return SpriteOutline.fromAlpha(sprite, spot.area()); }
        }
        Rectangle bounds = spot.area();
        return new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
    }

    /** 그림 파일이 없을 때 앱이 죽지 않고, 무엇이 문제인지 알려 주는 대체 화면. */
    private void drawMissingImageNotice(Graphics2D g) {
        g.setColor(new Color(0xE9E7CE));
        String[] lines = {"방 그림을 불러오지 못했습니다.", "확인할 파일: src/main/resources" + BACKGROUND_PATH};
        FontMetrics fm = g.getFontMetrics();
        int y = getHeight() / 2 - fm.getHeight();
        for (String line : lines) {
            g.drawString(line, (getWidth() - fm.stringWidth(line)) / 2, y);
            y += fm.getHeight() + 4;
        }
    }

    /** 클래스패스에서 그림을 읽는다. 실패하면 null을 돌려주고 원인을 로그로 남긴다. */
    private static BufferedImage load(String path) {
        try (InputStream in = RoomPanel.class.getResourceAsStream(path)) {
            if (in == null) {
                LOG.warning("방 그림을 찾을 수 없습니다: " + path);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException exception) {
            LOG.warning("방 그림을 읽지 못했습니다: " + path + " - " + exception.getMessage());
            return null;
        }
    }
}
