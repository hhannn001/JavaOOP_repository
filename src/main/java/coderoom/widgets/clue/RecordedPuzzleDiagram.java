package coderoom.widgets.clue;

import coderoom.entities.game.ItemId;
import coderoom.features.gameplay.ItemView;
import coderoom.features.gameplay.PuzzleVariant;
import coderoom.shared.ui.PixelTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Reconstructs the diagram from the saved snapshot, without using the current game's variant. */
public final class RecordedPuzzleDiagram {
    private static final Pattern BALANCE_LOAD = Pattern.compile("\\b(?:left|왼쪽)\\s*=\\s*(\\d{1,9})\\s*\\*\\s*1");

    private RecordedPuzzleDiagram() { }

    public static Optional<JComponent> create(long puzzleId, String code, String answer) {
        if (puzzleId == 403) {
            return illustration("/images/room/workshop/greenhouse/completed-bridge.png",
                    "오른쪽 가로 발판, 아래 세로 발판과 중앙 연결판을 끼운 모습");
        }
        if (puzzleId == 405 && answer.matches("[AB]{4}")) {
            return illustration("/images/game-ui/workshop/greenhouse-5.jpg",
                    "점검  " + railRoute(answer, 0) + "   /   운반  " + railRoute(answer, 2));
        }
        return switch (ItemView.Diagram.forPuzzle(puzzleId)) {
            case NONE -> Optional.empty();
            case BALANCE -> {
                var load = BALANCE_LOAD.matcher(code);
                if (!load.find()) { yield Optional.empty(); }
                var diagram = new BalanceDiagram();
                diagram.setLoad(Integer.parseInt(load.group(1)));
                yield Optional.of(paper(diagram));
            }
            case DISTRIBUTION -> variant(puzzleId, code)
                    .map(variant -> paper(noteDiagram(puzzleId, code, variant)))
                    .or(() -> RecordedDistributionTable.create(code));
            case SEEDS -> variant(puzzleId, code)
                    .map(variant -> paper(noteDiagram(puzzleId, code, variant)));
            default -> Optional.of(paper(noteDiagram(puzzleId, code, PuzzleVariant.STANDARD)));
        };
    }

    private static String railRoute(String answer, int offset) {
        return (answer.charAt(offset) == 'B' ? "높은 선로" : "낮은 선로")
                + " → " + (answer.charAt(offset + 1) == 'A' ? "우회선로" : "본선");
    }

    private static PuzzleDiagram noteDiagram(long puzzleId, String code, PuzzleVariant variant) {
        var diagram = new PuzzleDiagram();
        diagram.setClue(new ItemView(ItemId.NOTE1, "문제", code, "", ItemView.Diagram.forPuzzle(puzzleId), variant));
        return diagram;
    }

    private static Optional<PuzzleVariant> variant(long puzzleId, String code) {
        String compact = code.replaceAll("\\s+", "");
        for (PuzzleVariant variant : PuzzleVariant.values()) {
            String signature = puzzleId == 401
                    ? variant.specimenRule().javaConditions().replaceAll("\\s+", "")
                    : "{" + String.join(",", variant.distributionQueue().split("")) + "};intleft="
                            + variant.distributionCapacity() + ";";
            if (compact.contains(signature)) { return Optional.of(variant); }
        }
        return Optional.empty();
    }

    private static JComponent paper(JComponent diagram) {
        diagram.setOpaque(true);
        diagram.setBackground(PixelTheme.PAPER);
        diagram.setPreferredSize(new Dimension(380, 245));
        return diagram;
    }

    private static Optional<JComponent> illustration(String resource, String caption) {
        try (var stream = RecordedPuzzleDiagram.class.getResourceAsStream(resource)) {
            if (stream == null) { return Optional.empty(); }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) { return Optional.empty(); }
            JPanel picture = new JPanel() {
                @Override protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    double scale = Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
                    int width    = (int) (image.getWidth() * scale);
                    int height   = (int) (image.getHeight() * scale);
                    graphics.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
                }
            };
            picture.setBackground(PixelTheme.DARK);
            picture.setPreferredSize(new Dimension(380, 222));
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);
            panel.add(picture, BorderLayout.CENTER);
            JLabel label = new JLabel(caption, SwingConstants.CENTER);
            label.setFont(PixelTheme.font(15));
            label.setForeground(PixelTheme.LIGHT);
            label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            panel.add(label, BorderLayout.SOUTH);
            return Optional.of(panel);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }
}
