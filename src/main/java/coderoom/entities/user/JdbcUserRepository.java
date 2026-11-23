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

    @Override public boolean canRegister() { return true; }

    @Override
    public boolean create(String loginId, String passwordHash) {
        String sql = """
                INSERT INTO users (login_id, password_hash) VALUES (?, ?)
                ON CONFLICT(login_id) DO NOTHING
                """;
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loginId);
            statement.setString(2, passwordHash);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new StorageException("계정을 저장하지 못했습니다.", exception);
        }
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
}
