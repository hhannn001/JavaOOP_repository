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

    /** 회원가입 결과는 로그인된 사용자를 포함하지 않는다. 가입 후 별도로 로그인한다. */
    public record RegistrationResult(boolean success, String message) { }

    /**
     * 신규 ID는 영문·숫자·밑줄 3~32자, 비밀번호는 8~128자이며 공백만 입력할 수 없다.
     * 기존 계정의 로그인 정책은 변경하지 않는다. 두 암호 배열은 모든 종료 경로에서 지운다.
     */
    public RegistrationResult register(String loginId, char[] password, char[] confirmation) {
        try {
            if (loginId == null || !loginId.matches("[A-Za-z0-9_]{3,32}")) {
                return new RegistrationResult(false, "ID는 영문·숫자·밑줄로 3~32자 입력하세요.");
            }
            if (isBlank(password) || password.length < 8 || password.length > 128) {
                return new RegistrationResult(false, "비밀번호는 공백만 제외하고 8~128자로 입력하세요.");
            }
            if (!Arrays.equals(password, confirmation)) {
                return new RegistrationResult(false, "비밀번호 확인이 일치하지 않습니다.");
            }
            // 해시 생성 전에 빠른 중복 안내. 최종 중복 판정은 DB의 UNIQUE 제약에 맡긴다.
            if (users.findByLoginId(loginId).isPresent()) {
                return new RegistrationResult(false, "이미 사용 중인 ID입니다.");
            }
            if (!users.create(loginId, verifier.hash(password))) {
                return new RegistrationResult(false, "이미 사용 중인 ID입니다.");
            }
            return new RegistrationResult(true, "회원가입이 완료되었습니다. 로그인하세요.");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
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
