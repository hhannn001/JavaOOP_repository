package coderoom.shared.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.Map;

/** 방의 크기를 바꾸지 않는 조사 오버레이. 모든 슬롯은 화면 가장자리에서 떨어져 있다. */
public final class GameOverlayLayout implements LayoutManager {
    private final Map<String, Component> slots = new HashMap<>();

    @Override public void addLayoutComponent(String name, Component component) { slots.put(name, component); }
    @Override public void removeLayoutComponent(Component component) { slots.values().remove(component); }
    @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(1280, 800); }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(900, 650); }

    @Override public void layoutContainer(Container parent) {
        int width  = parent.getWidth();
        int height = parent.getHeight();
        int margin = Math.max(16, Math.min(32, width / 40));
        int bottom = Math.min(108, height / 6);
        int top    = 88;
        int usable = Math.max(240, height - top - bottom - 24);
        place("header", margin, 8, width - 2 * margin, 72);
        int noteWidth = Math.min(370, width / 3);
        place("clue", margin, top, noteWidth, Math.min(560, usable));
        int left       = margin + noteWidth + 24;
        int available  = Math.max(240, width - left - margin);
        Component input = slots.get("input");
        if (input != null) {
            int inputWidth  = Math.min(620, available);
            int inputHeight = Math.min(570, usable);
            place("input", left + (available - inputWidth) / 2,
                    top + (usable - inputHeight) / 2, inputWidth, inputHeight);
        }
        int inventoryWidth  = Math.min(410, width - 2 * margin);
        int inventoryHeight = 78;
        int inventoryTop    = height - inventoryHeight - 16;
        place("inventory", (width - inventoryWidth) / 2,
                inventoryTop, inventoryWidth, inventoryHeight);
        place("result", margin, top, width - 2 * margin, usable);
        int dialogueWidth  = Math.min(960, width - 2 * margin);
        int dialogueHeight = 190;
        int dialogueTop    = inventoryTop + inventoryHeight - dialogueHeight;
        place("dialogue", (width - dialogueWidth) / 2,
                dialogueTop, dialogueWidth, dialogueHeight);
        place("dimmer", 0, 0, width, height);
        place("dialogueClick", 0, 0, width, height);
        Component dimmer = slots.get("dimmer");
        if (dimmer != null && dimmer.isVisible() && input != null && input.isVisible()) {
            int puzzleHeight = Math.min(570, dialogueTop - 36);
            int puzzleWidth  = Math.min(620, width - 2 * margin);
            place("input", (width - puzzleWidth) / 2, 24, puzzleWidth, puzzleHeight);
        }
    }

    private void place(String name, int x, int y, int width, int height) {
        Component component = slots.get(name);
        if (component != null) {
            component.setBounds(x, y, width, height);
        }
    }
}
