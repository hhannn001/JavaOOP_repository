package coderoom.app;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/** 표정별 원화 8장을 발 위치에 맞추고 작은 크기의 중간 프레임을 한 번만 준비한다. */
final class CatReactionFrames {
    private static final int STEPS      = 6;
    private static final double[] TIMES = {0, 0.20, 0.40, 0.60, 1.40, 1.75, 2.05, 2.40};

    private final Map<DashboardCat.Reaction, BufferedImage[]> animations = new EnumMap<>(DashboardCat.Reaction.class);

    CatReactionFrames(BufferedImage idle) {
        BufferedImage neutral = resize(idle, 112.0 / idle.getWidth());
        load("stand-grumpy.png", DashboardCat.Reaction.STAND, DashboardCat.Reaction.GRUMPY, neutral);
        load("love-curious.png", DashboardCat.Reaction.PURR, DashboardCat.Reaction.CURIOUS, neutral);
        load("sleepy-shy.png", DashboardCat.Reaction.SLEEPY, DashboardCat.Reaction.SHY, neutral);
        load("surprise-stretch.png", DashboardCat.Reaction.SURPRISED, DashboardCat.Reaction.STRETCH, neutral);
    }

    BufferedImage frame(DashboardCat.Reaction reaction, double seconds) {
        BufferedImage[] frames = animations.get(reaction);
        int segment            = 0;
        while (segment < TIMES.length - 2 && seconds >= TIMES[segment + 1]) segment++;
        double progress = Math.max(0, Math.min(1, (seconds - TIMES[segment]) / (TIMES[segment + 1] - TIMES[segment])));
        double eased    = progress * progress * (3 - 2 * progress);
        int step        = (int) Math.round(eased * STEPS);
        return frames[segment * STEPS + step];
    }

    private void load(String file, DashboardCat.Reaction first, DashboardCat.Reaction second, BufferedImage neutral) {
        BufferedImage[] atlas = DashboardCat.loadFrames(file, 4);
        animations.put(first, sequence(atlas, 0, neutral, first));
        animations.put(second, sequence(atlas, 8, neutral, second));
    }

    private static BufferedImage[] sequence(BufferedImage[] atlas, int offset, BufferedImage neutral, DashboardCat.Reaction reaction) {
        BufferedImage[] poses = new BufferedImage[8];
        double scale          = 112.0 / atlas[offset].getWidth();
        int[] order           = switch (reaction) {
            case GRUMPY -> new int[]{0, 1, 2, 5, 4, 2, 6, 7};
            case STRETCH -> new int[]{0, 1, 2, 3, 4, 2, 1, 7};
            default -> new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        };
        poses[0] = neutral;
        poses[7] = neutral;
        for (int pose = 1; pose < 7; pose++) poses[pose] = resize(atlas[offset + order[pose]], scale);
        BufferedImage[] result = new BufferedImage[7 * STEPS + 1];
        for (int pose = 0; pose < 7; pose++) {
            for (int step = 0; step < STEPS; step++) {
                result[pose * STEPS + step] = tween(poses[pose], poses[pose + 1], step / (double) STEPS);
            }
        }
        result[result.length - 1] = neutral;
        return result;
    }

    private static BufferedImage resize(BufferedImage image, double scale) {
        int width            = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height           = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g         = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    static BufferedImage tween(BufferedImage first, BufferedImage second, double amount) {
        if (amount == 0) return first;
        // A taller pose means raised body parts, not a bigger cat. Pad at the feet instead of stretching pixels.
        BufferedImage pose   = amount < 0.5 ? first : second;
        int width            = Math.max(first.getWidth(), second.getWidth());
        int height           = Math.max(first.getHeight(), second.getHeight());
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g         = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(pose, width / 2 - pose.getWidth() / 2, height - pose.getHeight(), null);
        g.dispose();
        return result;
    }
}
