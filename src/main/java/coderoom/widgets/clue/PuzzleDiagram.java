package coderoom.widgets.clue;

import coderoom.features.gameplay.ItemView;
import coderoom.features.gameplay.PuzzleVariant;
import coderoom.shared.ui.PixelTheme;
import coderoom.shared.ui.SpecimenMark;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JPanel;

/** Paper schematics of the inputs and rules. Empty slots are left for the player to solve. */
final class PuzzleDiagram extends JPanel {
    private static final int WIDTH  = 320;
    private static final int HEIGHT = 240;
    private static final Color BRASS = new Color(119, 74, 35);
    private static final Color WASH  = new Color(143, 91, 42, 33);

    private static final Pattern NUMBER      = Pattern.compile("-?\\d+");
    private static final Pattern GRID_VALUES = Pattern.compile("\\bgrid\\s*=\\s*\\{([^;]+)}");

    private ItemView clue;
    private List<String> numbers = List.of();

    PuzzleDiagram() {
        setName("puzzleDiagram");
        setOpaque(false);
    }

    void setClue(ItemView clue) {
        this.clue = clue;
        if (clue.diagram() == ItemView.Diagram.GRID) {
            var initial = GRID_VALUES.matcher(clue.content());
            numbers = initial.find() ? NUMBER.matcher(initial.group(1)).results().map(match -> match.group()).toList() : List.of();
        }
        getAccessibleContext().setAccessibleName(clue.title() + " 그림");
        getAccessibleContext().setAccessibleDescription(clue.content());
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (clue == null) { return; }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
            g.translate((getWidth() - WIDTH * scale) / 2, (getHeight() - HEIGHT * scale) / 2);
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.5f));

            switch (clue.diagram()) {
                case GRID -> grid(g);
                case DISTRIBUTION -> distribution(g);
                case SEEDS -> seeds(g);
                case NONE, BALANCE -> { }
            }
        } finally {
            g.dispose();
        }
    }

    private void grid(Graphics2D g) {
        heading(g, "표의 열을 읽어 다이얼의 행으로");
        for (int index = 0; index < 9; index++) {
            int column = index % 3;
            int row    = index / 3;
            String value = index < numbers.size() ? numbers.get(index) : "";
            box(g, 21 + column * 34, 59 + row * 34, 30, 30, value, 16);
            box(g, 197 + column * 34, 59 + row * 34, 30, 30, "", 16);
        }
        right(g, 131, 186, 106);
        center(g, "출발 숫자표", 70, 176, 14);
        center(g, "벽의 다이얼", 246, 176, 14);
        center(g, "숫자표의 각 열은 위에서 아래로 읽기", 160, 206, 14);
    }

    private void distribution(Graphics2D g) {
        PuzzleVariant variant = clue.variant();
        heading(g, "전력 예산 " + variant.distributionCapacity() + "W");
        String queue = variant.distributionQueue();
        for (int i = 0; i < queue.length(); i++) {
            char device = queue.charAt(i);
            int index   = device - 'A';
            int x       = 8 + i * 62;
            box(g, x, 54, 52, 33, Character.toString(device), 16);
            center(g, PuzzleVariant.deviceWatts(index) + "W", x + 26, 113, 15);
            center(g, PuzzleVariant.operational(index) ? "정상" : "고장", x + 26, 135, 13);
            if (i < queue.length() - 1) { right(g, x + 54, x + 61, 70); }
        }
        center(g, "왼쪽부터 정상이고 남은 전력이 충분한 장비 선택", 160, 175, 13);
        center(g, "선택할 때마다 남은 전력에서 사용량 빼기", 160, 202, 14);
    }

    private void seeds(Graphics2D g) {
        var rule = clue.variant().specimenRule();
        heading(g, "봉투 표식으로 씨앗 분류");
        mark(g, rule.candidateLeaf(), rule.candidateLeaf().description(), 34, 52);
        center(g, "또는", 143, 76, 14);
        mark(g, rule.candidateStamp(), rule.candidateStamp().description() + " 도장", 176, 52);
        center(g, "위 조건 중 하나를 만족하면 후보", 160, 114, 14);
        mark(g, SpecimenMark.DAMAGED, "손상", 32, 130);
        center(g, "또는", 116, 154, 13);
        mark(g, rule.excludedLeaf(), "", 144, 130);
        center(g, "+", 181, 154, 16);
        mark(g, rule.excludedStamp(), rule.excludedStamp().description(), 195, 130);
        center(g, "손상 또는 위의 표식 조합이면 제외", 160, 194, 14);
        center(g, "남길 봉투를 열고 황동 버튼으로 수거", 160, 220, 14);
    }

    private static void mark(Graphics2D g, SpecimenMark mark, String label, int x, int y) {
        g.setColor(BRASS);
        g.fillRoundRect(x + 3, y + 3, 27, 27, 6, 6);
        String asset = "specimen-marks/" + mark.name().toLowerCase(java.util.Locale.ROOT);
        PixelTheme.asset(g, asset, x + 2, y + 2, 29, 29);
        center(g, label, x + 34 + label.length() * 4, y + 24, 13);
    }

    private static void box(Graphics2D g, int x, int y, int width, int height, String label, int size) {
        g.setColor(WASH);
        g.fillRoundRect(x, y, width, height, 7, 7);
        g.setColor(BRASS);
        g.drawRoundRect(x, y, width, height, 7, 7);
        center(g, label, x + width / 2, y + height / 2 + size / 3, size);
    }

    private static void heading(Graphics2D g, String text) {
        center(g, text, 160, 28, 17);
    }

    private static void center(Graphics2D g, String text, int x, int baseline, int size) {
        g.setColor(PixelTheme.INK);
        g.setFont(PixelTheme.font(size).deriveFont(Font.BOLD));
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    private static void right(Graphics2D g, int start, int end, int y) {
        g.setColor(BRASS);
        g.drawLine(start, y, end, y);
        g.drawLine(end - 4, y - 4, end, y);
        g.drawLine(end - 4, y + 4, end, y);
    }
}
