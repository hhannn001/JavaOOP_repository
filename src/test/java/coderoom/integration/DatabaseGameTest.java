package coderoom.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coderoom.entities.game.GameStage;
import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.map.JdbcMapRepository;
import coderoom.entities.puzzle.AnswerCheck;
import coderoom.entities.puzzle.AnswerType;
import coderoom.entities.puzzle.JdbcPuzzleRepository;
import coderoom.entities.puzzle.Puzzle;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.record.SolvedPuzzle;
import coderoom.features.dashboard.DashboardRecords;
import coderoom.entities.record.JdbcClearRecordRepository;
import coderoom.entities.user.JdbcUserRepository;
import coderoom.features.auth.LoginService;
import coderoom.features.auth.PasswordVerifier;
import coderoom.features.gameplay.GameFacade;
import coderoom.features.gameplay.GameSetup;
import coderoom.features.gameplay.SubmitResult.Verdict;
import coderoom.features.mapcatalog.MapCatalogService;
import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseGameTest {
    @TempDir Path directory;
    private Database database;
    private JdbcUserRepository users;
    private JdbcMapRepository maps;
    private JdbcPuzzleRepository puzzles;
    private JdbcClearRecordRepository records;

    @BeforeEach
    void initialize() {
        database = new Database(directory.resolve("game.db").toString());
        database.initialize();
        users   = new JdbcUserRepository(database);
        maps    = new JdbcMapRepository(database);
        puzzles = new JdbcPuzzleRepository(database);
        records = new JdbcClearRecordRepository(database);
    }

    @Test
    void seedsAuthenticateAndRepeatedInitializationPreservesExistingState() throws Exception {
        LoginService login = new LoginService(users, new PasswordVerifier());
        assertTrue(login.login("test", "test1234".toCharArray()).success());
        for (String name : List.of("demo1", "demo2", "demo3")) {
            assertTrue(login.login(name, "test-only".toCharArray()).success());
        }
        assertFalse(login.login("test", "incorrect".toCharArray()).success());
        assertTrue(users.findByLoginId("' OR 1=1 --").isEmpty());
        String hash = users.findByLoginId("test").orElseThrow().passwordHash();
        assertFalse(hash.contains("test1234"));
        records.saveBest(new ClearRecord(4, 1, 90));
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE puzzles SET hint='retained hint' WHERE puzzle_id=1");
        }
        database.initialize();
        assertEquals(hash, users.findByLoginId("test").orElseThrow().passwordHash());
        assertEquals(3, maps.findAll().size());
        assertEquals(2, puzzles.findByMapId(1).size());
        assertEquals("retained hint", puzzles.findByMapId(1).get(0).hint());
        assertEquals(90, records.findByUserId(4).get(0).getBestSeconds());
        try (var connection = database.open(); var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            assertTrue(rows.next());
            assertEquals(4, rows.getInt(1));
        }
    }

    @Test
    void actualJdbcGameFlowPersistsOnlyTheBestRecordAcrossReopen() {
        GameFacade game = new GameFacade(maps, puzzles, records);
        GameSetup setup = game.loadMap(1);
        long started = System.nanoTime();
        assertEquals(List.of(ItemId.NOTE1), game.start(4, setup, started).items().stream().map(item -> item.id()).toList());
        assertThrows(IllegalArgumentException.class, () -> game.selectItem(ItemId.KEY));
        assertNull(game.useKey(started).record());
        assertEquals(Verdict.BLOCKED, game.submit(RoomObjectId.WALL_DIAL, "423312133").verdict());
        assertEquals(Verdict.INVALID, game.submit(RoomObjectId.DRAWER, "12a4").verdict());
        assertEquals(Verdict.WRONG, game.submit(RoomObjectId.DRAWER, "0000").verdict());
        assertEquals(GameStage.START, game.view(started).stage());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.DRAWER, "2312").verdict());
        assertEquals(Verdict.ALREADY_SOLVED, game.submit(RoomObjectId.DRAWER, "2312").verdict());
        assertEquals(2, game.view(started).items().size());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.WALL_DIAL, "423312133").verdict());
        assertNull(game.useKey(started).record());
        assertTrue(new DashboardRecords(database).solvedProblems("test").isEmpty());
        game.selectItem(ItemId.KEY);
        var escaped = game.useKey(started + 42_000_000_000L);
        assertNotNull(escaped.record());
        assertEquals(GameStage.ESCAPED, escaped.view().stage());
        assertEquals(42, escaped.record().getBestSeconds());
        assertEquals(42, game.useKey(started + 99_000_000_000L).record().getBestSeconds());
        assertEquals(Verdict.BLOCKED, game.submit(RoomObjectId.DRAWER, "2312").verdict());
        assertThrows(UnsupportedOperationException.class, () -> escaped.view().items().clear());
        assertEquals(2, escaped.record().getSolvedPuzzles().size());
        assertThrows(UnsupportedOperationException.class, () -> escaped.record().getSolvedPuzzles().clear());
        game.start(1, setup, started);
        game.saveClear(escaped.record());
        game.saveClear(escaped.record());
        records.saveBest(new ClearRecord(4, 1, 70));
        records.saveBest(new ClearRecord(4, 1, 30));
        Database reopened = new Database(directory.resolve("game.db").toString());
        reopened.initialize();
        var persisted = new JdbcClearRecordRepository(reopened);
        assertEquals(1, persisted.findByUserId(4).size());
        assertEquals(30, persisted.findByUserId(4).get(0).getBestSeconds());
        assertTrue(persisted.findByUserId(1).isEmpty());
        var solved = new DashboardRecords(reopened).solvedProblems("test");
        assertEquals(2, solved.size());
        assertEquals(setup.puzzles().get(0).code(), solved.get(0).code());
        assertEquals("2312", solved.get(0).answer());
        assertEquals("423312133", solved.get(1).answer());
        assertTrue(new DashboardRecords(reopened).solvedProblems("demo1").isEmpty());
        assertEquals(3, new MapCatalogService(maps, persisted).listForUser(4).size());
        assertEquals(GameStage.START, game.start(4, setup, started).stage());
    }

    @Test
    void snapshotsSurvivePuzzleChangesAndFailedWritesRollbackEverything() throws Exception {
        ClearRecord clear = new ClearRecord(4, 1, 42,
                List.of(new SolvedPuzzle(1, 1, "original code", "0012")));
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER fail_snapshot BEFORE INSERT ON solved_puzzles "
                    + "BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        assertThrows(StorageException.class, () -> records.saveBest(clear));
        assertTrue(records.findByUserId(4).isEmpty());
        assertTrue(new DashboardRecords(database).solvedProblems("test").isEmpty());
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER fail_snapshot");
        }
        records.saveBest(clear);
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE puzzles SET code='changed', answer='9999' WHERE puzzle_id=1");
        }
        records.saveBest(new ClearRecord(4, 1, 30,
                List.of(new SolvedPuzzle(1, 1, "changed", "9999"))));
        database.initialize();
        var solved = new DashboardRecords(database).solvedProblems("test");
        assertEquals(1, solved.size());
        assertEquals("original code", solved.get(0).code());
        assertEquals("0012", solved.get(0).answer());
        assertEquals(30, records.findByUserId(4).get(0).getBestSeconds());
    }

    @Test
    void missingOrInvalidDataFailsExplicitlyAndConstraintsAreEnabled() throws Exception {
        GameFacade game = new GameFacade(maps, puzzles, records);
        assertThrows(IllegalArgumentException.class, () -> game.loadMap(2));
        assertThrows(IllegalArgumentException.class, () -> game.loadMap(999));
        assertThrows(IllegalArgumentException.class, () -> new ClearRecord(4, 1, -1));
        assertThrows(StorageException.class, () -> records.saveBest(new ClearRecord(999, 1, 10)));
        try (var connection = database.open(); var statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO clear_records VALUES (4,1,-1)"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE maps SET playable=2 WHERE map_id=1"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("UPDATE puzzles SET answer_type='BAD'"));
            statement.executeUpdate("UPDATE puzzles SET answer='bad' WHERE puzzle_id=1");
        }
        assertThrows(StorageException.class, () -> puzzles.findByMapId(1));
        Database empty = new Database(directory.resolve("empty.db").toString());
        assertThrows(StorageException.class, () -> new JdbcUserRepository(empty).findByLoginId("test"));
        assertThrows(StorageException.class, () -> new JdbcMapRepository(empty).findAll());
        assertThrows(StorageException.class, () -> new JdbcClearRecordRepository(empty).findByUserId(4));
    }

    @Test
    void puzzleValidationPreservesLeadingZeroAndDoesNotExecuteCode() {
        Puzzle puzzle = new Puzzle(1, 1, 1, "not executable Java", AnswerType.DIGITS4, "0012", "hint");
        assertEquals(AnswerCheck.CORRECT, puzzle.check("0012"));
        assertEquals(AnswerCheck.INVALID, puzzle.check("12"));
        assertEquals(AnswerCheck.INVALID, puzzle.check(" 0012"));
        assertEquals(AnswerCheck.INVALID, puzzle.check(null));
        assertEquals(AnswerCheck.WRONG, puzzle.check("0013"));
    }
}
