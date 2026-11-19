package coderoom.widgets.dialogue;

import coderoom.shared.ui.PixelTheme;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.imageio.ImageIO;

/** 생성된 시스템 창과 이름 이미지 위에 실제 대사를 표시한다. EDT에서 사용한다. */
public final class DialoguePanel extends JPanel {
    private static final BufferedImage FRAME     = loadTrimmed("panel");
    private static final BufferedImage PLAYER    = loadTrimmed("player");
    private static final BufferedImage SYSTEM    = loadTrimmed("system");
    private static final BufferedImage NAMEPLATE = loadTrimmed("nameplate");

    public enum Speaker { PLAYER, SYSTEM }
    public record Line(Speaker speaker, String text) { }

    private final ArrayDeque<Line> remaining = new ArrayDeque<>();
    private final JTextArea text            = new JTextArea();
    private Runnable onChanged             = () -> { };
    private Line current;

    public DialoguePanel() {
        setName("gameDialogue");
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(64, 28, 22, 28));
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(PixelTheme.font(18));
        text.setForeground(PixelTheme.LIGHT);
        add(text, BorderLayout.CENTER);
        setFocusable(true);
        for (String key : List.of("ENTER", "SPACE", "ESCAPE")) {
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), "advance");
        }
        getActionMap().put("advance", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { advance(); }
        });
        setVisible(false);
    }

    /** 새 조사에는 최신 대사를 보여 주며, 타이머 갱신으로 대사를 재시작하지 않는다. */
    public void showLines(Line... lines) {
        remaining.clear();
        remaining.addAll(List.of(lines));
        advance();
    }

    public void advance() {
        current = remaining.poll();
        setVisible(current != null);
        if (current != null) {
            text.setText(current.text().replaceAll("([.!?…]+[’”\"']?)\\s+", "$1\n"));
            text.getAccessibleContext().setAccessibleName(
                    (current.speaker() == Speaker.PLAYER ? "플레이어: " : "시스템: ") + current.text());
            requestFocusInWindow();
        }
        onChanged.run();
        revalidate();
        repaint();
    }

    public void setOnChanged(Runnable callback) { onChanged = callback; }

    public void close() {
        remaining.clear();
        advance();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        PixelTheme.pixels(g);
        paintFrame(g);
        if (current != null) {
            BufferedImage name = current.speaker() == Speaker.PLAYER ? PLAYER : SYSTEM;
            int nameHeight     = 26;
            int nameWidth      = name.getWidth() * nameHeight / name.getHeight();
            g.drawImage(NAMEPLATE, 20, 0, 164, 48, null);
            g.drawImage(name, 20 + (164 - nameWidth) / 2, 11, nameWidth, nameHeight, null);
        }
        g.dispose();
    }

    /** 제목 줄·모서리 두께는 고정하고, 가운데만 늘려 창 크기와 내부 좌표를 일치시킨다. */
    private void paintFrame(Graphics2D g) {
        int[] sourceX = {0, 36, FRAME.getWidth() - 36, FRAME.getWidth()};
        int[] sourceTop    = {0, FRAME.getHeight() / 5, FRAME.getHeight() - 28};
        int[] sourceBottom = {36, FRAME.getHeight() - 28, FRAME.getHeight()};
        int[] targetX = {0, 12, getWidth() - 12, getWidth()};
        int[] targetY = {24, 36, getHeight() - 12, getHeight()};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                g.drawImage(FRAME, targetX[column], targetY[row], targetX[column + 1], targetY[row + 1],
                        sourceX[column], sourceTop[row], sourceX[column + 1], sourceBottom[row], null);
            }
        }
    }

    /** 생성 이미지의 투명 바깥 여백을 렌더링에서 제외한다. 원본 에셋은 보존한다. */
    private static BufferedImage loadTrimmed(String name) {
        try (InputStream input = DialoguePanel.class.getResourceAsStream("/images/game-ui/dialogue/" + name + ".png")) {
            if (input == null) { throw new IOException("대사창 이미지가 없습니다: " + name); }
            BufferedImage image = ImageIO.read(input);
            int left            = image.getWidth();
            int top             = image.getHeight();
            int right           = -1;
            int bottom          = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) >= 128) {
                        left   = Math.min(left, x);
                        top    = Math.min(top, y);
                        right  = Math.max(right, x);
                        bottom = Math.max(bottom, y);
                    }
                }
            }
            if (right < left) { throw new IOException("대사창 이미지가 비어 있습니다: " + name); }
            return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
        } catch (IOException exception) {
            throw new IllegalStateException("대사창 이미지를 읽을 수 없습니다.", exception);
        }
    }
}
