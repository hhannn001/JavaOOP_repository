package coderoom.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coderoom.entities.map.JdbcMapRepository;
import coderoom.shared.db.Database;
import java.nio.file.Path;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HintSettingsTest {
    @TempDir Path directory;

    @Test void existingDatabaseGetsHintBudgetWithoutLosingMapsOrCustomSettings() throws Exception {
        Database database = new Database(directory.resolve("old.db").toString());
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE maps (map_id INTEGER PRIMARY KEY, title TEXT NOT NULL, "
                    + "description TEXT NOT NULL, topic TEXT NOT NULL, difficulty TEXT NOT NULL, "
                    + "poster_path TEXT NOT NULL, playable INTEGER NOT NULL)");
            statement.execute("INSERT INTO maps VALUES (1,'existing','description','Java','easy','poster',1)");
        }
        database.initialize();
        JdbcMapRepository maps = new JdbcMapRepository(database);
        assertEquals("existing", maps.findById(1).orElseThrow().getTitle());
        assertEquals(2, maps.findById(1).orElseThrow().getHintLimit());
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("UPDATE maps SET hint_limit=1 WHERE map_id=1");
            assertThrows(SQLException.class, () -> statement.execute("UPDATE maps SET hint_limit=-1"));
        }
        database.initialize();
        assertEquals(1, maps.findById(1).orElseThrow().getHintLimit());
    }
}
