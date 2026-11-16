package coderoom.app;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;

/** 고정 사물은 첫 이미지 그대로, 움직이는 부분만 프레임 에셋으로 합성한다. */
final class SceneObjectButton extends JButton {
    static final int FRAME_DELAY_MS = 60;
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private final BufferedImage[] frames;
    private final BufferedImage[] outlines;
    private final BufferedImage plaque;
    private final BufferedImage backdrop;
    private final String kind;
    private final Rectangle motion;
    private Timer animation;
    private int frame;
    private boolean keyboardFocus;
    private boolean activationInProgress;
    private boolean hoverSuppressed;

    SceneObjectButton(String title, String kind, int frameCount, Runnable action) {
        super(title);
        this.kind = kind;
        BufferedImage sheet = read("leaderboard".equals(kind) ? "leaderboard-main.png" : kind + ".png");
        boolean atlas       = !"leaderboard".equals(kind);
        int cellWidth       = sheet.getWidth() / (atlas ? 3 : 1);
        int cellHeight      = sheet.getHeight() / (atlas ? 2 : 1);
        Rectangle crop      = "curtain".equals(kind) ? new Rectangle(116, 94, 294, 362)
                : frameBounds(sheet, 1, cellWidth, cellHeight);
        Rectangle moving = switch (kind) {
            case "bookshelf" -> new Rectangle(192, 190, 83, 125);
            case "calendar" -> new Rectangle(84, 130, 354, 329);
            case "curtain" -> new Rectangle(crop.x + 8, crop.y + 8, crop.width - 16, crop.height - 8);
            default -> new Rectangle(crop);
        };
        moving = moving.intersection(crop);
        motion = new Rectangle(moving.x - crop.x, moving.y - crop.y, moving.width, moving.height);
        frames   = new BufferedImage[frameCount];
        outlines = new BufferedImage[frameCount];
        plaque   = "bookshelf".equals(kind) ? read("plaque-bookshelf.png") : null;
        backdrop = "curtain".equals(kind) ? galleryBackdrop() : null;
        for (int index = 0; index < frameCount; index++) {
            BufferedImage stable = new BufferedImage(crop.width, crop.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g         = stable.createGraphics();
            g.drawImage(sheet, 0, 0, crop.width, crop.height,
                    crop.x, crop.y, crop.x + crop.width, crop.y + crop.height, null);
            if (index > 0) {
                int x = index % 3 * cellWidth + crop.x;
                int y = index / 3 * cellHeight + crop.y;
                g.clip(motion);
                g.setComposite(AlphaComposite.Src);
                g.drawImage(sheet, 0, 0, crop.width, crop.height, x, y, x + crop.width, y + crop.height, null);
            }
            g.dispose();
            if ("curtain".equals(kind)) {
                // 열린 틈에 남은 반투명 잔상이 고정 배경을 프레임마다 어둡게 만드는 것을 제거한다.
                for (int y = 0; y < stable.getHeight(); y++) {
                    for (int x = 0; x < stable.getWidth(); x++) {
                        if ((stable.getRGB(x, y) >>> 24) <= 64) { stable.setRGB(x, y, 0); }
                    }
                }
            }
            frames[index]   = stable;
            outlines[index] = silhouette(stable);
        }
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(title);
        getAccessibleContext().setAccessibleName(title);
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(FocusEvent event) {
                keyboardFocus = event.getCause() == FocusEvent.Cause.TRAVERSAL_FORWARD
                        || event.getCause() == FocusEvent.Cause.TRAVERSAL_BACKWARD;
                if (keyboardFocus) {
                    hoverSuppressed = false;
                }
                repaint();
            }
            @Override public void focusLost(FocusEvent event) { clearHighlight(); }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent event) {
                pointerHover(contains(event.getX(), event.getY()));
            }
        });
        addActionListener(event -> animate(action));
    }

    /** 고정 진열대의 왼쪽 위 액자 하나에만 포스터를 합성한다. */
    private static BufferedImage galleryBackdrop() {
        BufferedImage gallery = read("/images/mapselect/gallery.png");
        BufferedImage poster  = read("/images/posters/room01_study_cipher.png");
        BufferedImage result  = new BufferedImage(gallery.getWidth(), gallery.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g          = result.createGraphics();
        double scale          = Math.max(151.0 / poster.getWidth(), 204.0 / poster.getHeight());
        int width             = (int) Math.ceil(poster.getWidth() * scale);
        int height            = (int) Math.ceil(poster.getHeight() * scale);
        g.drawImage(gallery, 0, 0, null);
        // 입구에서 보이는 중앙 두 열 중 왼쪽 위 액자.
        Graphics2D frame = (Graphics2D) g.create(567, 309, 151, 204);
        frame.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        frame.drawImage(poster, (151 - width) / 2, (204 - height) / 2, width, height, null);
        frame.dispose();
        g.dispose();
        return result;
    }

    static synchronized BufferedImage read(String filename) {
        return CACHE.computeIfAbsent(filename, SceneObjectButton::load);
    }

    private static BufferedImage load(String filename) {
        String path = filename.startsWith("/") ? filename : "/images/dashboard/lobby/" + filename;
        try (InputStream stream = SceneObjectButton.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("로비 이미지가 없습니다: " + filename);
            }
            BufferedImage image = ImageIO.read(stream);
            if (filename.startsWith("label-") || filename.startsWith("plaque-")
                    || "window.png".equals(filename) || filename.startsWith("leaderboard")) {
                Rectangle bounds = frameBounds(image, 1, image.getWidth(), image.getHeight());
                return image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            return image;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("로비 이미지를 읽을 수 없습니다.", exception);
        }
    }

    private static Rectangle frameBounds(BufferedImage image, int count, int width, int height) {
        int left   = width;
        int top    = height;
        int right  = 0;
        int bottom = 0;
        for (int index = 0; index < count; index++) {
            int offsetX = count == 1 ? 0 : index % 3 * width;
            int offsetY = count == 1 ? 0 : index / 3 * height;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if ((image.getRGB(offsetX + x, offsetY + y) >>> 24) > 64) {
                        left   = Math.min(left, x);
                        top    = Math.min(top, y);
                        right  = Math.max(right, x);
                        bottom = Math.max(bottom, y);
                    }
                }
            }
        }
        if (left > right || top > bottom) {
            throw new IllegalArgumentException("빈 애니메이션 이미지입니다.");
        }
        return new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    private static BufferedImage silhouette(BufferedImage source) {
        BufferedImage mask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = source.getRGB(x, y) >>> 24;
                mask.setRGB(x, y, (alpha << 24) | 0xe6bf79);
            }
        }
        return mask;
    }

    private Rectangle imageBounds() {
        if ("curtain".equals(kind)) {
            // 천만 기존 배경의 입구 안쪽에 맞춘다. 문틀·봉을 다시 그리지 않는다.
            return new Rectangle(4, 4, Math.max(1, getWidth() - 8), Math.max(1, getHeight() - 8));
        }
        double scale = Math.min((getWidth() - 8.0) / frames[0].getWidth(), (getHeight() - 8.0) / frames[0].getHeight());
        int width    = Math.max(1, (int) (frames[0].getWidth() * scale));
        int height   = Math.max(1, (int) (frames[0].getHeight() * scale));
        return new Rectangle((getWidth() - width) / 2, getHeight() - height - 4, width, height);
    }

    @Override public boolean contains(int x, int y) {
        Rectangle bounds = imageBounds();
        if (!bounds.contains(x, y)) {
            return false;
        }
        int sx = (x - bounds.x) * frames[0].getWidth() / bounds.width;
        int sy = (y - bounds.y) * frames[0].getHeight() / bounds.height;
        return (frames[0].getRGB(sx, sy) >>> 24) > 64;
    }

    void clearHighlight() {
        keyboardFocus   = false;
        hoverSuppressed = true;
        getModel().setRollover(false);
        repaint();
    }

    boolean isHighlighted() {
        return !"curtain".equals(kind) && !activationInProgress && !sceneBusy() && !hoverSuppressed
                && (getModel().isRollover() || keyboardFocus);
    }

    void pointerHover(boolean active) {
        if (!activationInProgress && !sceneBusy()) {
            hoverSuppressed = false;
            getModel().setRollover(active);
            repaint();
        }
    }

    private boolean sceneBusy() {
        return getParent() instanceof JComponent parent && Boolean.TRUE.equals(parent.getClientProperty("sceneBusy"));
    }

    private void animate(Runnable action) {
        if (activationInProgress || sceneBusy()) {
            return;
        }
        activationInProgress = true;
        clearHighlight();
        busy(true);
        if (frames.length == 1) {
            invokeAction(action);
            return;
        }
        frame = 0;
        animation = new Timer(FRAME_DELAY_MS, event -> {
            if (frame < frames.length - 1) {
                frame++;
                repaint();
            } else {
                animation.stop();
                animation = null;
                invokeAction(action);
            }
        });
        animation.start();
    }

    private void invokeAction(Runnable action) {
        clearHighlight();
        try {
            // 모달 창이 닫힐 때까지 busy를 유지한다. 중첩 이벤트 루프에서도 재진입을 막는다.
            action.run();
        } finally {
            frame                = 0;
            activationInProgress = false;
            busy(false);
            clearHighlight();
        }
    }

    private void busy(boolean busy) {
        if (getParent() instanceof JComponent parent) {
            parent.putClientProperty("sceneBusy", busy);
            parent.repaint();
        }
    }

    @Override public void removeNotify() {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
        frame                = 0;
        activationInProgress = false;
        busy(false);
        clearHighlight();
        super.removeNotify();
    }

    BufferedImage artworkFrame(int index) { return frames[index]; }
    Rectangle movingRegion() { return new Rectangle(motion); }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Rectangle bounds = imageBounds();
        g.translate(bounds.x, bounds.y);
        if (isHighlighted()) {
            for (int dy = -3; dy <= 3; dy += 3) {
                for (int dx = -3; dx <= 3; dx += 3) {
                    g.drawImage(outlines[frame], dx, dy, bounds.width, bounds.height, null);
                }
            }
        }
        if (backdrop != null) {
            // 커튼의 투명한 틈으로 실제 방 선택 공간을 보여 준다. 문틀 밖으로는 그리지 않는다.
            double scale = Math.max(bounds.width / (double) backdrop.getWidth(),
                    bounds.height / (double) backdrop.getHeight());
            int width    = (int) Math.ceil(backdrop.getWidth() * scale);
            int height   = (int) Math.ceil(backdrop.getHeight() * scale);
            g.clipRect(0, 0, bounds.width, bounds.height);
            g.drawImage(backdrop, (bounds.width - width) / 2, (bounds.height - height) / 2, width, height, null);
        }
        g.drawImage(frames[frame], 0, 0, bounds.width, bounds.height, null);
        if (plaque != null) {
            int width  = (int) (bounds.width * 0.34);
            int height = width * plaque.getHeight() / plaque.getWidth();
            g.drawImage(plaque, (bounds.width - width) / 2, (int) (bounds.height * 0.63), width, height, null);
        }
        g.dispose();
    }

    /** DB 사용자명·실시간 수치용. 고정 메뉴 문구는 PNG에 포함되어 있다. */
    static final class PixelFont {
        private static final Font FONT = load();
        static Font at(float size) { return FONT.deriveFont(size); }
        private static Font load() {
            try (InputStream stream = SceneObjectButton.class.getResourceAsStream("/fonts/Pretendard-Regular.otf")) {
                return Font.createFont(Font.TRUETYPE_FONT, stream);
            } catch (Exception exception) {
                throw new IllegalStateException("기록 표시 글꼴을 읽을 수 없습니다.", exception);
            }
        }
    }
}
