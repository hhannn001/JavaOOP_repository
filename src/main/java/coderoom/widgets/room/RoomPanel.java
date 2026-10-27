package coderoom.widgets.room;

import javax.swing.JPanel;

/**
 * 담당: D
 * 역할: 방 그림과 물건 클릭 영역
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class RoomPanel extends JPanel {
    // [연결]
    // 표시 데이터와 콜백을 GamePage에서 받는다. 다른 widgets 슬라이스를 참조하지 않는다.

    // [구현 예정]
    // paintComponent(Graphics): 배경·서랍·벽 다이얼·문과 열린 상태를 그린다.
    // 표시 크기에 맞게 그리기와 클릭 좌표에 같은 변환을 적용한다.
    // 마우스 클릭을 entities.game.RoomObjectId로 변환해 콜백으로 전달한다.
    // 책·창문 같은 조사 대상과 호버 피드백을 표시한다.
}
