package coderoom.features.gameplay.ui;

import coderoom.entities.game.WorkshopRoom;
import coderoom.features.gameplay.PuzzleVariant;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.util.Optional;

/** Renders the gameplay equipment into a flat, read-only review image. */
public final class PuzzleDevicePreview {
    private PuzzleDevicePreview() { }

    public static Optional<BufferedImage> render(long puzzleId, String code, String answer) {
        PropBoard board;
        if (puzzleId == 1 || puzzleId == 2) {
            var lock = new StudyLockPanel(puzzleId == 1, () -> { });
            lock.showRecordedAnswer(answer);
            board = lock;
        } else if ((puzzleId >= 311 && puzzleId <= 315) || (puzzleId >= 401 && puzzleId <= 405)) {
            var room    = WorkshopRoom.forMap(puzzleId / 100);
            int index   = (int) (puzzleId % 10) - 1;
            var variant = variant(puzzleId, code, answer);
            var device  = new WorkshopPanel(room, index, variant, puzzleId == 405 ? 1 : 0);
            board = device.boardForReview(answer);
        } else {
            return Optional.empty();
        }
        board.setSize(PropBoard.WIDTH, PropBoard.HEIGHT);
        layout(board);
        var image = new BufferedImage(PropBoard.WIDTH, PropBoard.HEIGHT, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            board.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        return Optional.of(image);
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) { layout(nested); }
        }
    }

    private static PuzzleVariant variant(long id, String code, String answer) {
        for (PuzzleVariant variant : PuzzleVariant.values()) {
            boolean matches = switch ((int) id) {
                case 311 -> code.contains("m.voltage >= " + variant.powerThreshold() + ")");
                case 312 -> {
                    int[][] readings = variant.sleeveReadings();
                    int largest = 0;
                    for (int i = 1; i < readings[0].length; i++) {
                        if (readings[1][i] - readings[0][i] > readings[1][largest] - readings[0][largest]) {
                            largest = i;
                        }
                    }
                    yield answer.length() == 2 && answer.charAt(0) == 'A' + largest;
                }
                case 313 -> code.contains("int left = " + variant.distributionCapacity() + ";");
                case 314 -> answer.equals(variant.pressureOrder());
                case 401 -> code.contains(variant.specimenRule().javaConditions());
                case 402 -> answer.equals((variant.highGutterInlet() ? "A" : "B") + "B"
                        + (variant.lowGutterOutlet() ? "C" : "B"));
                case 404 -> code.contains("int left = " + variant.balanceLoad() + " * 1");
                case 405 -> answer.equals((variant.railFlooded() ? "B" : "A")
                        + (variant.railBlocked() ? "A" : "B") + "AB");
                default -> false;
            };
            if (matches) { return variant; }
        }
        return PuzzleVariant.STANDARD;
    }
}
