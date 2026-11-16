package coderoom.app;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

/** 카운터에서 분리한 호출 종. 로비 타이머를 공유해 화면을 떠나면 함께 정지한다. */
final class DashboardBell extends JButton {
    private static final BufferedImage IMAGE = loadBell();
    private double ringing;

    DashboardBell(Runnable callCat) {
        super("고양이 부르기");
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("종 울리기 · 고양이 부르기");
        getAccessibleContext().setAccessibleName("종 울리기, 고양이 부르기");
        addActionListener(event -> {
            if (ringing > 0.4) return;
            ringing = 0.8;
            callCat.run();
            repaint();
        });
    }

    void advance(double seconds) {
        if (ringing > 0) {
            ringing = Math.max(0, ringing - Math.min(seconds, 0.05));
            repaint();
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min((getWidth() - 14.0) / IMAGE.getWidth(), (getHeight() - 12.0) / IMAGE.getHeight());
        int width    = Math.max(1, (int) (IMAGE.getWidth() * scale));
        int height   = Math.max(1, (int) (IMAGE.getHeight() * scale));
        if (getModel().isRollover() || hasFocus()) {
            g.setColor(new Color(239, 185, 88, 45));
            g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
        }
        g.translate(getWidth() / 2.0, getHeight() - 6);
        g.rotate(Math.sin((0.8 - ringing) * 48) * ringing * 0.15);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(IMAGE, -width / 2, -height, width, height, null);
        g.dispose();
    }

    private static BufferedImage loadBell() {
        BufferedImage source = SceneObjectButton.read("/images/dashboard/cat/bell.png");
        int left             = source.getWidth();
        int top              = source.getHeight();
        int right            = 0;
        int bottom           = 0;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getRGB(x, y) >>> 24) >= 128) {
                    left   = Math.min(left, x);
                    top    = Math.min(top, y);
                    right  = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        return source.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }
}
