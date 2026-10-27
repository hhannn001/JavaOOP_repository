package coderoom.entities.puzzle;

/**
 * 담당: E
 * 역할: 도메인 저장소 계약.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public interface PuzzleRepository {
    // [메서드 계약 예정]
    // findByMapId(mapId): List<Puzzle>.

    // [실패 처리]
    // 조회·저장 장애는 coderoom.shared.db.StorageException으로 전달한다.
    // 인터페이스 메서드 선언도 구현 단계에서 추가한다. 현재는 주석만 둔다.
}
