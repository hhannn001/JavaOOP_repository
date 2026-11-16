package coderoom.app;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import javax.imageio.ImageIO;

/** 로비 좌표계에서 움직이는 고양이. Swing EDT에서 입력·시간·그리기를 전달한다. */
final class DashboardCat {
    static final double LEFT_EDGE  = 230;
    static final double RIGHT_EDGE = 1350;
    static final double BELL_X     = 1280;
    static final int FLOOR_Y       = 890;

    static final double AUTO_JUMP_CHANCE = 0.35;

    enum Reaction { NONE, PURR, STAND, STRETCH, GRUMPY, CURIOUS, SLEEPY, SHY, SURPRISED }

    private static final BufferedImage[] FRAMES = loadFrames("cat-atlas.png", 3);
    private static final BufferedImage[] WALK   = loadFrames("cat-walk.png", 3);
    private static final BufferedImage[] REST   = loadFrames("cat-rest.png", 3);

    private static final CatReactionFrames REACTIONS = new CatReactionFrames(FRAMES[0]);
    // Contact → passing → opposite contact → return. Avoid repeated contact poses mid-stride.
    private static final int[] WALK_ORDER = {0, 9, 2, 6, 8, 7, 5, 1, 3, 10, 4, 11};
    private final Random random;
    private double x             = 640;
    private double height;
    private double velocityY;
    private double velocityX;
    private double walkDistance;
    private double manualSeconds;
    private double wanderSeconds;
    private double idleActionSeconds;
    private double jumpCheckSeconds = 5;
    private double jumpCooldown;
    private double destination   = 1060;
    private double reactionTime;
    private double poseSeconds;
    private double animationTime;
    private int direction;
    private int nextReaction;
    private Reaction reaction = Reaction.NONE;
    private boolean called;
    private boolean needsDestination;
    private boolean facingRight = true;
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean preparing;

    DashboardCat() { this(new Random()); }
    DashboardCat(Random random) {
        this.random = random;
        beginRest();
    }

    private void beginRest() {
        wanderSeconds     = 4 + random.nextDouble() * 3;
        idleActionSeconds = 1.5 + random.nextDouble() * 1.5;
    }

    void key(int key, boolean pressed) {
        if (!pressed && (key == KeyEvent.VK_LEFT && !left || key == KeyEvent.VK_RIGHT && !right
                || key == KeyEvent.VK_UP && !up)) return;
        manualSeconds = 4;
        if (pressed) {
            called           = false;
            reaction         = Reaction.NONE;
            needsDestination = true;
        }
        switch (key) {
            case KeyEvent.VK_LEFT -> left = pressed;
            case KeyEvent.VK_RIGHT -> right = pressed;
            case KeyEvent.VK_UP -> {
                if (pressed && !up) jump();
                up = pressed;
            }
            default -> { }
        }
    }

    void releaseKeys() {
        left  = false;
        right = false;
        up    = false;
    }

    void callToBell() {
        releaseKeys();
        called            = true;
        reaction          = Reaction.NONE;
        manualSeconds     = 0;
        wanderSeconds     = 0;
        idleActionSeconds = -1;
        destination       = BELL_X;
    }

    void interact() {
        if (reaction != Reaction.NONE) return; // Rapid clicks must not restart the animation.
        releaseKeys();
        called           = false;
        manualSeconds    = 0;
        needsDestination = true;
        beginRest();
        idleActionSeconds = -1;
        Reaction[] choices = {Reaction.PURR, Reaction.STAND, Reaction.STRETCH, Reaction.GRUMPY,
                Reaction.CURIOUS, Reaction.SLEEPY, Reaction.SHY, Reaction.SURPRISED};
        startReaction(choices[nextReaction++ % choices.length]);
    }

    private void startReaction(Reaction next) {
        reaction     = next;
        reactionTime = 0;
    }

    private void chooseDestination() {
        double middle = (LEFT_EDGE + RIGHT_EDGE) / 2;
        if (x < middle) {
            double first = Math.max(x + 360, middle + 180);
            destination = first + random.nextDouble() * (RIGHT_EDGE - 20 - first);
        } else {
            double last = Math.min(x - 360, middle - 180);
            destination = LEFT_EDGE + 20 + random.nextDouble() * (last - LEFT_EDGE - 20);
        }
        needsDestination = false;
    }

    private void jump() {
        if (height == 0 && poseSeconds <= 0) {
            preparing    = true;
            poseSeconds  = 0.09;
            jumpCooldown = 10;
        }
    }

    void advance(double seconds) {
        // A delayed EDT frame must not teleport the cat or skip through the floor.
        double dt = Math.max(0, Math.min(seconds, 0.05));
        animationTime += dt;
        jumpCooldown = Math.max(0, jumpCooldown - dt);
        if (reaction != Reaction.NONE) {
            wanderSeconds = Math.max(0, wanderSeconds - dt);
            if (height == 0 && !preparing && Math.abs(velocityX) < 1) reactionTime += dt;
            if (reactionTime >= 2.4 && height == 0 && !preparing) {
                reaction = Reaction.NONE;
            }
        }
        if (left || right || up) manualSeconds = 4;
        else manualSeconds = Math.max(0, manualSeconds - dt);
        if (reaction != Reaction.NONE) {
            direction = 0;
        } else if (manualSeconds > 0) {
            direction = (right ? 1 : 0) - (left ? 1 : 0);
        } else {
            if (needsDestination && !called) chooseDestination();
            wanderSeconds = Math.max(0, wanderSeconds - dt);
            direction     = wanderSeconds > 0 ? 0 : (int) Math.signum(destination - x);
            if (!called && wanderSeconds > 0 && height == 0 && !preparing && Math.abs(velocityX) < 1) {
                if (idleActionSeconds >= 0) {
                    idleActionSeconds -= dt;
                    if (idleActionSeconds < 0) {
                        double choice = random.nextDouble();
                        startReaction(choice < 0.3 ? Reaction.STRETCH : choice < 0.7 ? Reaction.SLEEPY : Reaction.PURR);
                        direction = 0;
                    }
                }
            } else if (!called && wanderSeconds == 0 && height == 0 && !preparing && jumpCooldown == 0) {
                jumpCheckSeconds -= dt;
                if (jumpCheckSeconds <= 0) {
                    jumpCheckSeconds = 5 + random.nextDouble() * 3;
                    if (random.nextDouble() < AUTO_JUMP_CHANCE) jump();
                }
            }
        }
        double speed = called ? 280 : manualSeconds > 0 ? 190 : 82;
        if (manualSeconds == 0 && reaction == Reaction.NONE) {
            speed = Math.min(speed, Math.sqrt(2 * 1400 * Math.abs(destination - x)));
        }
        double target = poseSeconds > 0 && height == 0 ? 0 : direction * speed;
        double delta  = Math.max(-1400 * dt, Math.min(1400 * dt, target - velocityX));
        double travel = (velocityX + delta / 2) * dt;
        velocityX += delta;
        if (Math.abs(velocityX) > 1) facingRight = velocityX > 0;
        double before = x;
        x += travel;
        if (manualSeconds == 0 && reaction == Reaction.NONE && wanderSeconds == 0
                && (Math.abs(destination - x) < 1 || (destination - before) * (destination - x) <= 0)) {
            x                = destination;
            velocityX        = 0;
            needsDestination = true;
            beginRest();
            if (called) {
                called = false;
                idleActionSeconds = -1;
                startReaction(Reaction.STAND);
            }
        }
        if (height == 0) walkDistance += Math.abs(travel);
        if (x <= LEFT_EDGE || x >= RIGHT_EDGE) {
            x               = Math.max(LEFT_EDGE, Math.min(RIGHT_EDGE, x));
            direction       = 0;
            velocityX       = 0;
        }
        if (poseSeconds > 0) {
            poseSeconds = Math.max(0, poseSeconds - dt);
            if (preparing && poseSeconds == 0) {
                preparing = false;
                velocityY = 470;
            }
        }
        if (height > 0 || velocityY > 0) {
            height    += velocityY * dt - 600 * dt * dt;
            velocityY -= 1200 * dt;
            if (height <= 0) {
                height      = 0;
                velocityY   = 0;
                poseSeconds = 0.12;
            }
        }
    }

    double x() { return x; }
    double height() { return height; }
    boolean isManual() { return manualSeconds > 0; }
    boolean isCalled() { return called; }
    boolean isResting() {
        return !called && manualSeconds == 0 && reaction == Reaction.NONE && wanderSeconds > 0
                && height == 0 && !preparing && Math.abs(velocityX) < 1;
    }
    Reaction reaction() { return reaction; }
    double destination() { return destination; }
    boolean facingRight() { return facingRight; }
    double velocityX() { return velocityX; }
    int walkFrameIndex() { return WALK_ORDER[(int) (walkDistance / 6) % WALK_ORDER.length]; }
    static BufferedImage walkingFrame(int index) { return WALK[index]; }

    Rectangle2D bounds() {
        BufferedImage sprite = currentSprite();
        double scale         = spriteScale();
        return new Rectangle2D.Double(x - sprite.getWidth() * scale / 2,
                FLOOR_Y - height - sprite.getHeight() * scale, sprite.getWidth() * scale, sprite.getHeight() * scale);
    }

    boolean contains(double sceneX, double sceneY) {
        Rectangle2D bounds = bounds();
        if (!bounds.contains(sceneX, sceneY)) return false;
        BufferedImage sprite = currentSprite();
        int sx               = (int) ((sceneX - bounds.getX()) / bounds.getWidth() * sprite.getWidth());
        int sy               = (int) ((sceneY - bounds.getY()) / bounds.getHeight() * sprite.getHeight());
        if (!facingRight) sx = sprite.getWidth() - 1 - sx;
        return (sprite.getRGB(sx, sy) >>> 24) >= 128;
    }

    private BufferedImage currentSprite() {
        if (showReaction()) return REACTIONS.frame(reaction, reactionTime);
        if (isResting()) return REST[animationTime % 5.2 > 4.9 ? 8 : 11];
        return frameIndex() == 4 ? WALK[walkFrameIndex()] : FRAMES[frameIndex()];
    }

    private boolean showReaction() {
        return reaction != Reaction.NONE && height == 0 && !preparing && Math.abs(velocityX) < 1;
    }

    private double spriteScale() {
        // Calibrate each atlas with its neutral standing pose, never with a curled tail or seated silhouette.
        return showReaction() ? 1 : 112.0 / (isResting() ? REST[0].getWidth() : FRAMES[0].getWidth());
    }

    int frameIndex() {
        if (preparing) return 8;
        if (height > 0) return velocityY > 0 ? 9 : 10;
        if (poseSeconds > 0) return 11;
        if (Math.abs(velocityX) > 1) return 4;
        // A brief blink, rather than blinking on every breathing cycle.
        if (animationTime % 4.8 > 4.6) return 2;
        return (int) (animationTime / 0.45) % 2 == 0 ? 0 : 3;
    }

    void paint(Graphics2D graphics) {
        Graphics2D g        = (Graphics2D) graphics.create();
        BufferedImage frame = currentSprite();
        double scale        = spriteScale();
        int shadowWidth     = (int) (70 - Math.min(height, 100) * 0.25);
        g.setColor(new Color(24, 13, 5, (int) (90 - Math.min(height, 100) * 0.45)));
        g.fillOval((int) x - shadowWidth / 2, FLOOR_Y - 5, shadowWidth, 10);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // Keep fractional scene coordinates; the scaled scene must not quantize motion to whole pixels.
        g.translate(x, FLOOR_Y - height);
        g.scale(facingRight ? scale : -scale, scale);
        if (isResting()) {
            double breath = Math.sin(animationTime * Math.PI * 2 / 3.4) * 0.012;
            g.scale(1 - breath / 2, 1 + breath);
        }
        g.drawImage(frame, -frame.getWidth() / 2, -frame.getHeight(), null);
        g.dispose();
        if (reaction == Reaction.PURR && showReaction()) paintHeart(graphics);
    }

    private void paintHeart(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        double rise  = Math.min(1, reactionTime / 2.4);
        g.translate(x + 22, bounds().getY() - 12 - rise * 20);
        g.setColor(new Color(201, 125, 91, (int) (210 * Math.sin(Math.PI * rise))));
        g.fillRect(-6, -3, 5, 5);
        g.fillRect(1, -3, 5, 5);
        g.fillRect(-6, 0, 12, 5);
        g.fillRect(-4, 5, 8, 3);
        g.fillRect(-2, 8, 4, 2);
        g.dispose();
    }

    static BufferedImage[] loadFrames(String name, int rows) {
        try (InputStream stream = DashboardCat.class.getResourceAsStream("/images/dashboard/cat/" + name)) {
            if (stream == null) throw new IOException("Missing cat atlas");
            BufferedImage atlas = ImageIO.read(stream);
            if (atlas == null || !atlas.getColorModel().hasAlpha()) throw new IOException("Invalid cat atlas");
            BufferedImage[] frames = new BufferedImage[4 * rows];
            for (int index = 0; index < frames.length; index++) {
                int x0     = index % 4 * atlas.getWidth() / 4;
                int y0     = index / 4 * atlas.getHeight() / rows;
                int x1     = (index % 4 + 1) * atlas.getWidth() / 4;
                int y1     = (index / 4 + 1) * atlas.getHeight() / rows;
                int left   = x1;
                int top    = y1;
                int right  = x0;
                int bottom = y0;
                // Generated cells have different transparent margins. Register paws to the floor.
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        if ((atlas.getRGB(x, y) >>> 24) >= 128) {
                            left   = Math.min(left, x);
                            top    = Math.min(top, y);
                            right  = Math.max(right, x);
                            bottom = Math.max(bottom, y);
                        }
                    }
                }
                if (left > right || top > bottom) throw new IOException("Empty cat frame " + index);
                frames[index] = atlas.getSubimage(left, top, right - left + 1, bottom - top + 1);
            }
            return frames;
        } catch (IOException exception) {
            throw new IllegalStateException("고양이 스프라이트를 읽을 수 없습니다.", exception);
        }
    }
}
