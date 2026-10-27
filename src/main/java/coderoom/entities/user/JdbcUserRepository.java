package coderoom.entities.user;

/**
 * 담당: B
 * 역할: JDBC와 도메인 저장소 사이의 Adapter.
 * 구현 계약: coderoom.entities.user.UserRepository
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class JdbcUserRepository implements UserRepository {
    // [연결]
    // coderoom.shared.db.Database를 생성자로 받는다.
    // 같은 패키지의 UserRepository 계약을 구현한다.

    // [구현 예정]
    // users를 login_id로 조회하고 password_hash를 포함한 UserAccount로 변환한다.
    // 계정이 없을 때만 Optional.empty를 반환한다.
    // 계약의 각 메서드를 @Override로 구현한다. SQL은 PreparedStatement로 실행한다.
    // 호출마다 연결을 열고 Connection·Statement·ResultSet을 try-with-resources로 닫는다.
    // SQLException은 원인을 보존해 StorageException으로 변환한다.
}
