package coderoom.entities.game;

/**
 * 담당: E
 * 역할: 선형 게임 진행 단계
 * 직접 선언한 상속·구현 관계 없음.
 * 주의: 선언 순서(START → DRAWER_OPEN → SAFE_OPEN → ESCAPED)가 곧 진행 순서이다.
 * RoomPanel이 "이 단계 이상인가"를 순서로 비교하므로 순서를 바꾸지 않는다.
 */
public enum GameStage {
    START, DRAWER_OPEN, SAFE_OPEN, ESCAPED
}
