package coderoom.features.auth;

import coderoom.entities.user.LoginUser;
import java.util.Objects;

/**
 * 담당: B. 비밀번호와 해시를 포함하지 않는 읽기 전용 로그인 결과.
 * 성공 시에만 user가 존재하며, 실패 시 user는 null이다.
 */
public record LoginResult(boolean success, String message, LoginUser user) {
    public LoginResult {
        Objects.requireNonNull(message, "안내 메시지가 필요합니다.");
        if (message.isBlank()) {
            throw new IllegalArgumentException("안내 메시지는 비어 있을 수 없습니다.");
        }
        if (success != (user != null)) {
            throw new IllegalArgumentException("성공한 로그인 결과에만 사용자가 있어야 합니다.");
        }
    }

    public static LoginResult success(LoginUser user) {
        return new LoginResult(true, "로그인되었습니다.", Objects.requireNonNull(user, "사용자가 필요합니다."));
    }

    public static LoginResult failure(String message) {
        return new LoginResult(false, message, null);
    }
}
