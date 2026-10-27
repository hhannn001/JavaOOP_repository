package coderoom.entities.map;

/**
 * 담당: C
 * 역할: JDBC와 도메인 저장소 사이의 Adapter.
 * 구현 계약: coderoom.entities.map.MapRepository
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
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
}
