
package coderoom.features.mapcatalog;

import java.util.ArrayList;
import java.util.List;

import coderoom.entities.map.GameMap;
import coderoom.entities.map.MapRepository;

public class MapCatalogService {

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
