package coderoom.features.dashboard;

import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 대시보드의 읽기 전용 DB 조회. 시연 계정의 숫자 ID를 DB 사용자 ID로 가정하지 않는다. */
public final class DashboardRecords {
    public record Room(long id, String title) {
        @Override public String toString() { return title; }
    }
    public record Entry(long rank, String loginId, long seconds) { }
    public record Ranking(List<Entry> leaders, Optional<Entry> mine) { }
    public record Progress(String room, Long seconds) { }
    public record RoomProgress(String room, Long seconds, List<Problem> problems) {
        public int solvedCount() {
            return (int) problems.stream().filter(problem -> problem.solution().isPresent()).count();
        }
    }
    public record Problem(int number, Optional<Solved> solution) { }
    public record Solved(String room, int stepNo, String code, String answer, long puzzleId) { }

    private final Database database;

    public DashboardRecords(Database database) {
        this.database = java.util.Objects.requireNonNull(database);
    }

    public List<Room> rooms() {
        List<Room> rooms = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement query = connection.prepareStatement("""
                        SELECT map_id, title FROM maps WHERE archived = 0
                        ORDER BY CASE WHEN map_id = 4 THEN 2 ELSE map_id END, map_id
                        """);
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                rooms.add(new Room(rows.getLong(1), rows.getString(2)));
            }
            return List.copyOf(rooms);
        } catch (SQLException exception) {
            throw new StorageException("방 목록을 읽을 수 없습니다.", exception);
        }
    }

    /** 동률은 같은 순위(1, 1, 3), 표시 인원은 최대 9명. 내 기록은 전체 순위에서 조회한다. */
    public Ranking ranking(long roomId, String loginId) {
        String sql = "WITH ranked AS (SELECT u.login_id, r.best_seconds, "
                + "RANK() OVER (ORDER BY r.best_seconds) AS place, "
                + "ROW_NUMBER() OVER (ORDER BY r.best_seconds, u.login_id, u.user_id) AS row_no "
                + "FROM clear_records r JOIN users u ON u.user_id = r.user_id JOIN maps m ON m.map_id = r.map_id "
                + "WHERE r.map_id = ? AND m.archived = 0) "
                + "SELECT login_id, best_seconds, place, row_no FROM ranked "
                + "WHERE row_no <= 9 OR login_id = ? ORDER BY row_no";
        List<Entry> leaders  = new ArrayList<>();
        Optional<Entry> mine = Optional.empty();
        try (Connection connection = database.open();
                PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, roomId);
            query.setString(2, loginId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    Entry entry = new Entry(rows.getLong(3), rows.getString(1), rows.getLong(2));
                    if (rows.getLong(4) <= 9) {
                        leaders.add(entry);
                    }
                    if (loginId.equals(entry.loginId())) {
                        mine = Optional.of(entry);
                    }
                }
            }
            return new Ranking(List.copyOf(leaders), mine);
        } catch (SQLException exception) {
            throw new StorageException("리더보드 기록을 읽을 수 없습니다.", exception);
        }
    }

    public List<Progress> progress(String loginId) {
        String sql = "SELECT m.title, r.best_seconds FROM maps m LEFT JOIN clear_records r "
                + "ON r.map_id = m.map_id AND r.user_id = (SELECT user_id FROM users WHERE login_id = ?) "
                + "WHERE m.archived = 0 ORDER BY CASE WHEN m.map_id = 4 THEN 2 ELSE m.map_id END, m.map_id";
        List<Progress> result = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, loginId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    long seconds    = rows.getLong(2);
                    boolean missing = rows.wasNull();
                    result.add(new Progress(rows.getString(1), missing ? null : seconds));
                }
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw new StorageException("진척도를 읽을 수 없습니다.", exception);
        }
    }

    public List<RoomProgress> roomProgress(String loginId) {
        String sql = """
                SELECT m.map_id, m.title, r.best_seconds, p.step_no, s.code, s.answer, p.puzzle_id
                FROM maps m
                LEFT JOIN users u ON u.login_id = ?
                LEFT JOIN clear_records r ON r.map_id = m.map_id AND r.user_id = u.user_id
                LEFT JOIN puzzles p ON p.map_id = m.map_id
                LEFT JOIN solved_puzzles s ON s.map_id = m.map_id AND s.user_id = u.user_id
                    AND s.puzzle_id = p.puzzle_id
                WHERE m.archived = 0
                ORDER BY CASE WHEN m.map_id = 4 THEN 2 ELSE m.map_id END, m.map_id, p.step_no
                """;
        var rooms = new java.util.LinkedHashMap<Long, RoomProgress>();
        try (Connection connection = database.open();
                PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, loginId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    long id     = rows.getLong(1);
                    String name = rows.getString(2);
                    Long best   = rows.getObject(3) == null ? null : rows.getLong(3);
                    int step    = rows.getInt(4);
                    boolean hasProblem = !rows.wasNull();
                    RoomProgress room = rooms.computeIfAbsent(id,
                            key -> new RoomProgress(name, best, new ArrayList<>()));
                    if (hasProblem) {
                        String code   = rows.getString(5);
                        String answer = rows.getString(6);
                        Solved solved = code == null ? null : new Solved(name, step, code, answer, rows.getLong(7));
                        room.problems().add(new Problem(step, Optional.ofNullable(solved)));
                    }
                }
            }
            return rooms.values().stream()
                    .map(room -> new RoomProgress(room.room(), room.seconds(), List.copyOf(room.problems()))).toList();
        } catch (SQLException exception) {
            throw new StorageException("방별 문제 기록을 읽을 수 없습니다.", exception);
        }
    }

    public List<Solved> solvedProblems(String loginId) {
        String sql = "SELECT m.title, s.step_no, s.code, s.answer, s.puzzle_id FROM solved_puzzles s "
                + "JOIN users u ON u.user_id = s.user_id JOIN maps m ON m.map_id = s.map_id "
                + "WHERE u.login_id = ? ORDER BY s.map_id, s.step_no, s.puzzle_id";
        List<Solved> result = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, loginId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    result.add(new Solved(rows.getString(1), rows.getInt(2), rows.getString(3), rows.getString(4),
                            rows.getLong(5)));
                }
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw new StorageException("문제 풀이 기록을 읽을 수 없습니다.", exception);
        }
    }

    public static String time(long seconds) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
