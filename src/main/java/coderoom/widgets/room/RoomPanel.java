package coderoom.widgets.room;

import coderoom.entities.game.RoomObjectId;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
 * 1단계 구현 범위: 방 배경 그리기 + 창 크기에 맞춘 확대·축소 + 서랍·다이얼·문 등의 클릭 처리.
 * 아직 하지 않는 것(다음 단계): 단계별 그림 교체(열린 서랍·해제된 다이얼·열린 문), GameView 연동.
 *
 * 핵심 아이디어: 클릭 영역을 "화면 좌표"가 아니라 "그림 원본 좌표(1586×992)"로 적어 두고,
 * 그리기와 클릭 판정이 항상 같은 변환(layout)을 쓰게 해서 그림과 클릭 영역이 어긋나지 않게 한다.
 */
public class RoomPanel extends JPanel {
    private static final Logger LOG = Logger.getLogger(RoomPanel.class.getName());

    /** 방 배경 그림의 위치. src/main/resources 아래를 기준으로 한 경로다. */
    private static final String BACKGROUND_PATH = "/images/room/study_locked.png";

    /** 그림 비율이 창과 다를 때 남는 빈 공간의 색. 프로토타입의 짙은 녹색(--deep)과 같다. */
    private static final Color LETTERBOX = new Color(0x15271E);
    private static final Color GOLD      = new Color(0xE5C286);

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

    private final BufferedImage background;
    private final Consumer<RoomObjectId> onObjectClick;
    private final boolean fillScreen;
    private Hotspot hovered;
    private Hotspot pressed;
    private boolean showHotspots = false;

    /** 클릭된 물건을 알려 줄 콜백을 받는다. 실제 게임 규칙은 이 패널이 아니라 콜백 쪽이 처리한다. */
    public RoomPanel(Consumer<RoomObjectId> onObjectClick) {
        this(onObjectClick, false);
    }

    /** 전체 화면 배치에서는 원본 비율을 유지하며 빈 공간 없이 채울 수 있다. */
    public RoomPanel(Consumer<RoomObjectId> onObjectClick, boolean fillScreen) {
        this.onObjectClick = Objects.requireNonNull(onObjectClick);
        this.background    = loadBackground();
        this.fillScreen    = fillScreen;
        setPreferredSize(new Dimension(960, 600));
        setBackground(LETTERBOX);
        setOpaque(true);

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
                pressed = event.getButton() == MouseEvent.BUTTON1 ? hotspotAt(event.getPoint()) : null;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                // 버튼처럼 동작한다: 누른 곳과 뗀 곳이 같은 물건일 때만 클릭으로 인정한다.
                Hotspot spot = hotspotAt(event.getPoint());
                if (event.getButton() == MouseEvent.BUTTON1 && spot != null && spot == pressed) {
                    RoomPanel.this.onObjectClick.accept(spot.id());
                }
                pressed = null;
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** 클릭 영역을 눈으로 확인하는 개발용 스위치. true면 모든 영역에 빨간 테두리를 그린다. */
    public void setShowHotspots(boolean show) {
        this.showHotspots = show;
        repaint();
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
            g.translate(layout.offsetX(), layout.offsetY());
            g.scale(layout.scale(), layout.scale());
            g.drawImage(background, 0, 0, null);
            drawHighlights(g);
        } finally {
            g.dispose();
        }
    }

    /** 기본은 그림 전체를 보이고, 전체 화면 모드에서는 가장자리만 잘라 빈 공간을 없앤다. */
    private Layout fitToPanel() {
        double horizontal = (double) getWidth() / background.getWidth();
        double vertical   = (double) getHeight() / background.getHeight();
        double scale      = fillScreen ? Math.max(horizontal, vertical) : Math.min(horizontal, vertical);
        double offsetX = (getWidth() - background.getWidth() * scale) / 2.0;
        double offsetY = (getHeight() - background.getHeight() * scale) / 2.0;
        return new Layout(scale, offsetX, offsetY);
    }

    /** 화면의 한 점 아래에 있는 물건을 찾는다. 없으면 null. */
    private Hotspot hotspotAt(Point screenPoint) {
        if (background == null || getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        Point2D.Double p = fitToPanel().toImage(screenPoint);
        for (Hotspot spot : HOTSPOTS) {
            if (spot.area().contains(p.x, p.y)) {
                return spot;
            }
        }
        return null;
    }

    private void updateHover(Hotspot spot) {
        if (spot == hovered) {
            return;
        }
        hovered = spot;
        setCursor(Cursor.getPredefinedCursor(spot == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        setToolTipText(spot == null ? null : spot.label());
        repaint();
    }

    private void drawHighlights(Graphics2D g) {
        if (showHotspots) {
            g.setColor(new Color(0xFF3030));
            g.setStroke(new BasicStroke(3f));
            for (Hotspot spot : HOTSPOTS) {
                Rectangle r = spot.area();
                g.drawRect(r.x, r.y, r.width, r.height);
            }
        }
        if (hovered != null) {
            Rectangle r = hovered.area();
            g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 48));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(4f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        }
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

    /** 클래스패스에서 배경 그림을 읽는다. 실패하면 null을 돌려주고 원인을 로그로 남긴다. */
    private static BufferedImage loadBackground() {
        try (InputStream in = RoomPanel.class.getResourceAsStream(BACKGROUND_PATH)) {
            if (in == null) {
                LOG.warning("방 배경 그림을 찾을 수 없습니다: " + BACKGROUND_PATH);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException exception) {
            LOG.warning("방 배경 그림을 읽지 못했습니다: " + exception.getMessage());
            return null;
        }
    }
}
