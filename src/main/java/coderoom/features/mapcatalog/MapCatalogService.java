package coderoom.features.mapcatalog;

/**
 * 담당: C
 * 역할: 맵과 사용자별 최고 기록 결합.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */

import java.util.ArrayList;
import java.util.List;

import coderoom.entities.map.GameMap;
import coderoom.entities.map.MapRepository;

public class MapCatalogService {

	// [연결]
    // coderoom.entities.map.MapRepository와 coderoom.entities.record.ClearRecordRepository를 받는다.

    // [구현 예정]
    // listForUser(userId): 맵 목록과 해당 계정의 기록을 합쳐 List<MapCard>를 반환한다.
    // DB 장애를 빈 목록이나 미완료 기록으로 숨기지 않는다.
	
    private final MapRepository mapRepository;

    // 생성자
    public MapCatalogService(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    // 전체 맵 목록을 가져와 화면 표시용 데이터로 변환
    public List<MapCard> listForUser(int userId) {

        List<GameMap> maps = mapRepository.findAll();
        List<MapCard> cards = new ArrayList<>();

        for (GameMap map : maps) {

            MapCard card = new MapCard(
                map.getMapId(),
                map.getTitle(),
                map.getDescription(),
                map.getTopic(),
                map.getDifficulty(),
                map.getPosterPath(),
                map.isPlayable(),
                false,
                null
            );

            cards.add(card);
        }

        return cards;
    }
}
