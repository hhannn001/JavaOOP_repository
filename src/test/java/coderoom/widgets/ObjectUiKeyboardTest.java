package coderoom.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.ItemId;
import coderoom.widgets.inventory.InventoryPanel;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ObjectUiKeyboardTest {
    @Test void realNumberKeysSelectSlotsAndRemainAvailableToTextFields() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        AtomicReference<ItemId> selected = new AtomicReference<>();
        JFrame[] frame                  = new JFrame[1];
        JButton[] focus                 = new JButton[1];
        JTextField[] input              = new JTextField[1];
        SwingUtilities.invokeAndWait(() -> {
            frame[0] = new JFrame("Inventory keyboard test");
            focus[0] = new JButton("Focus");
            input[0] = new JTextField();
            // 이 테스트는 숫자 키 전달을 검사하며 OS 입력기의 기존 조합 문자는 받지 않는다.
            input[0].enableInputMethods(false);
            InventoryPanel inventory = new InventoryPanel();
            inventory.setItems(List.of(ItemId.NOTE1, ItemId.NOTE2), List.of("메모 1", "메모 2"));
            inventory.setOnItemSelected(selected::set);
            frame[0].add(focus[0], BorderLayout.NORTH);
            frame[0].add(input[0], BorderLayout.CENTER);
            frame[0].add(inventory, BorderLayout.SOUTH);
            frame[0].setSize(460, 220);
            frame[0].setLocationRelativeTo(null);
            frame[0].setAlwaysOnTop(true);
            frame[0].setVisible(true);
            frame[0].toFront();
            focus[0].requestFocusInWindow();
        });
        try {
            Robot robot = new Robot();
            robot.waitForIdle();
            robot.delay(250);
            java.awt.Point[] point = new java.awt.Point[1];
            SwingUtilities.invokeAndWait(() -> {
                point[0] = focus[0].getLocationOnScreen();
                point[0].translate(focus[0].getWidth() / 2, focus[0].getHeight() / 2);
            });
            robot.mouseMove(point[0].x, point[0].y);
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            robot.delay(250);
            SwingUtilities.invokeAndWait(() -> assertTrue(focus[0].hasFocus()));
            robot.keyPress(KeyEvent.VK_2);
            robot.keyRelease(KeyEvent.VK_2);
            robot.waitForIdle();
            assertEquals(ItemId.NOTE2, selected.get());
            SwingUtilities.invokeAndWait(() -> input[0].requestFocusInWindow());
            robot.waitForIdle();
            robot.delay(100);
            robot.keyPress(KeyEvent.VK_1);
            robot.keyRelease(KeyEvent.VK_1);
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> assertEquals("1", input[0].getText()));
            assertEquals(ItemId.NOTE2, selected.get());
        } finally {
            SwingUtilities.invokeAndWait(() -> frame[0].dispose());
        }
    }
}
