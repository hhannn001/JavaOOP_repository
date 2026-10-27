package coderoom.shared.db;

/**
 * 담당: G
 * 역할: SQLite 연결과 초기화.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class Database {
    // [필드·연결]
    // 로컬 DB 파일 경로·접속 설정. 업무 엔티티 타입은 참조하지 않는다.

    // [구현 예정]
    // open(): 새 java.sql.Connection을 반환하고 매번 외래키 제약을 활성화한다.
    // initialize(): resources/db/schema.sql과 seed.sql을 이용해 테이블·시연 데이터를 준비한다.
    // 초기화는 반복 실행해도 기존 기록을 삭제하거나 시연 데이터를 중복 삽입하지 않아야 한다.
    // 연결을 전역으로 공유하지 않는다. SQLite JDBC 드라이버는 실제 구현 시 추가한다.
}
