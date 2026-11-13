package coderoom.entities.user;

import java.util.Objects;

/** 담당: B. 로그인 성공 후 앱과 화면에 전달하는 읽기 전용 사용자 정보. */
public record LoginUser(long userId, String loginId) {
    public LoginUser {
        if (userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }
        Objects.requireNonNull(loginId, "로그인 ID가 필요합니다.");
        if (loginId.isBlank()) {
            throw new IllegalArgumentException("로그인 ID는 비어 있을 수 없습니다.");
        }
    }
}
