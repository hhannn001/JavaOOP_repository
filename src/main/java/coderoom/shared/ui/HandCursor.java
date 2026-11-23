package coderoom.shared.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Uses the open hand by default and holds the closed hand until release. */
public final class HandCursor {
    private static final Cursor OPEN   = create(false);
    private static final Cursor CLOSED = create(true);

    private HandCursor() { }

    public static Cursor openHand() { return OPEN; }

    public static void install(JComponent component) {
        install(component, point -> true);
    }

    public static void install(JComponent component, Predicate<Point> target) {
        component.setCursor(OPEN);
        component.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                Point point = event.getPoint();
                if (SwingUtilities.isLeftMouseButton(event) && component.isEnabled()
                        && component.contains(point) && target.test(point)) {
                    component.setCursor(CLOSED);
                }
            }

            @Override public void mouseReleased(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) { component.setCursor(OPEN); }
            }
        });
        component.addPropertyChangeListener("enabled", event -> {
            if (!component.isEnabled()) { component.setCursor(OPEN); }
        });
        component.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !component.isShowing()) {
                component.setCursor(OPEN);
            }
        });
    }

    private static Cursor create(boolean closed) {
        Cursor fallback = Cursor.getPredefinedCursor(closed ? Cursor.MOVE_CURSOR : Cursor.HAND_CURSOR);
        String name     = closed ? "closed-hand" : "open-hand";
        String resource = "/images/game-ui/cursors/" + name + ".png";
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension size  = toolkit.getBestCursorSize(32, 32);
            if (size.width == 0 || size.height == 0) { return fallback; }
            try (var stream = HandCursor.class.getResourceAsStream(resource)) {
                if (stream == null) { return fallback; }
                BufferedImage sprite = ImageIO.read(stream);
                if (sprite == null) { return fallback; }

                BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                int side            = Math.min(size.width, size.height);
                Graphics2D g        = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(sprite, 0, 0, side, side, null);
                g.dispose();
                return toolkit.createCustomCursor(image, new Point(side / 2, side / 2), name);
            }
        } catch (HeadlessException | IOException exception) {
            return fallback;
        }
    }
}
