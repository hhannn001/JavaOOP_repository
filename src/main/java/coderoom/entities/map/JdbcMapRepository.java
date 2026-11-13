
package coderoom.entities.map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;

public class JdbcMapRepository implements MapRepository {

    private final Database database;

    // Database 연결 객체를 전달받는 생성자
    public JdbcMapRepository(Database database) {
        this.database = database;
    }

    // 모든 맵 조회
    @Override
    public List<GameMap> findAll() {

        String sql = """
                SELECT map_id, title, description, topic,
                       difficulty, poster_path, playable
                FROM maps
                ORDER BY map_id
                """;

        List<GameMap> maps = new ArrayList<>();

        try (Connection connection = database.open();
             PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                maps.add(mapRow(rs));
            }

            return maps;

        } catch (SQLException e) {
            throw new StorageException("맵 목록 조회 실패", e);
        }
    }

    // 맵 ID로 특정 맵 조회
    @Override
    public Optional<GameMap> findById(int mapId) {

        String sql = """
                SELECT map_id, title, description, topic,
                       difficulty, poster_path, playable
                FROM maps
                WHERE map_id = ?
                """;

        try (Connection connection = database.open();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, mapId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new StorageException("맵 조회 실패", e);
        }
    }

    // DB 조회 결과를 GameMap 객체로 변환
    private GameMap mapRow(ResultSet rs) throws SQLException {

        return new GameMap(
                rs.getInt("map_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("topic"),
                rs.getString("difficulty"),
                rs.getString("poster_path"),
                rs.getInt("playable") == 1
        );
    }
}
