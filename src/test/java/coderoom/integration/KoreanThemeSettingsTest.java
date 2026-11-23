package coderoom.integration;

import coderoom.entities.map.JdbcMapRepository;
import coderoom.shared.db.Database;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoreanThemeSettingsTest {
    @TempDir Path directory;

    @Test void legacyThemesRefreshWithoutResettingProgressOrGameplaySettings() throws Exception {
        Database database = new Database(directory.resolve("themes.db").toString());
        database.initialize();
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("UPDATE maps SET title='The Last Compile', description='old', hint_limit=1 WHERE map_id=1");
            statement.execute("INSERT INTO clear_records VALUES(4,1,123)");
        }
        database.initialize();
        database.initialize();
        var maps = new JdbcMapRepository(database).findAll();
        assertEquals(java.util.List.of("서재의 마지막 암호", "새벽 온실의 마지막 씨앗", "자정의 기계실"),
                maps.stream().map(map -> map.getTitle()).toList());
        assertEquals(1, maps.get(0).getHintLimit());
        assertTrue(maps.get(0).isPlayable());
        assertEquals(3, maps.stream().filter(map -> map.isPlayable()).count());
        for (var map : maps) {
            assertTrue(map.getDescription().length() > 80);
            assertNotNull(getClass().getResource(map.getPosterPath()));
        }
        try (var connection = database.open(); var statement = connection.createStatement();
                var row = statement.executeQuery("SELECT best_seconds FROM clear_records WHERE user_id=4 AND map_id=1")) {
            assertTrue(row.next());
            assertEquals(123, row.getLong(1));
        }
    }
}
