package coderoom.shared.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** SQLite 연결과 반복 가능한 초기화. 연결은 호출자가 닫는다. */
public final class Database {
    private final String url;

    public Database() {
        this("coderoom.db");
    }

    public Database(String dbFilePath) {
        url = "jdbc:sqlite:" + Objects.requireNonNull(dbFilePath);
    }

    public Connection open() {
        try {
            Connection connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            } catch (SQLException exception) {
                connection.close();
                throw exception;
            }
            return connection;
        } catch (SQLException exception) {
            throw new StorageException("데이터베이스에 연결할 수 없습니다.", exception);
        }
    }

    public void initialize() {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                runScript(connection, "/db/schema.sql");
                runScript(connection, "/db/seed.sql");
                connection.commit();
            } catch (SQLException | IOException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException | IOException exception) {
            throw new StorageException("데이터베이스 초기화에 실패했습니다.", exception);
        }
    }

    // 이 프로젝트의 SQL 리소스는 문자열 내부 세미콜론을 char(59)로 표현한다.
    private void runScript(Connection connection, String resource) throws SQLException, IOException {
        String script;
        try (InputStream input = Database.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("SQL 파일을 찾을 수 없습니다: " + resource);
            }
            script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
    }
}
