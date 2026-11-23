package coderoom.widgets.result;

import coderoom.entities.game.GameSession;
import coderoom.shared.ui.PixelTheme;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/** 모래시계 옆에 남은 시간을 표시한다. */
public final class TimeLimitBar extends JPanel {
    private static final Image HOURGLASS = new ImageIcon(
            TimeLimitBar.class.getResource("/images/game-ui/hud/hourglass.png")).getImage();

    private long remaining = GameSession.TIME_LIMIT_SECONDS;

    public TimeLimitBar() {
        setOpaque(false);
        setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        getAccessibleContext().setAccessibleName("제한시간 " + remaining + "초");
    }

    public void setElapsedSeconds(long elapsed) {
        long seconds = Math.max(0, GameSession.TIME_LIMIT_SECONDS - Math.max(0, elapsed));
        if (remaining == seconds) {
            return;
        }
        remaining = seconds;
        getAccessibleContext().setAccessibleName("제한시간 " + remaining + "초");
        repaint();
    }

    @Override public boolean contains(int x, int y) {
        return false;
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        PixelTheme.pixels(g);
        g.setFont(getFont());
        String time  = String.format(Locale.ROOT, "%02d:%02d", remaining / 60, remaining % 60);
        int iconSize = Math.min(56, getHeight() - 4);
        int textX    = iconSize * 7 / 11 + 14;
        int baseline = (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
        g.drawImage(HOURGLASS, 4, (getHeight() - iconSize) / 2, iconSize * 7 / 11, iconSize, null);
        g.setColor(new Color(22, 15, 12));
        g.drawString(time, textX + 1, baseline + 2);
        g.drawString(time, textX - 1, baseline + 2);
        g.setColor(remaining <= 10 ? new Color(255, 124, 102) : new Color(255, 233, 191));
        g.drawString(time, textX, baseline);
        g.dispose();
    }
}
