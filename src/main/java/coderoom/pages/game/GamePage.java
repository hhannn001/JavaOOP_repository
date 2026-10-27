package coderoom.pages.game;

import javax.swing.JPanel;

/**
 * 담당: A
 * 역할: 방 화면과 게임 조작의 연결
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class GamePage extends JPanel {
    // [연결]
    // coderoom.features.gameplay.GameFacade를 생성자로 받는다.
    // widgets의 RoomPanel·InventoryPanel·CluePanel·ResultPanel과 gameplay.ui.AnswerInputPanel을 조립한다.

    // [구현 예정]
    // render(view): 동일한 GameView를 각 위젯에 전달한다.
    // 물건 클릭·암호 제출·아이템 선택·열쇠 사용 콜백을 GameFacade에 연결한다.
    // loadMap은 작업 스레드, start와 게임 상태 변경은 EDT에서 처리한다.
    // 탈출 결과의 불변 ClearRecord를 작업 스레드에서 저장하고 실패 시 재시도를 제공한다.
    // javax.swing.Timer로 경과 시간 표시를 갱신하고 disposeGame에서 정리한다.
    // 작업 중 중복 실행·화면 이동을 막고 결과/저장 실패를 구분해 표시한다.

    // [경계]
    // app.AppController나 다른 pages 슬라이스를 직접 import하지 않는다.
}
