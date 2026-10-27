package coderoom.entities.record;

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

    // [구현 예정]
    // clear_records를 사용자별로 조회한다.
    // (user_id, map_id) 충돌 시 더 작은 best_seconds만 유지하는 UPSERT를 구현한다.
    // 반복 저장이 행을 늘리거나 느린 기록으로 덮어쓰지 않도록 한다.
    // 계약의 각 메서드를 @Override로 구현한다. SQL은 PreparedStatement로 실행한다.
    // 호출마다 연결을 열고 Connection·Statement·ResultSet을 try-with-resources로 닫는다.
    // SQLException은 원인을 보존해 StorageException으로 변환한다.
}
