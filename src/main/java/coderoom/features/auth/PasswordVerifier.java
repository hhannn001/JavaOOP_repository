package coderoom.features.auth;

/**
 * 담당: B
 * 역할: 비밀번호 해시 생성과 검증.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class PasswordVerifier {
    // [구현 예정]
    // JDK의 비밀번호 기반 키 도출 기능과 SecureRandom을 사용한다. 알고리즘을 직접 만들지 않는다.
    // 사용자별 salt·검증 설정·해시를 하나의 저장 형식으로 합의한다.
    // verify(char[] password, String storedHash): 저장 형식에 맞춰 검증한다.
    // 시연 계정 초기화에 사용할 해시 생성 기능도 같은 형식을 사용한다.
    // 평문 비밀번호·해시를 로그나 화면에 출력하지 않는다.
}
