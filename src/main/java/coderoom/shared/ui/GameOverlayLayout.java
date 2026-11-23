package coderoom.shared.ui;

import coderoom.features.gameplay.ui.AnswerInputPanel;
import coderoom.widgets.clue.CluePanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.Map;

/** 방의 크기를 바꾸지 않는 조사 오버레이. 모든 슬롯은 화면 가장자리에서 떨어져 있다. */
public final class GameOverlayLayout implements LayoutManager {
    private static final int INVENTORY_HEIGHT = 78;
    private static final int INVENTORY_MARGIN = 16;
    private static final int DIALOGUE_HEIGHT  = 190;
    private static final int HUD_GAUGE_GAP    = 18;

    private final Map<String, Component> slots = new HashMap<>();
    private final boolean compactHud;

    public GameOverlayLayout() { this(false); }

    public GameOverlayLayout(boolean compactHud) { this.compactHud = compactHud; }

    @Override public void addLayoutComponent(String name, Component component) { slots.put(name, component); }
    @Override public void removeLayoutComponent(Component component) { slots.values().remove(component); }
    @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(1280, 800); }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(900, 650); }

    public static int roomBottomInset() { return INVENTORY_HEIGHT + INVENTORY_MARGIN + 8; }

    @Override public void layoutContainer(Container parent) {
        int width  = parent.getWidth();
        int height = parent.getHeight();
        int margin = Math.max(16, Math.min(32, width / 40));
        int bottom = Math.min(108, height / 6);
        int top    = 88;
        int usable = Math.max(240, height - top - bottom - 24);
        Component dialogue = slots.get("dialogue");
        if (dialogue != null && dialogue.isVisible()) {
            usable = Math.max(180, Math.min(usable, height - DIALOGUE_HEIGHT - 12 - top));
        }
        int timerWidth = 164;
        int gaugeSize  = compactHud ? 82 : 116;
        int gaugeLeft  = margin + (timerWidth - gaugeSize) / 2;
        place("header", margin + timerWidth, 8, Math.max(0, width - 2 * margin - timerWidth), 72);
        place("timer", margin, 10, timerWidth, 68);
        place("progress", gaugeLeft, 84, gaugeSize, gaugeSize);
        place("attempts", gaugeLeft, 84 + gaugeSize + HUD_GAUGE_GAP, gaugeSize, gaugeSize);

        Component input = slots.get("input");
        int noteOffset = slots.containsKey("attempts") ? gaugeLeft + gaugeSize + 12 - margin : 0;
        int shared     = input != null && input.isVisible() ? noteOffset / 3 : 0;
        int noteWidth  = Math.max(1, Math.min(500, width * 46 / 100) - shared);
        int noteArea   = noteOffset + noteWidth;
        int left       = margin + noteArea + 24;
        int available  = Math.max(1, width - left - margin);
        int inputWidth = Math.min(620, available);
        place("input", left + (available - inputWidth) / 2, top, inputWidth, Math.min(570, usable));

        if (slots.get("clue") instanceof CluePanel clue && clue.isVisible()) {
            boolean speaking = dialogue != null && dialogue.isVisible();
            boolean solving  = input instanceof AnswerInputPanel && input.isVisible();
            int noteTop      = solving ? input.getY() + ((AnswerInputPanel) input).puzzleContentTop() : 78;
            int noteBottom   = speaking ? DIALOGUE_HEIGHT + 12 : solving ? INVENTORY_MARGIN : roomBottomInset();
            int noteLimit    = Math.max(130, height - noteBottom - noteTop);
            int noteHeight   = Math.min(noteLimit, Math.max(130, clue.readingHeight(noteWidth, noteLimit)));
            if (solving) { noteTop -= clue.paperTopInset(noteHeight); }
            place("clue", margin + noteOffset, noteTop, noteWidth, noteHeight);
        }
        int inventoryWidth  = Math.min(410, width - 2 * margin);
        int inventoryHeight = INVENTORY_HEIGHT;
        int inventoryTop    = height - inventoryHeight - INVENTORY_MARGIN;
        place("inventory", (width - inventoryWidth) / 2,
                inventoryTop, inventoryWidth, inventoryHeight);
        place("result", margin, top, width - 2 * margin, usable);
        int dialogueWidth  = Math.min(960, width - 2 * margin);
        int dialogueHeight = DIALOGUE_HEIGHT;
        int dialogueTop    = height - dialogueHeight - 8;
        place("dialogue", (width - dialogueWidth) / 2,
                dialogueTop, dialogueWidth, dialogueHeight);
        place("dimmer", 0, 0, width, height);
        place("dialogueClick", 0, 0, width, height);

    }

    private void place(String name, int x, int y, int width, int height) {
        Component component = slots.get(name);
        if (component != null) {
            component.setBounds(x, y, width, height);
        }
    }
}
