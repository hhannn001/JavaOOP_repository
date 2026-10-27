package coderoom.entities.puzzle;

/**
 * 담당: G
 * 역할: JDBC와 도메인 저장소 사이의 Adapter.
 * 구현 계약: coderoom.entities.puzzle.PuzzleRepository
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class JdbcPuzzleRepository implements PuzzleRepository {
    // [연결]
    // coderoom.shared.db.Database를 생성자로 받는다.
    // 같은 패키지의 PuzzleRepository 계약을 구현한다.

    // [구현 예정]
    // puzzles를 map_id로 조회하고 step_no 순서로 반환한다.
    // answer_type을 AnswerType으로 변환한다. 잘못된 형식은 숨기지 않는다.
    // 계약의 각 메서드를 @Override로 구현한다. SQL은 PreparedStatement로 실행한다.
    // 호출마다 연결을 열고 Connection·Statement·ResultSet을 try-with-resources로 닫는다.
    // SQLException은 원인을 보존해 StorageException으로 변환한다.
}
