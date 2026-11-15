package coderoom.entities.puzzle;

import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcPuzzleRepository implements PuzzleRepository {
    private final Database database;

    public JdbcPuzzleRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<Puzzle> findByMapId(long mapId) {
        String sql = "SELECT puzzle_id, map_id, step_no, code, answer_type, answer, hint "
                + "FROM puzzles WHERE map_id = ? ORDER BY step_no";
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, mapId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Puzzle> puzzles = new ArrayList<>();
                while (rows.next()) {
                    puzzles.add(new Puzzle(rows.getLong("puzzle_id"), rows.getLong("map_id"),
                            rows.getInt("step_no"), rows.getString("code"),
                            AnswerType.valueOf(rows.getString("answer_type")),
                            rows.getString("answer"), rows.getString("hint")));
                }
                return List.copyOf(puzzles);
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new StorageException("퍼즐 데이터를 불러올 수 없습니다.", exception);
        }
    }
}
