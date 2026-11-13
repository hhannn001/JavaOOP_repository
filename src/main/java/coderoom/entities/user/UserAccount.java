package coderoom.entities.user;

import java.util.Objects;

/**
 * 담당: B. 인증 내부에서만 사용하는 계정 정보.
 * passwordHash는 salt와 검증 설정을 포함한 저장 형식이며 화면에 전달하지 않는다.
 */
public record UserAccount(long userId, String loginId, String passwordHash) {
    public UserAccount {
        if (userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }
        Objects.requireNonNull(loginId, "로그인 ID가 필요합니다.");
        Objects.requireNonNull(passwordHash, "비밀번호 해시가 필요합니다.");
        if (loginId.isBlank() || passwordHash.isBlank()) {
            throw new IllegalArgumentException("로그인 ID와 비밀번호 해시는 비어 있을 수 없습니다.");
        }
    }

    /** record의 기본 문자열 표현에 해시가 포함되지 않도록 한다. */
    @Override
    public String toString() {
        return "UserAccount[userId=" + userId + ", loginId=" + loginId + ", passwordHash=<redacted>]";
    }
}
