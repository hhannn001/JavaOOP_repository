package coderoom.widgets.result;

import coderoom.entities.game.GameSession;
import coderoom.shared.ui.PixelTheme;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Locale;
import javax.swing.JPanel;

/** 남은 시간만큼 줄어들며 초록 → 노랑 → 빨강으로 변하는 제한시간 표시. */
public final class TimeLimitBar extends JPanel {
    private long remaining = GameSession.TIME_LIMIT_SECONDS;

    public TimeLimitBar() {
        setFont(PixelTheme.font(15));
    }

    public void setElapsedSeconds(long elapsed) {
        remaining = Math.max(0, GameSession.TIME_LIMIT_SECONDS - Math.max(0, elapsed));
        getAccessibleContext().setAccessibleName("제한시간 " + remaining + "초");
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        PixelTheme.pixels(g);
        double ratio = remaining / (double) GameSession.TIME_LIMIT_SECONDS;
        int width    = Math.max(0, getWidth() - 8);
        g.setColor(PixelTheme.DARK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(getFont());
        g.setColor(PixelTheme.LIGHT);
        g.drawString("제한시간", 8, 18);
        String time = String.format(Locale.ROOT, "%02d:%02d", remaining / 60, remaining % 60);
        g.drawString(time, getWidth() - g.getFontMetrics().stringWidth(time) - 8, 18);
        g.setColor(new Color(75, 65, 49));
        g.fillRect(3, 26, width + 2, 18);
        g.setColor(new Color(25, 30, 32));
        g.fillRect(4, 27, width, 16);
        g.setColor(Color.getHSBColor((float) (ratio / 3.0), 0.65f, 0.85f));
        g.fillRect(4, 27, (int) Math.round(width * ratio), 16);
        g.dispose();
    }
}
