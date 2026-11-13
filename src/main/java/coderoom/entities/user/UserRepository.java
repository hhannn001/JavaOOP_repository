package coderoom.entities.user;

import java.util.Optional;

/** 담당: B. 사용자 계정 조회 계약. SQL이나 Swing에 의존하지 않는다. */
@FunctionalInterface
public interface UserRepository {
    /**
     * 로그인 ID로 계정을 찾는다. loginId는 null이나 공백이 아닌 값을 전달한다.
     *
     * @return 계정이 있으면 해당 계정, 계정이 없을 때만 Optional.empty()
     * @throws coderoom.shared.db.StorageException DB 연결 또는 조회에 실패한 경우
     */
    Optional<UserAccount> findByLoginId(String loginId);
}
