package coderoom.features.mapcatalog;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import coderoom.drivers.CMapDriver;
import coderoom.entities.map.GameMap;
import coderoom.entities.map.MapRepository;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.record.ClearRecordRepository;
import coderoom.testkit.TestPorts;

class MapCatalogServiceTest {

    @Test
    void mapCatalogDriverTest() {

        // 1. 테스트용 맵 데이터 준비
        MapRepository mapRepository = new MapRepository() {

            @Override
            public List<GameMap> findAll() {
                return List.of(
                    new GameMap(1, "맵 1", "설명 1",
                        "추리", "쉬움", "map1.png", true),

                    new GameMap(2, "맵 2", "설명 2",
                        "공포", "보통", "map2.png", false),

                    new GameMap(3, "맵 3", "설명 3",
                        "모험", "어려움", "map3.png", false)
                );
            }

            @Override
            public Optional<GameMap> findById(int mapId) {
                return findAll().stream()
                    .filter(map -> map.getMapId() == mapId)
                    .findFirst();
            }
        };

        // 2. 테스트용 기록 저장소 준비
        ClearRecordRepository recordRepository =
            new ClearRecordRepository() {

                @Override
                public List<ClearRecord> findByUserId(long userId) {
                    return List.of();
                }

                @Override
                public void saveBest(ClearRecord record) {
                    // 이번 테스트에서는 기록을 저장하지 않음
                }
            };

        // 3. 실제 서비스 생성
        MapCatalogService service =
            new MapCatalogService(mapRepository, recordRepository);

        // 4. 실제 서비스 결과를 테스트용 Card로 변환
        TestPorts.Catalog adapter = userId ->
            service.listForUser(userId).stream()
                .map(card -> new TestPorts.Card(
                    card.getMapId(),
                    card.isPlayable(),
                    card.getBestSeconds()
                ))
                .toList();

        // 5. 팀에서 제공한 테스트 실행
        CMapDriver.verify(adapter);
    }
    @Test
    void clearRecordTest() {

        // 1. 테스트용 맵 데이터 준비
        MapRepository mapRepository = new MapRepository() {

            @Override
            public List<GameMap> findAll() {
                return List.of(
                    new GameMap(1, "맵 1", "설명 1",
                        "추리", "쉬움", "map1.png", true),

                    new GameMap(2, "맵 2", "설명 2",
                        "공포", "보통", "map2.png", false),

                    new GameMap(3, "맵 3", "설명 3",
                        "모험", "어려움", "map3.png", false)
                );
            }

            @Override
            public Optional<GameMap> findById(int mapId) {
                return findAll().stream()
                    .filter(map -> map.getMapId() == mapId)
                    .findFirst();
            }
        };

        // 2. 클리어 기록이 있는 저장소 준비
        ClearRecordRepository recordRepository =
            new ClearRecordRepository() {

                @Override
                public List<ClearRecord> findByUserId(long userId) {
                    return List.of(
                        new ClearRecord(1L, 1L, 120L)
                    );
                }

                @Override
                public void saveBest(ClearRecord record) {
                }
            };

        // 3. 실제 서비스 실행
        MapCatalogService service =
            new MapCatalogService(mapRepository, recordRepository);

        List<MapCard> cards = service.listForUser(1L);

        // 4. 결과 검증
        assertTrue(cards.get(0).isCleared());
        assertEquals(120L, cards.get(0).getBestSeconds());

        assertFalse(cards.get(1).isCleared());
        assertNull(cards.get(1).getBestSeconds());
    }
    @Test
    void invalidUserIdTest() {

        MapRepository mapRepository = new MapRepository() {

            @Override
            public List<GameMap> findAll() {
                return List.of();
            }

            @Override
            public Optional<GameMap> findById(int mapId) {
                return Optional.empty();
            }
        };

        ClearRecordRepository recordRepository =
            new ClearRecordRepository() {

                @Override
                public List<ClearRecord> findByUserId(long userId) {
                    return List.of();
                }

                @Override
                public void saveBest(ClearRecord record) {
                }
            };

        MapCatalogService service =
            new MapCatalogService(mapRepository, recordRepository);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.listForUser(0L)
        );
    }
}
