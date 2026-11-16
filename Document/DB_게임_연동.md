# DB·게임 연동

## 실행 구성

`Main`에서 SQLite를 초기화한 뒤 `JdbcUserRepository`, `JdbcMapRepository`, `JdbcPuzzleRepository`, `JdbcClearRecordRepository`를 서비스에 주입한다. 실제 실행에는 메모리 계정이나 DemoGame을 사용하지 않는다.

- 계정 조회·비밀번호 검증, 맵·퍼즐 조회, 기록 저장은 SwingWorker에서 수행한다.
- 게임 상태와 화면 변경은 GamePage의 EDT 경계에서 수행한다.
- 새 게임마다 새 GameFacade와 GameSession을 생성한다.
- 초기화 실패 시 정상 로그인 화면을 열지 않는다. 맵 조회 실패에는 재시도 버튼을 제공한다.
- 게임 로드 실패 시 설정 → 나가기로 돌아가 다시 입장할 수 있다.

## 데이터

SQL 원본: `src/main/resources/db/schema.sql`, `seed.sql`.

| 테이블 | 초기 데이터 |
| --- | --- |
| users | demo1~3, test 총 4개. PBKDF2-HMAC-SHA256 해시 |
| maps | 3개. 첫 번째만 플레이 가능 |
| puzzles | 첫 방의 DIGITS4, GRID3X3 퍼즐 2개 |
| clear_records | 없음. 실제 탈출 시 계정·맵별 최고 기록 저장 |

시연 계정 비밀번호는 README에 명시한다. 시드 재실행은 계정·퍼즐·기록을 유지하며, 사용자·맵을 참조하지 않는 기록은 외래키로 차단한다. 로컬 DB 파일은 커밋하지 않는다.

## 게임 규칙

시작 시 메모 1을 보유한다. 서랍 정답은 `2312`, 벽 다이얼 정답은 행 순서대로 `423312133`이다. 서랍을 열어야 두 번째 퍼즐에 입력할 수 있다. 금고를 열어 열쇠를 얻은 뒤 열쇠를 선택하고 문을 누르면 탈출한다.

정답은 문자열로 판정하여 앞자리 0을 보존한다. 퍼즐 코드는 표시만 하며 실행하지 않는다. UI에는 코드·힌트만 전달하고 정답은 전달하지 않는다. 탈출 시간은 한 번 확정하며 저장 재시도에서 같은 값을 사용한다. UPSERT는 더 빠른 기록만 보존한다.

## 검증

- DatabaseGameTest: 실제 JDBC 로그인, SQL 입력 안전성, 반복 초기화, 제약조건, 잘못된 데이터 실패, 두 퍼즐과 탈출, DB 재연결 후 기록 보존 및 계정 분리.
- AIntegrationTest: 비동기 로그인·맵 조회·게임 로드, 실제 입력 패널 제출과 결과 저장, 기존 저장 실패 재시도·화면 폐기·이탈 방지 테스트.
- 실제 창 테스트: 로그인부터 방 진입, 설정을 통한 복귀 및 창 종료.

DB 파일 경로는 `-Dcoderoom.db=...`로 바꿀 수 있다. 기본 파일은 실행 디렉터리의 `coderoom.db`다. 데이터만 준비하려면 `Main --init-db`를 사용한다.

## 진행 중인 변경 사항 (2026-11-16 작업 트리 기준)

- `Main` → `AppScreens` → `DashboardRecordDialog`에 `DashboardRecords`를 연결했다. 방별 상위 9명·내 순위·최고 기록과 로그인 ID별 진척도를 실제 DB에서 읽는다. 개별 문제 풀이 이력은 저장하지 않는다.
- 진척도는 현재 준비 중인 맵을 포함한 전체 등록 맵 기준이다. 플레이 가능 맵만을 기준으로 하는 기획과의 조정은 남아 있다.
- `maps.hint_limit`은 기본값 2, 0 이상 제약을 갖는다. 초기화 시 컬럼이 없는 기존 DB에만 추가하고 기존 계정·맵·기록을 유지한다. `JdbcMapRepository`가 읽어 `GameMap.getHintLimit()`으로 제공한다.
- 힌트 제한은 **설정 저장·조회까지만 구현**했다. 공개 UI에서 횟수를 차감하거나 제한하지 않는다. `CluePanel`의 메모별 힌트 공개 상태와 DB 제한 값은 아직 연결되지 않았다.
- `GameOverlayLayout`으로 방 위에 단서·입력·인벤토리를 배치하고 `PixelTheme`·`ItemSprite`로 종이·잠금장치·아이템을 표현한다. 정답 판정은 기존 GameFacade 경계를 유지한다.

관련 자동 테스트: `DashboardRecordsTest`(순위·진척도), `SceneObjectButtonTest`(사물 입력), `ObjectUiTest`(게임 위젯), `HintSettingsTest`(설정·기존 DB 마이그레이션), `AIntegrationTest`(화면 연동). 실제 창의 레이아웃·애니메이션은 별도 수동 QA 대상이다.

## 기본 테마 갱신

`seed.sql`은 이전 영문 기본 이름 또는 현재 기본 한글 이름과 일치하는 맵의 제목·설명·주제·난이도·포스터를 갱신한다. 사용자 정의 이름의 맵과 플레이 가능 여부·힌트 제한·계정·퍼즐·기록은 유지한다. 현재 기본 테마는 서재의 마지막 암호, 사라진 기록, 자정의 기계실이다.
