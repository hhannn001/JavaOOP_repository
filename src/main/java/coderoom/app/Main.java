package coderoom.app;

/**
 * 담당: A
 * 역할: 실행 진입점과 객체 조립.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class Main {
    // [연결]
    // coderoom.shared.db.Database와 각 entities의 Jdbc...Repository를 생성한다.
    // Repository를 features의 서비스 생성자에 전달하고 AppController와 MainFrame을 연결한다.
    // 구체 JDBC 구현은 이 조립 지점에서만 직접 생성한다.

    // [구현 예정]
    // main(String[] args): DB 초기화 완료 후 SwingUtilities.invokeLater로 화면을 생성한다.
    // 초기화 실패 시 오류를 알리고 정상 실행으로 진행하지 않는다.
}
