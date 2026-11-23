package coderoom.features.gameplay.ui;

import coderoom.shared.audio.GameAudio;
import coderoom.entities.game.WorkshopRoom;
import coderoom.shared.ui.PixelTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/** A generated close-up with operable controls anchored to its surface. */
class PropBoard extends JPanel {
    static final int WIDTH  = 1000;
    static final int HEIGHT = 667;
    private static final Color INK   = new Color(29, 21, 16);
    private static final Color RIM   = new Color(205, 153, 78);
    private final BufferedImage art;
    private final Map<JComponent, Rectangle> placements = new LinkedHashMap<>();
    private Consumer<Graphics2D> effects = graphics -> { };

    PropBoard(WorkshopRoom room, int index) {
        this("/images/game-ui/workshop/" + (room == WorkshopRoom.ENGINE ? "engine" : "greenhouse")
                + "-" + (index + 1) + ".jpg");
    }

    PropBoard(String path) {
        super(null);
        setOpaque(false);
        art = read(path);
    }

    void place(JComponent component, int x, int y, int width, int height) {
        placements.put(component, new Rectangle(x, y, width, height));
        add(component);
    }

    void move(JComponent component, int x, int y) {
        placements.get(component).setLocation(x, y);
        doLayout();
        repaint();
    }

    void effects(Consumer<Graphics2D> painter) { effects = painter; }

    @Override public void doLayout() {
        Rectangle image = imageBounds();
        for (var entry : placements.entrySet()) {
            Rectangle slot = entry.getValue();
            entry.getKey().setBounds(image.x + slot.x * image.width / WIDTH,
                    image.y + slot.y * image.height / HEIGHT,
                    slot.width * image.width / WIDTH, slot.height * image.height / HEIGHT);
        }
    }

    Point designPoint(Point point) {
        Rectangle bounds = imageBounds();
        if (bounds.isEmpty()) { return new Point(); }
        return new Point((point.x - bounds.x) * WIDTH / bounds.width,
                (point.y - bounds.y) * HEIGHT / bounds.height);
    }

    Rectangle imageBounds() {
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        int width    = (int) (WIDTH * scale);
        int height   = (int) (HEIGHT * scale);
        return new Rectangle((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Rectangle image = imageBounds();
        if (image.isEmpty()) { return; }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(art, image.x, image.y, image.width, image.height, null);
            g.translate(image.x, image.y);
            g.scale(image.width / (double) WIDTH, image.height / (double) HEIGHT);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            effects.accept(g);
        } finally {
            g.dispose();
        }
    }

    static BufferedImage read(String path) {
        try (InputStream input = PropBoard.class.getResourceAsStream(path)) {
            if (input == null) { throw new IOException(path); }
            return ImageIO.read(input);
        } catch (IOException exception) {
            throw new IllegalStateException("장치 이미지 로드 실패: " + path, exception);
        }
    }

    static BufferedImage sprite(String name) {
        return read("/images/game-ui/workshop/parts/" + name + ".png");
    }

    static void sprite(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.min(width / (double) image.getWidth(), height / (double) image.getHeight());
        int artworkWidth  = (int) (image.getWidth() * scale);
        int artworkHeight = (int) (image.getHeight() * scale);
        g.drawImage(image, x + (width - artworkWidth) / 2, y + (height - artworkHeight) / 2,
                artworkWidth, artworkHeight, null);
    }

    static void label(Graphics2D g, String text, int x, int baseline, int size) {
        g.setFont(PixelTheme.font(size).deriveFont(Font.BOLD));
        g.setColor(PixelTheme.LIGHT);
        int left = x - g.getFontMetrics().stringWidth(text) / 2;
        g.drawString(text, left, baseline);
    }

    static void engraving(Graphics2D g, String text, int x, int baseline, int size) {
        g.setFont(PixelTheme.font(size).deriveFont(Font.BOLD));
        int left = x - g.getFontMetrics().stringWidth(text) / 2;
        g.setColor(new Color(49, 27, 14));
        g.drawString(text, left + 1, baseline + 2);
        g.setColor(new Color(255, 225, 172));
        g.drawString(text, left, baseline);
    }

    static void readout(Graphics2D g, String text, int x, int baseline, int size) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, size));
        int left = x - g.getFontMetrics().stringWidth(text) / 2;
        g.setColor(new Color(151, 223, 151, 52));
        g.drawString(text, left - 1, baseline);
        g.drawString(text, left + 1, baseline);
        g.drawString(text, left, baseline + 1);
        g.setColor(new Color(185, 237, 172));
        g.drawString(text, left, baseline);
    }

    static void lamp(Graphics2D g, int x, int y, boolean on) {
        g.setColor(on ? new Color(158, 222, 121) : new Color(104, 48, 34));
        g.fillOval(x - 9, y - 9, 18, 18);
        g.setColor(new Color(48, 29, 19));
        g.drawOval(x - 9, y - 9, 18, 18);
    }

    static final class Control extends JButton {
        private boolean lit;
        private boolean hotspot;
        private boolean showsTooltip = true;
        private BufferedImage sprite;
        private javax.swing.Timer resistance;
        private int wobble;

        Control(String name, String label) {
            super(label);
            setName(name);
            describe(label);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
        }

        void lit(boolean value) {
            lit = value;
            repaint();
        }

        void describe(String label) {
            getAccessibleContext().setAccessibleName(label);
            if (showsTooltip) { setToolTipText(label); }
        }

        void hideTooltip() {
            showsTooltip = false;
            setToolTipText(null);
        }

        void resist() {
            if (resistance != null) { resistance.stop(); }
            wobble      = 5;
            resistance = new javax.swing.Timer(36, event -> {
                int magnitude = Math.abs(wobble) - 1;
                wobble = wobble > 0 ? -magnitude : magnitude;
                if (wobble == 0) { ((javax.swing.Timer) event.getSource()).stop(); }
                repaint();
            });
            resistance.start();
            repaint();
        }

        void sprite(BufferedImage image) {
            sprite = image;
            hotspot();
            repaint();
        }

        void hotspot() { hotspot = true; }

        @Override protected void fireActionPerformed(ActionEvent event) {
            GameAudio.play("pushTrolley".equals(getName()) ? GameAudio.Cue.CART : GameAudio.Cue.METAL);
            super.fireActionPerformed(event);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active  = getModel().isRollover() || hasFocus();
                boolean pressed = getModel().isPressed();
                if (hotspot) {
                    if (sprite != null) {
                        int offset = pressed ? 3 : 0;
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        PropBoard.sprite(g, sprite, 3 + wobble, 3 + offset, getWidth() - 6, getHeight() - 6);
                    }
                    if (isEnabled() && (active || lit)) {
                        int rim = Math.min(getWidth(), getHeight()) - 5;
                        g.setColor(active ? PixelTheme.LIGHT : RIM);
                        g.setStroke(new BasicStroke(active ? 2 : 1));
                        g.drawOval((getWidth() - rim) / 2, (getHeight() - rim) / 2, rim, rim);
                    }
                    return;
                }
                int top = pressed ? 3 : 1;
                int height = getHeight() - top - 2;
                if (height <= 0) { return; }
                g.setColor(lit ? new Color(63, 88, 43, 235) : new Color(INK.getRed(), INK.getGreen(), INK.getBlue(), 222));
                g.fillRoundRect(1, top, getWidth() - 3, height, 8, 8);
                g.setColor(active ? PixelTheme.LIGHT : RIM);
                g.setStroke(new BasicStroke(active ? 2 : 1));
                g.drawRoundRect(1, top, getWidth() - 3, height, 8, 8);
                String label = getText();
                var font = PixelTheme.font(Math.max(9, Math.min(18, getHeight() * .36f)));
                g.setFont(font);
                int textWidth = g.getFontMetrics().stringWidth(label);
                if (textWidth > getWidth() - 10) {
                    g.setFont(font.deriveFont(Math.max(7f, font.getSize2D() * (getWidth() - 10) / textWidth)));
                }
                g.setColor(isEnabled() ? PixelTheme.LIGHT : new Color(190, 169, 133));
                var metrics = g.getFontMetrics();
                g.drawString(label, (getWidth() - metrics.stringWidth(label)) / 2,
                        top + (height - metrics.getHeight()) / 2 + metrics.getAscent());
            } finally {
                g.dispose();
            }
        }
    }

    static final class Selector {
        private final String[] options;
        private final Control control;
        private final Consumer<Integer> changed;
        private BufferedImage[] sprites;
        private int value;

        Selector(String name, Consumer<Integer> changed, String... options) {
            this.options = options;
            this.changed = changed;
            control      = new Control(name, options[0]);
            control.addActionListener(event -> set((value + 1) % options.length));
            control.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous");
            control.getActionMap().put("previous", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                    set((value + options.length - 1) % options.length);
                }
            });
        }

        Control control() { return control; }

        int value() { return value; }

        String label() { return options[value]; }

        void sprites(BufferedImage... images) {
            sprites = images;
            control.sprite(sprites[value]);
        }

        void set(int index) {
            value = index;
            control.setText(options[index]);
            control.describe(options[index]);
            if (sprites != null) { control.sprite(sprites[index]); }
            changed.accept(index);
        }

        char letter() { return (char) ('A' + value); }
    }
}
