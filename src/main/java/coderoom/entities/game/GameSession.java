package coderoom.entities.game;

/**
 * 담당: E
 * 역할: 플레이 한 회의 메모리 상태와 전이 규칙.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class GameSession {
    // [필드 예정]
    // userId, mapId, GameStage stage, ItemId selectedItem.
    // startedAtNanos, 탈출 전에는 값이 없는 clearedSeconds.

    // [구현 예정]
    // inventory(): 단계에서 NOTE1 / NOTE1·NOTE2 / NOTE1·NOTE2·KEY 보유 목록을 계산한다.
    // select(itemId): 미획득 아이템은 거절한다.
    // unlockDrawer(), unlockSafe(): 선행 단계 확인 후 한 번만 진행한다.
    // escape(nowNanos): SAFE_OPEN과 KEY 선택을 확인하고 시간을 한 번만 확정한다.
    // 상태 변경은 EDT에서만 실행하고 재시작 시 새 세션을 생성한다.

    // [경계]
    // Swing·SQL·entities.puzzle 등 다른 엔티티 슬라이스를 참조하지 않는다.
    // 정답 판정과 상태 전이의 결합은 상위 GameFacade가 담당한다.
}
