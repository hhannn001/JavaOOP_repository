package coderoom.shared.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.BorderFactory;

/** 물체 확대 UI에서 공유하는 글꼴, 잉크, 픽셀 렌더링. */
public final class PixelTheme {
    public static final Color INK   = new Color(57, 38, 24);
    public static final Color PAPER = new Color(224, 200, 147);
    public static final Color LIGHT = new Color(247, 225, 174);
    public static final Color DARK  = new Color(29, 22, 17);
    private static final Font FONT = loadFont();
    private static final java.util.Map<String, BufferedImage> ASSETS = new java.util.HashMap<>();

    private PixelTheme() { }

    public static Font font(float size) { return FONT.deriveFont(size); }

    public static void pixels(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** 물체별로 새로 제작한 투명 에셋. 원본 방 이미지를 잘라 사용하지 않는다. */
    public static void asset(Graphics2D graphics, String name, int x, int y, int width, int height) {
        pixels(graphics);
        BufferedImage asset = ASSETS.computeIfAbsent(name, PixelTheme::loadAsset);
        graphics.drawImage(asset, x, y, width, height, null);
    }

    public static JButton button(String text) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(getModel().isPressed() ? new Color(67, 48, 29)
                        : getModel().isRollover() ? new Color(49, 36, 23) : DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setFont(getFont());
                g.setColor(isEnabled() ? LIGHT : new Color(141, 120, 85));
                String label = getText().replace("→", ">");
                g.drawString(label, (getWidth() - g.getFontMetrics().stringWidth(label)) / 2,
                        (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
                if (hasFocus()) { g.drawLine(12, getHeight() - 6, getWidth() - 12, getHeight() - 6); }
                g.dispose();
            }
        };
        button.setFont(font(16));
        button.setForeground(LIGHT);
        button.setBackground(DARK);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(128, 94, 48), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        button.setFocusPainted(true);
        return button;
    }

    private static Font loadFont() {
        try (InputStream input = PixelTheme.class.getResourceAsStream("/fonts/Pretendard-Regular.otf")) {
            return Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (Exception exception) {
            return new Font(Font.MONOSPACED, Font.PLAIN, 16);
        }
    }

    private static BufferedImage loadAsset(String name) {
        try (InputStream input = PixelTheme.class.getResourceAsStream("/images/game-ui/" + name + ".png")) {
            return ImageIO.read(input);
        } catch (Exception exception) {
            throw new IllegalStateException("물체 에셋을 읽을 수 없습니다: " + name, exception);
        }
    }
}
