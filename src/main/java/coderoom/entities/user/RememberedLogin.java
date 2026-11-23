package coderoom.entities.user;

import coderoom.shared.db.Database;
import coderoom.shared.db.StorageException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** OS 사용자 설정의 기기 키와 현재 DB의 계정 연결을 함께 확인한다. */
public final class RememberedLogin {
    private static final String DEVICE_KEY = "remembered-login-device-key";
    private final Database database;
    private final Preferences preferences;

    public RememberedLogin(Database database, Preferences preferences) {
        this.database    = database;
        this.preferences = preferences;
    }

    public Optional<LoginUser> restore() {
        String key = deviceKey(false);
        if (key == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT users.user_id, users.login_id
                FROM remembered_logins JOIN users USING (user_id)
                WHERE device_hash = ?
                """;
        try (var connection = database.open();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, hash(key));
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(new LoginUser(rows.getLong(1), rows.getString(2))) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new StorageException("자동 로그인 정보를 읽지 못했습니다.", exception);
        }
    }

    public void save(LoginUser user) {
        String key = deviceKey(true);
        String sql = """
                INSERT INTO remembered_logins (device_hash, user_id) VALUES (?, ?)
                ON CONFLICT(device_hash) DO UPDATE SET user_id = excluded.user_id
                """;
        try (var connection = database.open();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, hash(key));
            statement.setLong(2, user.userId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("자동 로그인 정보를 저장하지 못했습니다.", exception);
        }
    }

    public void clear() {
        String key = deviceKey(false);
        if (key == null) {
            return;
        }
        try (var connection = database.open();
                var statement = connection.prepareStatement("DELETE FROM remembered_logins WHERE device_hash = ?")) {
            statement.setString(1, hash(key));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("자동 로그인 정보를 해제하지 못했습니다.", exception);
        }
    }

    private synchronized String deviceKey(boolean create) {
        try {
            String key = preferences.get(DEVICE_KEY, null);
            if (create) {
                if (key == null) {
                    byte[] random = new byte[32];
                    new SecureRandom().nextBytes(random);
                    key = HexFormat.of().formatHex(random);
                    preferences.put(DEVICE_KEY, key);
                }
                preferences.flush();
            }
            return key;
        } catch (BackingStoreException | SecurityException exception) {
            throw new StorageException("기기 설정에 접근할 수 없습니다.", exception);
        }
    }

    private static String hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
