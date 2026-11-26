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
import coderoom.features.gameplay.ItemView;
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
    void wrongAnswersShareFiveAttemptsAcrossPuzzlesAndRestartRestoresThem() {
        GameFacade game = new GameFacade(maps, puzzles, records);
        GameSetup setup = game.loadMap(1);
        long started    = System.nanoTime();
        assertEquals(5, game.start(4, setup, started).attemptsRemaining());
        assertEquals(5, game.submit(RoomObjectId.WALL_DIAL, "000000000").view().attemptsRemaining());
        assertEquals(5, game.submit(RoomObjectId.DRAWER, "12a4").view().attemptsRemaining());
        assertEquals(4, game.submit(RoomObjectId.DRAWER, "0000").view().attemptsRemaining());
        assertEquals(4, game.submit(RoomObjectId.DRAWER, "2312").view().attemptsRemaining());
        assertEquals(4, game.submit(RoomObjectId.DRAWER, "0000").view().attemptsRemaining());
        for (int remaining = 3; remaining >= 0; remaining--) {
            var result = game.submit(RoomObjectId.WALL_DIAL, "111111111");
            assertEquals(Verdict.WRONG, result.verdict());
            assertEquals(remaining, result.view().attemptsRemaining());
        }
        assertTrue(game.view(started).attemptsExhausted());
        assertEquals(Verdict.BLOCKED, game.submit(RoomObjectId.WALL_DIAL, "423312133").verdict());
        assertNull(game.useKey(started).record());
        assertTrue(records.findByUserId(4).isEmpty());
        assertEquals(5, game.start(4, setup, started).attemptsRemaining());
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
    void newAccountCanSignInAndDuplicateRegistrationKeepsItsPassword() {
        var login = new LoginService(users, new PasswordVerifier());
        char[] secret  = "archive-password".toCharArray();
        char[] confirm = secret.clone();
        assertTrue(login.register("reader", secret, confirm).success());
        assertEquals('\0', secret[0]);
        assertEquals('\0', confirm[0]);
        assertEquals("이미 사용 중인 ID입니다.",
                login.register("reader", "replacement".toCharArray(), "replacement".toCharArray()).message());
        assertTrue(login.login("reader", "archive-password".toCharArray()).success());
        assertFalse(login.login("reader", "replacement".toCharArray()).success());
    }

    @Test
    void registrationRejectsInvalidInputsAndClearsPasswordArrays() {
        var login = new LoginService(users, new PasswordVerifier());
        assertFalse(login.register("a!", "valid-password".toCharArray(), "valid-password".toCharArray()).success());
        assertFalse(login.register("reader", "short".toCharArray(), "short".toCharArray()).success());
        char[] secret  = "valid-password".toCharArray();
        char[] confirm = "different-password".toCharArray();
        assertFalse(login.register("reader", secret, confirm).success());
        assertEquals('\0', secret[0]);
        assertEquals('\0', confirm[0]);
        assertTrue(users.findByLoginId("reader").isEmpty());
    }

    @Test
    void alternateSetsChangeCluesAndAcceptOnlyTheirMatchingAnswers() {
        var variants = coderoom.features.gameplay.PuzzleVariant.values();
        String[][][] solutions = {
            {{"2312", "423312133"}, {"ACD", "CB", "CAE", "DCAB", "ACBD"}, {"BC", "ABC", "AB", "BC", "BAAB"}},
            {{"3241", "241134423"}, {"D", "AA", "EDA", "BDAC", "ACBD"}, {"AD", "BBC", "AB", "CA", "AAAB"}},
            {{"1425", "324413241"}, {"ACDE", "BC", "AED", "CADB", "ACBD"}, {"BE", "ABB", "AB", "DC", "BBAB"}}
        };
        long[] mapIds = {1, 3, 4};
        RoomObjectId[] devices = {RoomObjectId.DRAWER, RoomObjectId.WALL_DIAL,
            RoomObjectId.THIRD_DEVICE, RoomObjectId.FOURTH_DEVICE, RoomObjectId.FIFTH_DEVICE};
        long started = System.nanoTime();
        for (int deck = 0; deck < variants.length; deck++) {
            var variant = variants[deck];
            var game = new GameFacade(maps, puzzles, records, ignored -> variant);
            for (int map = 0; map < mapIds.length; map++) {
                var setup = game.loadMap(mapIds[map], 4);
                game.start(4, setup, started);
                for (int step = 0; step < setup.puzzles().size(); step++) {
                    String expected = solutions[deck][map][step];
                    String standard = solutions[0][map][step];
                    ItemView clue = mapIds[map] == 1 ? game.view(started).items().get(step)
                            : game.inspect(devices[step]).instructions();
                    assertEquals(ItemView.Diagram.forPuzzle(setup.puzzles().get(step).puzzleId()), clue.diagram());
                    boolean hasDiagram = (map == 0 && step == 1) || (map == 1 && (step == 1 || step == 2 || step == 4))
                            || (map == 2 && (step == 0 || step == 3));
                    assertEquals(hasDiagram, clue.diagram() != ItemView.Diagram.NONE);
                    if (!expected.equals(standard)) { assertEquals(variant, clue.variant()); }
                    if (mapIds[map] == 3 && step == 0) {
                        assertTrue(clue.content().contains("m.voltage"));
                        assertFalse(clue.content().contains("모듈하나"));
                    }
                    if (mapIds[map] == 4 && step == 4) {
                        assertTrue(clue.content().contains("낮은 선로에 물이 차 있으면 높은 선로로 간다."));
                        assertTrue(clue.content().contains("본선 앞에 정지턱이 있으면 우회선로로 간다."));
                        assertTrue(clue.content().contains("if (본선정지턱있음) 우회선로()"));
                        assertFalse(clue.content().contains("bufferAhead"));
                    }
                    if (mapIds[map] == 3 && step == 4) {
                        assertTrue(clue.content().contains("boolean running = powerOk && loadOk"), variant.toString());
                        assertTrue(clue.content().contains("boolean safe = pressureOk && sealed"), variant.toString());
                    }
                    if (!expected.equals(standard)) {
                        assertEquals(variant, game.inspect(devices[step]).variant(), "map " + mapIds[map] + ", step " + step);
                        var wrong = game.submit(devices[step], standard);
                        assertEquals(Verdict.WRONG, wrong.verdict());
                        if (wrong.view().attemptsExhausted()) {
                            game.start(4, setup, started);
                            for (int prior = 0; prior < step; prior++) {
                                assertEquals(Verdict.SOLVED, game.submit(devices[prior], solutions[deck][map][prior]).verdict());
                            }
                        }
                    }
                    assertEquals(Verdict.SOLVED, game.submit(devices[step], expected).verdict(),
                            variant + " map " + mapIds[map] + ", step " + step);
                }
                if (mapIds[map] == 1) { game.selectItem(coderoom.entities.game.ItemId.KEY); }
                var result = game.useKey(started + 12_000_000_000L);
                assertNotNull(result.record());
            }
        }

        var deck = coderoom.features.gameplay.PuzzleVariant.rotatingDeck();
        for (long mapId : mapIds) {
            var last = deck.apply(mapId);
            for (int run = 0; run < 12; run++) {
                var next = deck.apply(mapId);
                assertTrue(last != next);
                last = next;
            }
        }
    }

    @Test
    void newRoomsHaveIndependentProgressAndPersistTheirOwnRecords() {
        GameFacade game = new GameFacade(maps, puzzles, records);
        long started    = System.nanoTime();
        GameSetup engine = game.loadMap(3, 4);
        var initial = game.start(4, engine, started);
        assertEquals(5, initial.puzzleCount());
        assertNull(initial.previousBest());
        assertTrue(initial.items().isEmpty());
        assertNull(game.inspect(RoomObjectId.WALL_DIAL).answerType());
        game.inspect(RoomObjectId.EVIDENCE_3);
        assertTrue(game.view(started).items().get(0).hint().isBlank());
        game.inspect(RoomObjectId.EVIDENCE_2);
        game.inspect(RoomObjectId.EVIDENCE_4);
        assertEquals(1, game.view(started).items().size());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.DRAWER, "ACD").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.THIRD_DEVICE, "CAE").verdict());
        assertEquals(java.util.Set.of(0, 2), game.view(started).solvedSteps());
        assertEquals(Verdict.BLOCKED, game.submit(RoomObjectId.FOURTH_DEVICE, "DCAB").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.WALL_DIAL, "CB").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.FOURTH_DEVICE, "DCAB").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.FIFTH_DEVICE, "ACBD").verdict());
        game.saveClear(game.useKey(started + 30_000_000_000L).record());
        assertEquals(30, game.loadMap(3, 4).previousBest());
        assertNull(game.loadMap(3, 1).previousBest());

        var greenhouse = game.start(4, game.loadMap(4, 4), started);
        assertEquals(5, greenhouse.puzzleCount());
        assertTrue(greenhouse.evidence().isEmpty());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.WALL_DIAL, "ABC").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.THIRD_DEVICE, "AB").verdict());
        assertEquals(Verdict.BLOCKED, game.submit(RoomObjectId.FOURTH_DEVICE, "BC").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.DRAWER, "BC").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.FOURTH_DEVICE, "BC").verdict());
        assertEquals(Verdict.SOLVED, game.submit(RoomObjectId.FIFTH_DEVICE, "BAAB").verdict());
        assertNull(game.useKey(started + 600_000_000_000L).record());
        game.saveClear(game.useKey(started + 59_000_000_000L).record());
        assertEquals(59, game.loadMap(4, 4).previousBest());

        assertThrows(IllegalArgumentException.class, () -> game.loadMap(2, 4));
    }

    @Test
    void replacesArchiveSlotAndPreservesLegacyRecordsOnRepeatedInitialization() throws Exception {
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE maps DROP COLUMN archived");
            statement.executeUpdate("""
                    INSERT INTO maps (map_id, title, description, topic, difficulty, poster_path, playable)
                    VALUES (2, '사라진 기록', 'legacy', 'history', 'easy', '/legacy.png', 1)
                    """);
            statement.executeUpdate("UPDATE maps SET poster_path='/images/room/workshop/greenhouse/poster.jpg' WHERE map_id=4");
            statement.executeUpdate("UPDATE puzzles SET answer='AB', code='old balance' WHERE puzzle_id=404");
            statement.executeUpdate("UPDATE puzzles SET code='낮은 왼쪽 화분으로', answer='BBC' WHERE puzzle_id=402");
            statement.executeUpdate("UPDATE puzzles SET code='old platforms' WHERE puzzle_id=403");
            statement.executeUpdate("UPDATE puzzles SET code='old specimen', answer='C' WHERE puzzle_id=401");
            statement.executeUpdate("UPDATE puzzles SET code='old rail', answer='BA' WHERE puzzle_id=405");
            statement.executeUpdate("UPDATE puzzles SET code='System.out.print(digit + 1)' WHERE puzzle_id=1");
            statement.executeUpdate("UPDATE puzzles SET code='예전 점검표' WHERE puzzle_id=315");
            statement.executeUpdate("INSERT INTO clear_records VALUES (4, 2, 37)");
            statement.executeUpdate("INSERT INTO solved_puzzles VALUES (4, 2, 201, 1, 'legacy code', 'old answer')");
        }
        database.initialize();
        database.initialize();

        assertEquals(List.of(1, 4, 3), maps.findAll().stream().map(map -> map.getMapId()).toList());
        assertTrue(maps.findById(2).isEmpty());
        var greenhouse = maps.findById(4).orElseThrow();
        assertEquals("/images/posters/room02_last_seed.png", greenhouse.getPosterPath());
        var poster = javax.imageio.ImageIO.read(getClass().getResource(greenhouse.getPosterPath()));
        assertTrue(poster.getHeight() > poster.getWidth());
        assertEquals(37, records.findByUserId(4).stream().filter(record -> record.getMapId() == 2)
                .findFirst().orElseThrow().getBestSeconds());
        DashboardRecords dashboard = new DashboardRecords(database);
        assertEquals(List.of(1L, 4L, 3L), dashboard.rooms().stream().map(DashboardRecords.Room::id).toList());
        assertTrue(dashboard.ranking(2, "test").leaders().isEmpty());
        assertEquals("legacy code", dashboard.solvedProblems("test").get(0).code());
        assertNull(new GameFacade(maps, puzzles, records).loadMap(4, 4).previousBest());
        assertEquals(AnswerCheck.CORRECT, puzzles.findByMapId(4).get(3).check("BC"));
        assertTrue(puzzles.findByMapId(4).get(1).code().contains("유입관과 배수구 높이"));
        assertTrue(puzzles.findByMapId(1).get(0).code().contains("total += step"));
        assertTrue(puzzles.findByMapId(1).get(1).code().contains("grid[col][row]"));
        assertEquals(AnswerCheck.CORRECT, puzzles.findByMapId(4).get(0).check("BC"));
        assertEquals(AnswerCheck.CORRECT, puzzles.findByMapId(4).get(1).check("ABC"));
        var platforms = puzzles.findByMapId(4).get(2);
        assertTrue(platforms.code().contains("황동 걸쇠"));
        assertEquals(AnswerCheck.CORRECT, platforms.check("AB"));
        assertEquals(AnswerCheck.WRONG, platforms.check("ABBB"));
        assertEquals(AnswerCheck.CORRECT, puzzles.findByMapId(4).get(4).check("BAAB"));
        Puzzle interlock = puzzles.findByMapId(3).get(4);
        assertTrue(interlock.code().contains("boolean running = powerOk && loadOk;"));
        assertTrue(interlock.code().contains("boolean safe = pressureOk && sealed;"));
        assertEquals(AnswerCheck.CORRECT, interlock.check("ACBD"));
    }

    @Test
    void missingOrInvalidDataFailsExplicitlyAndConstraintsAreEnabled() throws Exception {
        GameFacade game = new GameFacade(maps, puzzles, records);
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
