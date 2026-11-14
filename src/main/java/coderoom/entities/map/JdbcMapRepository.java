package coderoom.entities.map;

/**
 * 담당: C
 * 역할: JDBC와 도메인 저장소 사이의 Adapter.
 * 구현 계약: coderoom.entities.map.MapRepository
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */

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

	// [연결]
    // coderoom.shared.db.Database를 생성자로 받는다.
    // 같은 패키지의 MapRepository 계약을 구현한다.

    // [구현 예정]
    // maps의 모든 포스터 메타데이터와 개별 맵을 조회한다.
    // playable 정수값을 도메인 값으로 변환한다.
    // 계약의 각 메서드를 @Override로 구현한다. SQL은 PreparedStatement로 실행한다.
    // 호출마다 연결을 열고 Connection·Statement·ResultSet을 try-with-resources로 닫는다.
    // SQLException은 원인을 보존해 StorageException으로 변환한다.
	
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
