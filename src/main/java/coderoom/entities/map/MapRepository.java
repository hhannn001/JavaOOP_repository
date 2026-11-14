package coderoom.entities.map;

/**
 * 담당: C
 * 역할: 도메인 저장소 계약.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */

import java.util.List;
import java.util.Optional;

//맵 데이터를 조회하기 위한 인터페이스
public interface MapRepository {

	 // [메서드 계약 예정]
    // findAll(): List<GameMap>.
    // findById(mapId): Optional<GameMap>.

    // [실패 처리]
    // 조회·저장 장애는 coderoom.shared.db.StorageException으로 전달한다.
    // 인터페이스 메서드 선언도 구현 단계에서 추가한다. 현재는 주석만 둔다.
	
    // 모든 맵 조회
    List<GameMap> findAll();

    // 맵 ID로 특정 맵 조회
    Optional<GameMap> findById(int mapId);
}
