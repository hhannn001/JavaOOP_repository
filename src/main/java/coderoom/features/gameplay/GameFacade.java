package coderoom.features.gameplay;

/**
 * 담당: E
 * 역할: 게임 조작의 Facade 진입점.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class GameFacade {
    // [연결]
    // coderoom.entities.map.MapRepository, entities.puzzle.PuzzleRepository, entities.record.ClearRecordRepository를 받는다.
    // 같은 패키지 GameSetup과 entities.game.GameSession을 보유한다.

    // [구현 예정]
    // loadMap(mapId): 작업 스레드에서 맵·퍼즐을 읽고 플레이 가능 여부·퍼즐 2개의 순서와 형식을 검증한다.
    // start(userId, setup, nowNanos): EDT에서 새 게임 상태를 생성하고 GameView를 반환한다.
    // inspect(objectId): 상태 변경 없이 설명 또는 입력창 정보인 InspectResult를 반환한다.
    // submit(objectId, rawAnswer): 현재 단계와 장치를 확인하고 Puzzle.check 결과에 따라 세션 전이를 요청한다.
    // selectItem(itemId): 획득한 아이템만 선택하고 GameView를 반환한다.
    // useKey(nowNanos): 열쇠 선택 여부를 확인하고 탈출 시간·ClearRecord가 담긴 EscapeResult를 반환한다.
    // saveClear(record): 작업 스레드에서 저장소에 전달한다. GameSession에는 접근하지 않는다.
    // view(nowNanos): 정답을 제외한 읽기 전용 GameView를 만든다. DB는 조회하지 않는다.

    // [경계]
    // SQL·Swing 그리기·비밀번호 검증을 넣지 않는다. 다른 features 슬라이스를 참조하지 않는다.
}
