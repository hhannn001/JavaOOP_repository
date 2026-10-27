package coderoom.entities.puzzle;

/**
 * 담당: E
 * 역할: 코드 퍼즐과 정답 판정.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class Puzzle {
    // [필드 예정]
    // puzzleId, mapId, stepNo, code, AnswerType answerType, answer, hint.

    // [구현 예정]
    // check(rawAnswer): 형식 오류·오답·정답을 AnswerCheck로 반환한다.
    // DIGITS4는 숫자 4개, GRID3X3은 행 순서의 숫자 9개로 검증한다.
    // 정답은 문자열로 비교해 앞자리 0을 보존한다. Java 코드를 실제 실행하지 않는다.
    // 게임 단계나 보상은 변경하지 않고 정답을 화면 DTO로 내보내지 않는다.
}
