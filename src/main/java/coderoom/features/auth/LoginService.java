package coderoom.features.auth;

/**
 * 담당: B
 * 역할: 사용자 조회와 비밀번호 검증 조율.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class LoginService {
    // [연결]
    // coderoom.entities.user.UserRepository와 같은 패키지의 PasswordVerifier를 생성자로 받는다.

    // [구현 예정]
    // login(loginId, char[] password): LoginResult를 반환한다.
    // 계정 조회 후 해시를 검증하고 성공 시 해시가 없는 LoginUser를 만든다.
    // 없는 계정·틀린 비밀번호는 로그인 실패로, DB 장애는 StorageException으로 구분한다.
}
