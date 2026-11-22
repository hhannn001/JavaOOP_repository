package coderoom.entities.user;

import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** 로그인 ID로 계정을 조회한다. 계정 부재와 DB 장애를 구분한다. */
public final class JdbcUserRepository implements UserRepository {
    private final Database database;

    public JdbcUserRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Optional<UserAccount> findByLoginId(String loginId) {
        String sql = "SELECT user_id, login_id, password_hash FROM users WHERE login_id = ?";
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loginId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserAccount(rows.getLong("user_id"),
                        rows.getString("login_id"), rows.getString("password_hash")));
            }
        } catch (SQLException exception) {
            throw new StorageException("계정 조회에 실패했습니다.", exception);
        }
    }

    @Override
    public boolean create(String loginId, String passwordHash) {
        Objects.requireNonNull(loginId, "로그인 ID가 필요합니다.");
        Objects.requireNonNull(passwordHash, "비밀번호 해시가 필요합니다.");
        if (loginId.isBlank() || passwordHash.isBlank()) {
            throw new IllegalArgumentException("로그인 ID와 해시는 비어 있을 수 없습니다.");
        }
        // UNIQUE 제약으로 동시 가입도 판정한다. 다른 SQL 장애는 중복 ID로 숨기지 않는다.
        String sql = "INSERT INTO users (login_id, password_hash) VALUES (?, ?) "
                + "ON CONFLICT(login_id) DO NOTHING";
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loginId);
            statement.setString(2, passwordHash);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new StorageException("회원가입 정보를 저장할 수 없습니다.", exception);
        }
    }

}
