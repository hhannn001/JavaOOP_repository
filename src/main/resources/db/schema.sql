-- 담당: G
-- 구현 예정: users, maps, puzzles, clear_records 테이블을 생성한다.
-- 모든 속성 NOT NULL. users.login_id UNIQUE. puzzles(map_id, step_no) UNIQUE.
-- clear_records는 (user_id, map_id) 복합 PK와 두 FK를 가진다.
-- puzzles.map_id는 maps.map_id를 참조한다.
-- playable은 0/1, best_seconds는 0 이상을 검증한다.
-- 구체 DDL은 구현 단계에서 작성한다.

CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY,
    login_id TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS maps (
    map_id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    topic TEXT NOT NULL,
    difficulty TEXT NOT NULL,
    poster_path TEXT NOT NULL,
    playable INTEGER NOT NULL CHECK (playable IN (0, 1))
);

CREATE TABLE IF NOT EXISTS puzzles (
    puzzle_id INTEGER PRIMARY KEY,
    map_id INTEGER NOT NULL,
    step_no INTEGER NOT NULL,
    code TEXT NOT NULL,
    answer_type TEXT NOT NULL CHECK (answer_type IN ('DIGITS4', 'GRID3X3')),
    answer TEXT NOT NULL,
    hint TEXT NOT NULL,

    UNIQUE (map_id, step_no),
    FOREIGN KEY (map_id) REFERENCES maps(map_id)
);

CREATE TABLE IF NOT EXISTS clear_records (
    user_id INTEGER NOT NULL,
    map_id INTEGER NOT NULL,
    best_seconds INTEGER NOT NULL CHECK (best_seconds >= 0),

    PRIMARY KEY (user_id, map_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (map_id) REFERENCES maps(map_id)
);