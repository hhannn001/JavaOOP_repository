package coderoom.app;

import javax.swing.JFrame;

/**
 * 담당: A
 * 역할: 앱에서 사용하는 하나의 최상위 창.
 * 상위 타입: javax.swing.JFrame
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class MainFrame extends JFrame {
    // [필드·연결]
    // java.awt.CardLayout과 화면 컨테이너를 보유한다.

    // [구현 예정]
    // showPage(name): 로그인·맵 선택·게임 화면을 전환한다.
    // 창 종료 시 현재 GamePage의 타이머와 작업을 정리하도록 콜백을 호출한다.
    // 게임 화면에서는 방이 창의 전체 콘텐츠 영역을 채우도록 배치한다.
}
