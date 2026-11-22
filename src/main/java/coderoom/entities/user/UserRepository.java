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

    /**
     * 해시로 새 계정을 저장한다. ID 중복이면 false, 저장 성공이면 true.
     * 조회 전용 대역은 기본 구현을 유지할 수 있다. 실제 저장소는 반드시 구현한다.
     * 저장 장애는 StorageException으로 전달한다.
     */
    default boolean create(String loginId, String passwordHash) {
        throw new UnsupportedOperationException("회원가입을 지원하지 않는 저장소입니다.");
    }
}
