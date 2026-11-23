INSERT INTO maps (map_id, title, description, topic, difficulty, poster_path, playable)
VALUES (4, '새벽 온실의 마지막 씨앗',
'폭우가 온실의 운반 통로를 잠갔다.

마른 작업대에서 씨앗을 찾아내고 물받이의 흐름을 되살려라. 수면 아래에 숨었던 레일을 잇고, 안전한 갈래로 운반차를 밀어 새벽의 문턱을 넘어야 한다.',
'조건 분기 · 물리 퍼즐', '보통', '/images/posters/room02_last_seed.png', 1)
ON CONFLICT(map_id) DO UPDATE SET poster_path = excluded.poster_path
WHERE maps.poster_path = '/images/room/workshop/greenhouse/poster.jpg';

INSERT INTO puzzles (puzzle_id, map_id, step_no, code, answer_type, answer, hint)
VALUES
(311, 3, 1, 'ok: ✓ / ×    voltage: V

for (Module m : modules) {
    if (m.ok
        && m.voltage >= 15) {
        connect(m)' || char(59) || '
    }
}

비상 전원을 복구한다.', 'WORKSHOP', 'ACD', '각 모듈에서 if 조건의 두 부분을 확인한다.'),
(312, 3, 2, '1. 밸브를 닫고 A~D 손잡이에서 검침한다.
2. 밸브를 열고 같은 손잡이에서 다시 검침한다.
명찰은 닫힘 / 열림 순서다. ‘–’는 미검침이다.
3. 열림 − 닫힘이 가장 큰 손잡이를 다시 누른다.
‘수리 대상’을 확인하고 같은 줄 표시의 슬리브를 아래 받침에 놓는다.', 'WORKSHOP', 'CB', '같은 이음쇠에서 열린 값과 닫힌 값의 차이를 비교한다.'),
(313, 3, 3, 'ok: ✓ / ×    watts: W

Device[] queue = {
    C, B, A, D, E
}' || char(59) || '
int left = 70' || char(59) || '
for (Device d : queue) {
    if (d.ok
        && d.watts <= left) {
        turnOn(d)' || char(59) || '
        left -= d.watts' || char(59) || '
    }
}

켜질 차단기를 순서대로 예약한다.', 'WORKSHOP', 'CAE', '한 장비를 켤 때마다 남은 전력 left를 다시 계산한다.'),
(314, 3, 4, '안전 배출 기록

• 시작 압력 100 → 두 번째 직후 55
• 두 번째 감소량은 첫 번째보다 크다.
• 마지막 감소량은 두 번째보다 크다.

레버 네 개를 한 번씩 예약하고 배출한다.', 'WORKSHOP', 'DCAB', '합이 45인 두 레버 조합을 모두 찾아 본다. 그중 더 큰 값을 두 번째에 놓고, 마지막에 쓸 수 있는 값이 남는지 확인한다.'),
(315, 3, 5, '1. 아래 플러그를 위에 두 개씩 꽂는다.
왼쪽은 가동등, 오른쪽은 안전등이다.
각 불은 연결된 두 신호가 모두 정상일 때 켜진다.

2. 기록을 보고 각 플러그가 어느 쪽인지 찾는다.
차단 뒤 1은 왼쪽만, 2는 오른쪽만 켜진 상태다.
0은 둘 다 꺼짐, 3은 둘 다 켜짐이다.

int lamps = (running ? 1 : 0)
          + (safe ? 2 : 0)' || char(59) || '

power만 차단 → 2
sealed만 차단 → 1
sealed와 loadOk 동시 차단 → 0

3. 양쪽에서 플러그 하나씩 시험한다.
플러그 → 계속 켜져 있을 불 → 빨간 버튼 순으로 누른다. 빨간 버튼을 다시 누르면 복구된다.
두 번 모두 맞히고 복구한 뒤 황동 버튼을 누른다.', 'WORKSHOP', 'ACBD', 'sealed만 끊을 때 1인데 loadOk도 함께 끊으면 0이다. 추가로 꺼진 표시등은 어느 쪽인가?'),
(401, 4, 1, '봉투의 표식을 왼쪽부터 읽는다. leaf는 잎 모양, state는 손상 여부, stamp는 도장이다. reject가 true면 남기지 않는다.

boolean validShape =
    leaf == ''✹'' || stamp == ''▲''' || char(59) || '
boolean reject =
    state == ''×''
    || (leaf == ''✹''
        && stamp == ''∿'')' || char(59) || '
if (validShape && !reject) {
    keep(sample)' || char(59) || '
}

남길 봉투를 모두 열고 황동 버튼으로 수거한다.', 'WORKSHOP', 'BC', '각 봉투의 validShape와 reject를 따로 계산한다.'),
(402, 4, 2, '화면에 보이는 유입관과 배수구 높이에 맞춰 홈통 끝을 이어라. 밸브로 물길을 시험한다.', 'WORKSHOP', 'ABC', '첫 누수 지점에서 양쪽 홈통 끝의 높이를 비교한다.'),
(403, 4, 3, '가로 발판을 왼쪽으로 빼고, 세로 발판을 아래 받침에 끼운 뒤 가로 발판을 오른쪽 받침에 끼운다.
가로·세로 레일 조절 버튼으로 양쪽 레일 끝을 고정 레일에 맞추고 황동 걸쇠를 누른다.', 'WORKSHOP', 'AB', '교차로에 발판이 있으면 다른 발판은 지나갈 수 없다.'),
(404, 4, 4, 'lightSlot과 heavySlot은 각각 추 2와 추 3을 건 고리 번호다.

int left = 13 * 1' || char(59) || '
int torque(int weight, int slot) {
    return weight * slot' || char(59) || '
}
int right = torque(2, lightSlot)
          + torque(3, heavySlot)' || char(59) || '
boolean balanced = left == right' || char(59), 'WORKSHOP', 'BC', '한 추를 먼저 건 뒤 오른쪽에 부족한 무게 × 거리를 계산한다.'),
(405, 4, 5, 'if (flooded) highTrack()' || char(59) || '
else lowTrack()' || char(59) || '

if (bufferAhead) sideTrack()' || char(59) || '
else mainTrack()' || char(59) || '

빈 차로 점검한 뒤 씨앗을 운반한다.', 'WORKSHOP', 'BAAB', '낮은 선로의 물과 본선의 정지턱을 살펴본다.')
ON CONFLICT(map_id, step_no) DO UPDATE SET
    puzzle_id = excluded.puzzle_id,
    code = excluded.code,
    answer_type = excluded.answer_type,
    answer = excluded.answer,
    hint = excluded.hint
WHERE (puzzles.map_id = 3 AND puzzles.puzzle_id BETWEEN 301 AND 305
    AND puzzles.answer_type <> 'WORKSHOP')
    OR (puzzles.map_id = 4 AND puzzles.puzzle_id = 404 AND puzzles.answer = 'AB')
    OR (puzzles.map_id = 4 AND puzzles.puzzle_id = 402 AND puzzles.code LIKE '%왼쪽 화분으로%')
    OR (puzzles.answer_type = 'WORKSHOP'
        AND puzzles.puzzle_id = excluded.puzzle_id
        AND (puzzles.code <> excluded.code OR puzzles.hint <> excluded.hint));

UPDATE maps SET playable = 1
WHERE map_id = 3 AND title = '자정의 기계실'
    AND (SELECT count(*) FROM puzzles WHERE map_id = 3 AND answer_type = 'WORKSHOP') = 5;
