package coderoom.features.auth;

import coderoom.entities.user.LoginUser;
import coderoom.entities.user.UserAccount;
import coderoom.entities.user.UserRepository;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** 담당: B. 사용자 조회와 비밀번호 검증을 조율하며 Swing과 SQL에 의존하지 않는다. */
public final class LoginService {
    private static final String INVALID_LOGIN = "ID 또는 비밀번호를 확인하세요.";
    private final UserRepository users;
    private final PasswordVerifier verifier;

    public LoginService(UserRepository users, PasswordVerifier verifier) {
        this.users = Objects.requireNonNull(users, "사용자 저장소가 필요합니다.");
        this.verifier = Objects.requireNonNull(verifier, "비밀번호 검증기가 필요합니다.");
    }

    /**
     * ID는 입력 그대로 조회하며 ID와 비밀번호를 자동으로 trim하지 않는다.
     * null·빈 값·공백만 있는 값은 조회 전에 실패한다.
     * 전달된 password 배열은 성공·실패·예외 모두에서 지운다. 재시도에는 새 배열을 전달한다.
     * DB 장애는 인증 실패로 바꾸지 않고 StorageException을 호출자에게 그대로 전달한다.
     * DB와 해시 계산이 있으므로 Swing에서는 작업 스레드에서 호출한다.
     */
    public LoginResult login(String loginId, char[] password) {
        try {
            if (loginId == null || loginId.isBlank() || isBlank(password)) {
                return LoginResult.failure("ID와 비밀번호를 입력하세요.");
            }
            Optional<UserAccount> found = users.findByLoginId(loginId);
            if (found.isEmpty()) {
                return LoginResult.failure(INVALID_LOGIN);
            }
            UserAccount account = found.get();
            if (!verifier.verify(password, account.passwordHash())) {
                return LoginResult.failure(INVALID_LOGIN);
            }
            return LoginResult.success(new LoginUser(account.userId(), account.loginId()));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private static boolean isBlank(char[] password) {
        if (password == null || password.length == 0) {
            return true;
        }
        for (char character : password) {
            if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return true;
    }
}
