package coderoom.features.mapcatalog;

/**
 * 담당: C
 * 역할: 맵과 사용자별 최고 기록 결합.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import coderoom.entities.map.GameMap;
import coderoom.entities.map.MapRepository;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.record.ClearRecordRepository;

public class MapCatalogService {

	// [연결]
    // coderoom.entities.map.MapRepository와 coderoom.entities.record.ClearRecordRepository를 받는다.

    // [구현 예정]
    // listForUser(userId): 맵 목록과 해당 계정의 기록을 합쳐 List<MapCard>를 반환한다.
    // DB 장애를 빈 목록이나 미완료 기록으로 숨기지 않는다.
	
    private final MapRepository mapRepository;
    private final ClearRecordRepository recordRepository;

    // 맵 저장소와 기록 저장소 연결
    public MapCatalogService(
            MapRepository mapRepository,
            ClearRecordRepository recordRepository) {

    	this.mapRepository = Objects.requireNonNull(mapRepository);
    	this.recordRepository = Objects.requireNonNull(recordRepository);
    }

    // 전체 맵 목록과 현재 사용자의 기록을 결합
    public List<MapCard> listForUser(long userId) {

    	if (userId <= 0) {
            throw new IllegalArgumentException("잘못된 사용자 ID입니다.");
        }
    	
        List<GameMap> maps = mapRepository.findAll();

        List<ClearRecord> records =
                recordRepository.findByUserId(userId);

        // 맵 ID를 기준으로 기록 검색
        Map<Long, ClearRecord> recordMap = new HashMap<>();

        for (ClearRecord record : records) {
            recordMap.put(
                    record.getMapId(),
                    record
            );
        }

        List<MapCard> cards = new ArrayList<>();

        for (GameMap map : maps) {

            ClearRecord record =
                    recordMap.get((long) map.getMapId());

            boolean cleared = record != null;

            Long bestSeconds = cleared
                    ? record.getBestSeconds()
                    : null;

            MapCard card = new MapCard(
                    map.getMapId(),
                    map.getTitle(),
                    map.getDescription(),
                    map.getTopic(),
                    map.getDifficulty(),
                    map.getPosterPath(),
                    map.isPlayable(),
                    cleared,
                    bestSeconds
            );

            cards.add(card);
        }

        return cards;
    }
}
