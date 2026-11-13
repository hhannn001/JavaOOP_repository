package coderoom.entities.record;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;

/**
 * 담당: G
 * 역할: JDBC와 도메인 저장소 사이의 Adapter.
 * 구현 계약: coderoom.entities.record.ClearRecordRepository
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class JdbcClearRecordRepository implements ClearRecordRepository {
    // [연결]
    // coderoom.shared.db.Database를 생성자로 받는다.
    // 같은 패키지의 ClearRecordRepository 계약을 구현한다.

    private final Database database;

    // 생성자로 Database 전달받기
    public JdbcClearRecordRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    // [구현 예정]
    // clear_records를 사용자별로 조회한다.
    // (user_id, map_id) 충돌 시 더 작은 best_seconds만 유지하는 UPSERT를 구현한다.
    // 반복 저장이 행을 늘리거나 느린 기록으로 덮어쓰지 않도록 한다.
    // 계약의 각 메서드를 @Override로 구현한다. SQL은 PreparedStatement로 실행한다.
    // 호출마다 연결을 열고 Connection·Statement·ResultSet을 try-with-resources로 닫는다.
    // SQLException은 원인을 보존해 StorageException으로 변환한다.

    /** G의 JDBC 연결 전에는 조회가 성공한 것처럼 빈 목록을 반환하지 않는다. */
    @Override
    public List<ClearRecord> findByUserId(long userId) {
        String sql = "SELECT user_id, map_id, best_seconds "
                + "FROM clear_records "
                + "WHERE user_id = ? "
                + "ORDER BY map_id";

        List<ClearRecord> records = new ArrayList<>();

        try (Connection conn = database.open();
             PreparedStatement pstmt =
                     conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    ClearRecord record = new ClearRecord(
                            rs.getLong("user_id"),
                            rs.getLong("map_id"),
                            rs.getLong("best_seconds")
                    );

                    records.add(record);
                }
            }

            return records;

        } catch (SQLException e) {
            throw new StorageException(
                    "클리어 기록 조회 실패", e);
        }
    }
    

    /** G의 JDBC 연결 전에는 저장이 성공한 것처럼 처리하지 않는다. */
    @Override
    public void saveBest(ClearRecord record) {
    Objects.requireNonNull(record, "저장할 기록이 필요합니다.");

            String sql = "INSERT INTO clear_records "
                    + "(user_id, map_id, best_seconds) "
                    + "VALUES (?, ?, ?) "
                    + "ON CONFLICT(user_id, map_id) "
                    + "DO UPDATE SET best_seconds = "
                    + "MIN(clear_records.best_seconds, "
                    + "excluded.best_seconds)";

            try (Connection conn = database.open();
                PreparedStatement pstmt =
                        conn.prepareStatement(sql)) {

                pstmt.setLong(1, record.getUserId());
                pstmt.setLong(2, record.getMapId());
                pstmt.setLong(3, record.getBestSeconds());

                pstmt.executeUpdate();

            } catch (SQLException e) {
                throw new StorageException(
                        "클리어 기록 저장 실패", e);
            }
        }
}
