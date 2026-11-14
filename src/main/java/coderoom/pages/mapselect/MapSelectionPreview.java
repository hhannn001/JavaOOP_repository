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

            // 테스트 1: 정상적인 맵
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

            // 테스트 2: 잘못된 이미지 경로
            cards.add(new MapCard(
                2,
                "Missing Notes",
                "이미지 경로 오류 테스트",
                "Java",
                "어려움",
                "/images/posters/wrong_image.png",
                false,
                false,
                null
            ));

            // 테스트 3: 제목과 설명이 비어 있는 맵
            cards.add(new MapCard(
                3,
                "",
                "",
                null,
                "어려움",
                "/images/posters/room03_stopped_machine.png",
                false,
                false,
                null
            ));

            // 맵 선택 화면 생성
            MapSelectionPage page = new MapSelectionPage();

            // 테스트할 목록 선택
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
