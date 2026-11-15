-- 담당: G (사용자 해시 형식 B, 맵 데이터 C, 퍼즐 데이터 E와 합의)
-- 임시 데이터: 맵 3개(플레이 가능 1개), 서재 퍼즐 2개.
-- 시연 계정은 인증 구현과 해시 형식을 합의한 뒤 별도로 추가한다.
-- 퍼즐 정답 문자열: 2312, 423312133. 코드의 실제 출력과 일치하도록 검증한다.
-- 반복 초기화 시 기존 계정·퍼즐·클리어 기록을 덮어쓰지 않는다.
-- 현재 Database의 세미콜론 분할 방식과 호환되도록 코드 안의 세미콜론은 char(59)로 표현한다.
-- 인벤토리는 별도 테이블 없이 GameSession의 진행 단계에서 NOTE1, NOTE2, KEY를 계산한다.

INSERT INTO maps (map_id, title, description, topic, difficulty, poster_path, playable)
VALUES
    (1, 'The Last Compile', '마지막 컴파일을 완성하라.', 'Java', '보통',
     '/images/posters/room01_last_compile.png', 1),
    (2, 'Missing Notes', '흩어진 단서를 모아라.', 'Java', '어려움',
     '/images/posters/room02_missing_notes.png', 0),
    (3, 'Stopped Machine', '멈춘 장치를 다시 켜라.', 'Java', '어려움',
     '/images/posters/room03_stopped_machine.png', 0)
ON CONFLICT(map_id) DO NOTHING;

INSERT INTO puzzles (puzzle_id, map_id, step_no, code, answer_type, answer, hint)
VALUES
    (1, 1, 1,
     'int[] digits = {1, 2, 0, 1}' || char(59) || '

for (int digit : digits) {
    System.out.print(digit + 1)' || char(59) || '
}',
     'DIGITS4', '2312',
     '배열의 숫자를 왼쪽부터 하나씩 꺼내 1을 더하세요. print는 줄을 바꾸지 않으므로 결과를 이어 붙이면 됩니다.'),
    (2, 1, 2,
     'int[][] grid = {
    {3, 1, 2},
    {2, 0, 1},
    {0, 2, 2}
}' || char(59) || '

for (int[] row : grid) {
    for (int n : row) {
        System.out.print(n + 1)' || char(59) || '
    }
    System.out.println()' || char(59) || '
}',
     'GRID3X3', '423312133',
     '모든 칸의 숫자에 각각 1을 더하세요. 첫 번째 줄은 4, 2, 3입니다. 같은 방법으로 나머지 두 줄을 채워보세요.')
ON CONFLICT DO NOTHING;
