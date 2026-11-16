package coderoom.widgets.room;

import coderoom.entities.game.GameStage;
import coderoom.entities.game.RoomObjectId;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * 담당: D
 * 역할: 방 그림과 물건 클릭 영역
 * 상위 타입: javax.swing.JPanel
 *
 * 하는 일: ① 방 그림을 창 크기에 맞춰 그린다. ② 서랍·다이얼·문 등을 누르면 콜백으로 알린다.
 * ③ setStage(단계)로 열린 서랍·해제된 다이얼·열린 문 그림을 덧그린다.
 * 클릭 영역은 "그림 원본 좌표(1586×992)"로 적고, 그리기와 클릭 판정이 같은 배율·여백을 쓴다.
 */
public class RoomPanel extends JPanel {
    private static final Logger LOG = Logger.getLogger(RoomPanel.class.getName());
    private static final String DIR = "/images/room/";
    private static final Color GOLD = new Color(0xE5C286);

    /** 클릭 영역(x, y, 가로, 세로). 그림 원본 픽셀 기준이며 서로 겹치지 않는다. 어긋나면 이 숫자만 고친다. */
    private static final Map<RoomObjectId, Rectangle> HOTSPOTS = Map.of(
            RoomObjectId.DRAWER,    new Rectangle(522, 494, 290, 258),
            RoomObjectId.WALL_DIAL, new Rectangle(945, 285, 193, 195),
            RoomObjectId.DOOR,      new Rectangle(1233, 112, 289, 624),
            RoomObjectId.BOOKS,     new Rectangle(520, 170, 332, 248),
            RoomObjectId.WINDOW,    new Rectangle(118, 72, 280, 408));

    private final Consumer<RoomObjectId> onClick;
    private final BufferedImage background = load("study_locked.png");
    private final BufferedImage drawerOpen = load("drawer_open.png");
    private final BufferedImage dialSolved = load("dial_solved.png");
    private final BufferedImage doorOpen   = load("door_open.png");
    private BufferedImage scene;                       // 배경 + 현재 단계의 덧그림을 미리 합쳐 둔 그림
    private GameStage stage = GameStage.START;
    private RoomObjectId hovered;
    private RoomObjectId pressed;

    /** 물건을 누르면 onClick이 호출된다. 정답 판정 같은 게임 규칙은 이 패널이 아니라 받는 쪽이 처리한다. */
    public RoomPanel(Consumer<RoomObjectId> onClick) {
        this.onClick = Objects.requireNonNull(onClick);
        setPreferredSize(new Dimension(960, 600));
        setBackground(new Color(0x15271E));            // 그림 비율이 창과 다를 때 남는 여백의 색
        rebuildScene();

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                hover(objectAt(event.getX(), event.getY()));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hover(null);
            }

            @Override
            public void mousePressed(MouseEvent event) {
                pressed = event.getButton() == MouseEvent.BUTTON1 ? objectAt(event.getX(), event.getY()) : null;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                // 누른 곳과 뗀 곳이 같은 물건일 때만 클릭으로 인정한다.
                RoomObjectId released = objectAt(event.getX(), event.getY());
                if (event.getButton() == MouseEvent.BUTTON1 && released != null && released == pressed) {
                    RoomPanel.this.onClick.accept(released);
                }
                pressed = null;
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** 게임 단계에 맞는 그림으로 바꾼다. GamePage가 새 GameView를 받을 때 room.setStage(view.stage())처럼 부른다. */
    public void setStage(GameStage stage) {
        Objects.requireNonNull(stage);
        if (stage != this.stage) {
            this.stage = stage;
            rebuildScene();
            repaint();
        }
    }

    /** 배경 위에 단계별 덧그림을 한 번만 합쳐 둔다. 매번 그릴 때 다시 합치지 않아 빠르다. */
    private void rebuildScene() {
        if (background == null) {
            return;
        }
        scene = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scene.createGraphics();
        try {
            g.drawImage(background, 0, 0, null);
            // 단계는 앞 단계를 이어받으므로 "이 단계 이상"이면 겹친다. GameStage 선언 순서(START→ESCAPED)에 의존한다.
            addOverlay(g, GameStage.DRAWER_OPEN, drawerOpen);
            addOverlay(g, GameStage.SAFE_OPEN, dialSolved);
            addOverlay(g, GameStage.ESCAPED, doorOpen);
        } finally {
            g.dispose();
        }
    }

    private void addOverlay(Graphics2D g, GameStage from, BufferedImage overlay) {
        if (overlay != null && stage.compareTo(from) >= 0) {
            g.drawImage(overlay, 0, 0, background.getWidth(), background.getHeight(), null);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);                // 여백 색으로 먼저 칠한다
        if (scene == null) {
            graphics.setColor(Color.WHITE);
            graphics.drawString("방 그림을 불러오지 못했습니다: src/main/resources" + DIR + "study_locked.png", 20, 30);
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(marginX(), marginY());         // 이후 그리기는 모두 "그림 원본 좌표"
            g.scale(scale(), scale());
            g.drawImage(scene, 0, 0, null);
            if (hovered != null) {
                Rectangle area = HOTSPOTS.get(hovered);
                g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 48));
                g.fill(area);
                g.setColor(GOLD);
                g.setStroke(new BasicStroke(4f));
                g.draw(area);
            }
        } finally {
            g.dispose();
        }
    }

    /** 그림 전체가 잘리지 않고 창에 들어가는 가장 큰 배율. */
    private double scale() {
        return Math.min((double) getWidth() / background.getWidth(), (double) getHeight() / background.getHeight());
    }

    /** 남는 공간을 양쪽에 똑같이 나누는 여백. 그리기와 클릭 판정이 함께 쓴다. */
    private double marginX() {
        return (getWidth() - background.getWidth() * scale()) / 2;
    }

    private double marginY() {
        return (getHeight() - background.getHeight() * scale()) / 2;
    }

    /** 화면의 한 점 아래에 있는 물건. 없으면 null. */
    private RoomObjectId objectAt(int screenX, int screenY) {
        if (background == null || getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        double x = (screenX - marginX()) / scale();
        double y = (screenY - marginY()) / scale();
        for (Map.Entry<RoomObjectId, Rectangle> spot : HOTSPOTS.entrySet()) {
            if (spot.getValue().contains(x, y)) {
                return spot.getKey();
            }
        }
        return null;
    }

    private void hover(RoomObjectId object) {
        if (object != hovered) {
            hovered = object;
            setCursor(Cursor.getPredefinedCursor(object == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
            repaint();
        }
    }

    /** 그림 한 장을 읽는다. 없거나 읽지 못하면 null을 돌려주고 로그만 남겨 앱은 계속 실행된다. */
    private static BufferedImage load(String name) {
        try (InputStream in = RoomPanel.class.getResourceAsStream(DIR + name)) {
            if (in == null) {
                LOG.warning("방 그림을 찾을 수 없습니다: " + DIR + name);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException exception) {
            LOG.warning("방 그림을 읽지 못했습니다: " + DIR + name + " (" + exception.getMessage() + ")");
            return null;
        }
    }
}
