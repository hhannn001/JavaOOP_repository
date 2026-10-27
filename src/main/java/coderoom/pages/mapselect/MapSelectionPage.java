package coderoom.pages.mapselect;

import javax.swing.JPanel;

/**
 * 담당: C
 * 역할: 포스터형 맵 선택 화면
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class MapSelectionPage extends JPanel {
    // [연결]
    // coderoom.features.mapcatalog.MapCatalogService와 현재 사용자 ID를 전달받는다.
    // 맵 선택은 LongConsumer 콜백으로 상위에 알린다.

    // [구현 예정]
    // render(cards): 플레이 가능한 포스터 1개와 준비 중 포스터 2개를 표시한다.
    // 클리어 여부·최고 기록을 표시하고 준비 중 맵의 입장을 막는다.
    // 목록 조회는 SwingWorker로 처리하고 게임 복귀 시 기록을 다시 읽는다.

    // [경계]
    // app.AppController나 다른 pages 슬라이스를 직접 import하지 않는다.
}
