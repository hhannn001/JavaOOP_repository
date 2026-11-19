package coderoom.widgets.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.GameStage;
import coderoom.entities.game.RoomObjectId;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** D 담당: 어떤 창 크기에서도 클릭이 맞게 전달되는지, 단계가 바뀌면 그림이 바뀌는지 확인한다. */
class RoomPanelTest {
    private static final int W = 1586;
    private static final int H = 992;

    /** 그림 원본 좌표에서 서랍·다이얼·문의 중앙 근처. */
    private static final double[][] SPOTS = {{667, 623}, {1041, 382}, {1377, 424}};

    private static int[] toScreen(int width, int height, double[] spot) {
        double scale = Math.min(width / (double) W, height / (double) H);
        double x = (width - W * scale) / 2 + spot[0] * scale;
        double y = (height - H * scale) / 2 + spot[1] * scale;
        return new int[] {(int) Math.round(x), (int) Math.round(y)};
    }

    private static void mouse(RoomPanel panel, int type, int x, int y) {
        panel.dispatchEvent(new MouseEvent(panel, type, 0L, 0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static void click(RoomPanel panel, int x, int y) {
        mouse(panel, MouseEvent.MOUSE_PRESSED, x, y);
        mouse(panel, MouseEvent.MOUSE_RELEASED, x, y);
    }

    private static int[] picture(RoomPanel panel) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        return image.getRGB(0, 0, W, H, null, 0, W);
    }

    @Test
    void clicksReachTheRightObjectAtAnyWindowSize() {
        for (int[] size : new int[][] {{1586, 992}, {1280, 720}, {800, 600}, {500, 900}, {2400, 700}}) {
            List<RoomObjectId> got = new ArrayList<>();
            RoomPanel panel = new RoomPanel(got::add);
            panel.setSize(size[0], size[1]);
            for (double[] spot : SPOTS) {
                int[] p = toScreen(size[0], size[1], spot);
                click(panel, p[0], p[1]);
            }
            assertEquals(List.of(RoomObjectId.DRAWER, RoomObjectId.WALL_DIAL, RoomObjectId.DOOR), got,
                    size[0] + "x" + size[1]);
        }
    }

    @Test
    void emptySpotsAndDraggingOffAreNotClicks() {
        List<RoomObjectId> got = new ArrayList<>();
        RoomPanel panel = new RoomPanel(got::add);
        panel.setSize(2400, 700);                                   // 좌우에 빈 여백이 생기는 창
        click(panel, 5, 350);                                       // 여백
        int[] rug = toScreen(2400, 700, new double[] {800, 900});
        click(panel, rug[0], rug[1]);                               // 카펫
        int[] drawer = toScreen(2400, 700, SPOTS[0]);
        int[] door = toScreen(2400, 700, SPOTS[2]);
        mouse(panel, MouseEvent.MOUSE_PRESSED, drawer[0], drawer[1]);
        mouse(panel, MouseEvent.MOUSE_RELEASED, door[0], door[1]);  // 서랍에서 눌러 문에서 뗌
        panel.setSize(0, 0);
        click(panel, 0, 0);                                         // 크기 0이어도 오류 없음
        assertTrue(got.isEmpty());
    }

    @Test
    void everyStageLooksDifferentAndRestartGoesBackToStart() {
        RoomPanel panel = new RoomPanel(x -> { });
        panel.setSize(W, H);
        int[] start = picture(panel);
        int[] previous = start;
        for (GameStage stage : new GameStage[] {GameStage.DRAWER_OPEN, GameStage.SAFE_OPEN, GameStage.ESCAPED}) {
            panel.setStage(stage);
            int[] now = picture(panel);
            assertTrue(!Arrays.equals(previous, now), stage + " 단계에서 그림이 바뀌어야 한다");
            previous = now;
        }
        panel.setStage(GameStage.START);
        assertTrue(Arrays.equals(start, picture(panel)), "시작 단계로 돌아가면 처음 그림이어야 한다");
    }
}
