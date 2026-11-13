
package coderoom.pages.mapselect;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import coderoom.features.mapcatalog.MapCard;

public class MapSelectionPreview {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // 테스트용 맵 데이터
            List<MapCard> cards = new ArrayList<>();

            // 첫 번째 맵: 플레이 가능
            cards.add(new MapCard(
                1,
                "The Last Compile",
                "마지막 컴파일을 완성하라.",
                "Java",
                "보통",
                "/images/posters/room01_last_compile.png",
                true,
                false,
                null
            ));

            // 두 번째 맵: 준비 중
            cards.add(new MapCard(
                2,
                "Missing Notes",
                "사라진 코드를 찾아라.",
                "Java",
                "어려움",
                "/images/posters/room02_missing_notes.png",
                false,
                false,
                null
            ));

            // 세 번째 맵: 준비 중
            cards.add(new MapCard(
                3,
                "Stopped Machine",
                "멈춰버린 기계를 작동시켜라.",
                "Python",
                "어려움",
                "/images/posters/room03_stopped_machine.png",
                false,
                false,
                null
            ));

            // 맵 선택 화면 생성
            MapSelectionPage page = new MapSelectionPage();
            page.render(cards);

            // 테스트 창 생성
            JFrame frame = new JFrame("CODE ROOM - MAP SELECT");

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 650);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(page);
            frame.setVisible(true);
        });
    }
}
