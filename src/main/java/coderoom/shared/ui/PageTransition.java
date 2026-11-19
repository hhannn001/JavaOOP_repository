package coderoom.shared.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** 창의 glass pane에 설치하는 공통 암전 전환. 모든 메서드는 EDT에서 호출한다. */
public final class PageTransition extends JComponent {
    private static final long HALF_DURATION = 350_000_000L;

    private final KeyEventDispatcher inputBlocker = event ->
            isVisible() && SwingUtilities.getWindowAncestor(event.getComponent())
                    == SwingUtilities.getWindowAncestor(this);
    private final Timer timer = new Timer(16, event -> advance());
    private BufferedImage previous;
    private long started;
    private float progress;

    public PageTransition() {
        setOpaque(false);
        setVisible(false);
        MouseAdapter blocker = new MouseAdapter() { };
        addMouseListener(blocker);
        addMouseMotionListener(blocker);
        addMouseWheelListener(blocker);
    }

    /** 이전 컴포넌트를 제거하기 전에 호출한다. 이미 보관한 화면은 유지한다. */
    public void capture(JComponent source) {
        if (previous != null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return;
        }
        previous = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = previous.createGraphics();
        source.printAll(graphics);
        graphics.dispose();
    }

    /** 새 화면을 배치한 뒤 호출한다. 최초 표시처럼 이전 화면이 없으면 즉시 표시한다. */
    public void play() {
        if (previous == null) {
            return;
        }
        timer.stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(inputBlocker);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(inputBlocker);
        started  = System.nanoTime();
        progress = 0f;
        setVisible(true);
        timer.start();
        repaint();
    }

    public void stop() {
        timer.stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(inputBlocker);
        previous = null;
        setVisible(false);
    }

    private void advance() {
        progress = Math.min(2f, (System.nanoTime() - started) / (float) HALF_DURATION);
        if (progress >= 2f) {
            stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (previous == null) {
            return;
        }
        if (progress < 1f) {
            graphics.drawImage(previous, 0, 0, getWidth(), getHeight(), null);
        }
        float phase   = progress < 1f ? progress : 2f - progress;
        float opacity = phase * phase * (3f - 2f * phase);
        graphics.setColor(new Color(0, 0, 0, Math.round(255 * opacity)));
        graphics.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override
    public void removeNotify() {
        stop();
        super.removeNotify();
    }
}
