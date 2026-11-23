package coderoom.entities.map;

import java.util.List;
import java.util.Optional;

//맵 데이터를 조회하기 위한 인터페이스
/** 맵 데이터 조회를 위한 저장소 계약이다. */
public interface MapRepository {

	 // [조회 계약]
    // findAll(): List<GameMap>.
    // findById(mapId): Optional<GameMap>.

    // [실패 처리]
    // 조회·저장 장애는 coderoom.shared.db.StorageException으로 전달한다.
	
    // 모든 맵 조회
    List<GameMap> findAll();

    // 맵 ID로 특정 맵 조회
    Optional<GameMap> findById(int mapId);
}
