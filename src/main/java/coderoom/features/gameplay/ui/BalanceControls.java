package coderoom.features.gameplay.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Hang both loose weights and use the beam's response to set the cart latch. */
final class BalanceControls {
    private static final int PIVOT = 480;
    private static final int BEAM = 310;
    private static final int SPACING = 87;
    private final int load;
    private final PropBoard board;
    private final BufferedImage weight = PropBoard.sprite("weight");
    private final BufferedImage beam   = PropBoard.sprite("scale-beam");
    private final BufferedImage crate  = PropBoard.read("/images/room/workshop/greenhouse/layers/seed-chest-movable.png");
    private final int[] slots = {0, 0};
    private final PropBoard.Control[] weights = new PropBoard.Control[2];
    private final Timer movement;
    private final Timer release;
    private final PropBoard.Control clasp;
    private boolean locking;
    private double tilt;
    private int selected = -1;
    private Point looseWeight;

    BalanceControls(PropBoard board, int load, Runnable complete) {
        this.board = board;
        this.load  = load;
        movement = new Timer(16, event -> settle());
        release  = new Timer(290, event -> complete.run());
        release.setRepeats(false);
        for (int i = 0; i < weights.length; i++) {
            int index = i;
            var control = new PropBoard.Control("balanceWeight" + i, "무게 " + (i + 2) + " 추");
            control.hotspot();
            control.addActionListener(event -> {
                selected = index;
                board.repaint();
            });
            weights[i] = control;
            board.place(control, 580 + i * 148, 481, 110, 116);
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mouseDragged(MouseEvent event) {
                    if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) { return; }
                    selected = index;
                    looseWeight = design(control, event);
                    board.repaint();
                }

                @Override public void mouseReleased(MouseEvent event) {
                    if (looseWeight == null) { return; }
                    Point point = design(control, event);
                    looseWeight = null;
                    int slot = (int) Math.round((point.x - PIVOT) / (double) SPACING);
                    if (slot >= 1 && slot <= 4 && Math.abs(point.y - BEAM) < 104) { hang(slot); }
                    else if (point.y > 468) { hang(0); }
                    board.repaint();
                }
            };
            control.addMouseMotionListener(mouse);
            control.addMouseListener(mouse);
        }
        for (int i = 1; i <= 4; i++) {
            int slot = i;
            var hook = new PropBoard.Control("balanceSlot" + slot, "중심에서 " + slot + "칸");
            hook.hotspot();
            hook.addActionListener(event -> {
                if (selected < 0) {
                    for (int index = 0; index < slots.length; index++) {
                        if (slots[index] == slot) { selected = index; break; }
                    }
                } else { hang(slot); }
                board.repaint();
            });
            board.place(hook, PIVOT + slot * SPACING - 35, 221, 70, 157);
        }
        clasp = new PropBoard.Control("balanceClasp", "고정 걸쇠 잠그기");
        clasp.sprite(PropBoard.read("/images/game-ui/study/latch.png"));
        clasp.addActionListener(event -> {
            if (!answer().isEmpty()) {
                locking = true;
                movement.start();
            } else {
                tilt += .035;
                movement.start();
            }
            board.repaint();
        });
        board.place(clasp, PIVOT - 31, 378, 62, 65);
        board.effects(this::paint);
        reset();
    }

    private Point design(PropBoard.Control source, MouseEvent event) {
        return board.designPoint(SwingUtilities.convertPoint(source, event.getPoint(), board));
    }

    private void hang(int slot) {
        release.stop();
        locking = false;
        clasp.lit(false);
        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == slot) { slots[index] = 0; }
        }
        slots[selected] = slot;
        selected = -1;
        updateTargets();
        movement.start();
    }

    private void updateTargets() {
        for (int i = 0; i < weights.length; i++) {
            int slot = slots[i];
            int x    = slot == 0 ? 580 + i * 148 : PIVOT + slot * SPACING - 55;
            int y    = slot == 0 ? 481 : BEAM + (int) (Math.sin(tilt) * slot * SPACING);
            board.move(weights[i], x, y);
        }
    }

    private int rightTorque() { return 2 * slots[0] + 3 * slots[1]; }

    private double target() {
        return Math.max(-.15, Math.min(.15, (rightTorque() - load) * .012));
    }

    private void settle() {
        double target = target();
        tilt += (target - tilt) * .22;
        if (Math.abs(target - tilt) < .001) {
            tilt = target;
            movement.stop();
            if (locking && !release.isRunning()) {
                clasp.lit(true);
                release.start();
            }
        }
        if (looseWeight == null) { updateTargets(); }
        board.repaint();
    }

    private void paint(Graphics2D g) {
        var pose = g.getTransform();
        g.rotate(tilt, PIVOT, BEAM);
        g.drawImage(beam, 125, BEAM - 45, 710, 90, null);
        PropBoard.sprite(g, crate, PIVOT - SPACING - 73, BEAM - 112, 146, 116);
        PropBoard.label(g, Integer.toString(load), PIVOT - SPACING, BEAM - 42, 31);
        for (int slot = 1; slot <= 4; slot++) {
            int x = PIVOT + slot * SPACING;
            g.setColor(new Color(227, 178, 103));
            g.fillOval(x - 9, BEAM - 9, 18, 18);
            PropBoard.label(g, Integer.toString(slot), x, BEAM + 35, 26);
        }
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] > 0 && (looseWeight == null || selected != i)) {
                paintWeight(g, i, PIVOT + slots[i] * SPACING, BEAM + 84 + 14 * i);
            }
        }
        g.setTransform(pose);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == 0 && (looseWeight == null || selected != i)) {
                paintWeight(g, i, 635 + i * 148, 600);
            }
        }
        if (looseWeight != null) { paintWeight(g, selected, looseWeight.x, looseWeight.y + 42); }
        if (slots[0] > 0 || slots[1] > 0) {
            String formula = "2 × " + slots[0] + " + 3 × " + slots[1] + " = " + rightTorque();
            PropBoard.label(g, formula, 485, 628, 25);
        }
    }

    private void paintWeight(Graphics2D g, int index, int x, int bottom) {
        int height = 82 + 14 * index;
        PropBoard.sprite(g, weight, x - 40, bottom - height, 80, height);
        PropBoard.label(g, Integer.toString(index + 2), x, bottom - 10, 27);
    }

    String answer() {
        return slots[0] > 0 && slots[1] > 0 && rightTorque() == load
                ? new String(new char[] {(char) ('A' + slots[0] - 1), (char) ('A' + slots[1] - 1)}) : "";
    }

    void stop() {
        movement.stop();
        release.stop();
        locking = false;
        clasp.lit(false);
    }

    void reset() {
        stop();
        Arrays.fill(slots, 0);
        tilt        = target();
        selected    = -1;
        looseWeight = null;
        updateTargets();
        board.repaint();
    }
}
