
package coderoom.entities.map;

import java.util.List;
import java.util.Optional;

//맵 데이터를 조회하기 위한 인터페이스
public interface MapRepository {

    // 모든 맵 조회
    List<GameMap> findAll();

    // 맵 ID로 특정 맵 조회
    Optional<GameMap> findById(int mapId);
}
