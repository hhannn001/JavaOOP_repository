package coderoom.features.dashboard;

import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardRecordsTest {
    @TempDir Path directory;

    @Test void rankingIncludesNinePeopleAndPersonalRankOutsideTopNine() throws Exception {
        Database database = new Database(directory.resolve("ranking.db").toString());
        database.initialize();
        try (Connection connection = database.open();
                PreparedStatement user = connection.prepareStatement("INSERT INTO users VALUES (?, ?, 'hash')");
                PreparedStatement record = connection.prepareStatement("INSERT INTO clear_records VALUES (?, ?, ?)")) {
            for (int index = 1; index <= 12; index++) {
                user.setLong(1, 100 + index);
                user.setString(2, "player" + index);
                user.executeUpdate();
                record.setLong(1, 100 + index);
                record.setLong(2, 1);
                record.setLong(3, index == 2 ? 10 : index * 10L);
                record.executeUpdate();
            }
            record.setLong(1, 112);
            record.setLong(2, 2);
            record.setLong(3, 0);
            record.executeUpdate();
        }
        DashboardRecords records         = new DashboardRecords(database);
        DashboardRecords.Ranking ranking = records.ranking(1, "player12");
        assertEquals(9, ranking.leaders().size());
        assertEquals(1, ranking.leaders().get(0).rank());
        assertEquals(1, ranking.leaders().get(1).rank());
        assertEquals(3, ranking.leaders().get(2).rank());
        assertEquals(12, ranking.mine().orElseThrow().rank());
        assertEquals(120, ranking.mine().orElseThrow().seconds());
        assertEquals(1, records.ranking(2, "player12").mine().orElseThrow().rank());
        assertEquals(0, records.ranking(2, "player12").mine().orElseThrow().seconds());
        assertTrue(records.ranking(3, "player12").leaders().isEmpty());
        assertTrue(records.ranking(1, "unknown").mine().isEmpty());
        assertEquals(120L, records.progress("player12").get(0).seconds());
        assertEquals(0L, records.progress("player12").get(1).seconds());
        assertNull(records.progress("player12").get(2).seconds());
        assertEquals(3, records.rooms().size());
        assertEquals("01:01:01", DashboardRecords.time(3661));
    }

    @Test void databaseFailureIsNotPresentedAsAnEmptyLeaderboard() {
        DashboardRecords records = new DashboardRecords(new Database(directory.resolve("no-schema.db").toString()));
        assertThrows(StorageException.class, records::rooms);
        assertThrows(StorageException.class, () -> records.ranking(1, "test"));
        assertThrows(StorageException.class, () -> records.progress("test"));
    }
}
