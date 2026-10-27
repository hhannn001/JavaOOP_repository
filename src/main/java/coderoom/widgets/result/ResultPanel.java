package coderoom.widgets.result;

import javax.swing.JPanel;

/**
 * 담당: G
 * 역할: 탈출 결과와 기록 저장 상태
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class ResultPanel extends JPanel {
    // [연결]
    // 표시 데이터와 콜백을 GamePage에서 받는다. 다른 widgets 슬라이스를 참조하지 않는다.

    // [구현 예정]
    // 확정된 탈출 시간, 저장 중·저장 완료·기록 미저장 상태를 표시한다.
    // 재시도·재시작·맵 복귀 요청을 콜백으로 전달한다.
    // DB에 직접 접근하지 않고 저장 실패가 탈출 실패로 표시되지 않도록 한다.
}
