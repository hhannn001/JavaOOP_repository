package coderoom.features.gameplay.ui;

import javax.swing.JPanel;

/**
 * 담당: F
 * 역할: 두 종류의 암호 입력 화면.
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class AnswerInputPanel extends JPanel {
    // [연결]
    // entities.puzzle.AnswerType과 표시 문구·제출 콜백을 받는다. 정답은 받지 않는다.

    // [구현 예정]
    // DIGITS4는 4자리 문자열 입력, GRID3X3은 9개 숫자 입력칸을 표시한다.
    // 3×3 값은 왼쪽 위부터 행 순서로 이어 붙이고 앞자리 0을 유지한다.
    // 빈 입력·숫자 외 입력·길이 오류를 안내하고 키보드 이동과 제출을 지원한다.
    // 힌트 요청 콜백과 메시지 표시 기능을 제공한다.
    // 입력값을 콜백으로 넘기고 정답 비교는 하지 않는다. Factory Method는 도입하지 않는다.
}
