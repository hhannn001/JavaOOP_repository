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
                migrateMapColumns(connection);
                migratePuzzleTypes(connection);
                runScript(connection, "/db/seed.sql");
                runScript(connection, "/db/workshops.sql");
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

    /** Adds display settings without replacing map rows referenced by existing records. */
    private void migrateMapColumns(Connection connection) throws SQLException {
        boolean hintLimit = false;
        boolean archived  = false;
        try (Statement statement = connection.createStatement();
                java.sql.ResultSet columns = statement.executeQuery("PRAGMA table_info(maps)")) {
            while (columns.next()) {
                String name = columns.getString("name");
                hintLimit |= "hint_limit".equals(name);
                archived  |= "archived".equals(name);
            }
        }
        try (Statement statement = connection.createStatement()) {
            if (!hintLimit) {
                statement.execute("ALTER TABLE maps ADD COLUMN hint_limit INTEGER NOT NULL DEFAULT 2 CHECK (hint_limit >= 0)");
            }
            if (!archived) {
                statement.execute("ALTER TABLE maps ADD COLUMN archived INTEGER NOT NULL DEFAULT 0 CHECK (archived IN (0, 1))");
            }
        }
    }

    private void migratePuzzleTypes(Connection connection) throws SQLException, IOException {
        try (Statement statement = connection.createStatement();
                java.sql.ResultSet schema = statement.executeQuery(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name='puzzles'")) {
            if (schema.next() && schema.getString(1).contains("'WORKSHOP'")) {
                return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE puzzles RENAME TO legacy_puzzles");
            runScript(connection, "/db/schema.sql");
            statement.executeUpdate("""
                    INSERT INTO puzzles (puzzle_id, map_id, step_no, code, answer_type, answer, hint)
                    SELECT puzzle_id, map_id, step_no, code, answer_type, answer, hint FROM legacy_puzzles
                    """);
            statement.executeUpdate("DROP TABLE legacy_puzzles");
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
