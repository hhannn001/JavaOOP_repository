-- 담당: G (사용자 해시 형식 B, 맵 데이터 C, 퍼즐 데이터 E와 합의)
-- 임시 데이터: 맵 3개(플레이 가능 1개), 서재 퍼즐 2개.
-- 로컬 시연 계정: test/test1234, demo1~3/test-only. PBKDF2 해시만 저장한다.
-- 퍼즐 정답 문자열: 2312, 423312133. 코드의 실제 출력과 일치하도록 검증한다.
-- 반복 초기화 시 기존 계정·퍼즐·클리어 기록을 덮어쓰지 않는다.
-- 현재 Database의 세미콜론 분할 방식과 호환되도록 코드 안의 세미콜론은 char(59)로 표현한다.
-- 인벤토리는 별도 테이블 없이 GameSession의 진행 단계에서 NOTE1, NOTE2, KEY를 계산한다.

-- 기본 테마 설정. 신규 DB와 이전 영문 기본 테마에 동일하게 적용한다.
-- 사용자 정의 이름의 맵, 플레이 가능 여부, 힌트 설정, 퍼즐과 클리어 기록은 유지한다.
INSERT INTO maps (map_id, title, description, topic, difficulty, poster_path, playable)
VALUES
    (1, '서재의 마지막 암호',
     '책장이 닫히기 전, 마지막 암호를 풀어라.

비 내리는 밤, 주인 없는 서재에서 문이 잠겼다. 책상 위에는 누군가 남긴 두 장의 메모와 암호로 잠긴 서랍이 놓여 있다.

숫자 사이에 숨은 규칙을 읽고 벽의 다이얼을 맞춰라. 마지막 단서가 가리키는 열쇠를 찾아 이 서재를 벗어나야 한다.',
     '추리 · 암호', '보통', '/images/posters/room01_study_cipher.png', 1),
    (3, '자정의 기계실',
     '멈춘 톱니가 움직이면, 출구가 열린다.

자정을 가리킨 채 멈춘 시계 아래, 오래된 기계실의 경고등이 켜졌다. 굳게 닫힌 철문 너머에서는 정체를 알 수 없는 진동이 이어진다.

흩어진 설계의 흔적을 읽고 장치의 연결을 복원하라. 잠든 기계를 다시 움직이는 것만이 이곳을 빠져나갈 방법이다.',
     '기계 · 모험', '어려움', '/images/posters/room03_midnight_engine.png', 0)
ON CONFLICT(map_id) DO UPDATE SET
    title=excluded.title,
    description=excluded.description,
    topic=excluded.topic,
    difficulty=excluded.difficulty,
    poster_path=excluded.poster_path
WHERE maps.title IN ('The Last Compile', 'Stopped Machine',
                     '서재의 마지막 암호', '자정의 기계실');

INSERT INTO puzzles (puzzle_id, map_id, step_no, code, answer_type, answer, hint)
VALUES
    (1, 1, 1,
     'int[] steps = {2, 1, -2, 1}' || char(59) || '
int total = 0' || char(59) || '
for (int step : steps) {
    total += step' || char(59) || '
    System.out.print(total)' || char(59) || '
}',
     'DIGITS4', '2312',
     '반복할 때마다 total을 이어서 사용한다. 코드가 출력한 순서대로 롤러를 돌린다.'),
    (2, 1, 2,
     'int[][] grid = {
    {4, 3, 1},
    {2, 1, 3},
    {3, 2, 3}
}' || char(59) || '
for (int row = 0' || char(59) || ' row < 3' || char(59) || ' row++) {
    for (int col = 0' || char(59) || ' col < 3' || char(59) || ' col++) {
        System.out.print(grid[col][row])' || char(59) || '
    }
    System.out.println()' || char(59) || '
}',
     'GRID3X3', '423312133',
     'grid에서 첫 인덱스는 행이다. 코드가 col을 첫 인덱스에 넣는다는 점을 확인한다.')
ON CONFLICT(map_id, step_no) DO UPDATE SET code=excluded.code, hint=excluded.hint
WHERE puzzles.map_id = 1 AND puzzles.puzzle_id = excluded.puzzle_id
    AND (puzzles.code LIKE '%System.out.print(digit + 1)%'
        OR puzzles.code LIKE '%System.out.print(n + 1)%');

-- 기존 계정의 비밀번호와 기록을 초기화하지 않는다.
INSERT INTO users (user_id, login_id, password_hash)
VALUES
    (1, 'demo1', 'v1$pbkdf2-sha256$600000$9r/yyzWXdD2UlSJeXv/cIw==$tmojqlvKd2Fqyg1bgC09+LtJ8CcZhMpaai9oH0D8kFk='),
    (2, 'demo2', 'v1$pbkdf2-sha256$600000$HZOwmM+4oBnaC0MeLraPDQ==$rr4oHhT58A0DbcHRsOx1bhmuBCmRFtNRJ5gcYeojNgQ='),
    (3, 'demo3', 'v1$pbkdf2-sha256$600000$SPzEgYlB7exN4qJYxLqqgQ==$IpucdeWoAObMmFmjrJo0TXLMCTGRpQ4mv1LnoU2ckWc='),
    (4, 'test', 'v1$pbkdf2-sha256$600000$u+LQ9iXt8nk19J7Fi9Nwaw==$n4p0xK2Vy+ICR04ICxgq4Oz3hzVd1Z//LRXBqWJEZwI=')
ON CONFLICT DO NOTHING;

-- Keep the previous archive map and its historical records in existing databases.
UPDATE maps SET archived = 1, playable = 0 WHERE map_id = 2;
