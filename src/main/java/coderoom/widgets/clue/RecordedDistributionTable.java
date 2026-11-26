package coderoom.widgets.clue;

import coderoom.shared.ui.PixelTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Device nameplates from the original priority-based distribution puzzle. */
final class RecordedDistributionTable {
    private static final Pattern BUDGET = Pattern.compile("합계\\s+(\\d+)까지");
    private static final Pattern DEVICE = Pattern.compile(
            "\\b([A-Z][A-Z_]+)\\s+(\\d+)\\s*/\\s*(\\d+)([ \\t]+고장)?(?=\\s*[,\\.])");
    private static final Color ROW  = new Color(211, 185, 134);
    private static final Color FAULT = new Color(127, 42, 29);

    private RecordedDistributionTable() { }

    static Optional<JComponent> create(String code) {
        var budget  = BUDGET.matcher(code);
        var devices = DEVICE.matcher(code).results().toList();
        if (!code.contains("분배 작업표") || !budget.find() || devices.isEmpty()) {
            return Optional.empty();
        }

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(PixelTheme.PAPER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 3));
        heading.setOpaque(false);
        heading.add(label("분배 작업표  ·  전력 한도 " + budget.group(1) + "W", 18));
        heading.add(label("우선순위 숫자가 작은 정상 장비부터 가동하세요.", 15));
        panel.add(heading, BorderLayout.NORTH);

        JPanel nameplates = new JPanel(new GridLayout(0, 4, 2, 2));
        nameplates.setOpaque(false);
        for (String column : new String[] {"장비", "소비 전력", "우선순위", "상태"}) {
            nameplates.add(cell(column, true));
        }
        for (var device : devices) {
            boolean failed = device.group(4) != null;
            nameplates.add(cell(device.group(1), false));
            nameplates.add(cell(device.group(2) + "W", false));
            nameplates.add(cell(device.group(3), false));
            JLabel state = cell(failed ? "고장" : "정상", false);
            if (failed) { state.setForeground(FAULT); }
            nameplates.add(state);
        }
        panel.add(nameplates, BorderLayout.CENTER);
        if (code.contains("Integer.compare(b.priority, a.priority)") && code.contains("used + d.power >=")) {
            JLabel note = label("저장된 코드와 작업표의 정렬 순서·한도 조건이 서로 다릅니다.", 14);
            note.setForeground(FAULT);
            panel.add(note, BorderLayout.SOUTH);
        }
        return Optional.of(panel);
    }

    private static JLabel cell(String value, boolean heading) {
        JLabel cell = label(value, 15);
        cell.setOpaque(true);
        cell.setBackground(heading ? PixelTheme.INK : ROW);
        cell.setForeground(heading ? PixelTheme.LIGHT : PixelTheme.INK);
        cell.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        return cell;
    }

    private static JLabel label(String value, int size) {
        JLabel label = new JLabel(value, SwingConstants.CENTER);
        label.setFont(PixelTheme.font(size));
        label.setForeground(PixelTheme.INK);
        return label;
    }
}
