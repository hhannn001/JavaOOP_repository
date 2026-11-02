# Test: 파트별 스텁과 드라이버

## 현재 상태

2026-11-02 기준 운영 Java 클래스는 필드·메서드가 없는 설계 초안이다. `src/test/java/coderoom`에만 실행 가능한 준비물을 추가했다. **9개 테스트 통과는 대역과 드라이버가 실행된다는 뜻이며 실제 로그인·Swing·게임·SQLite 검증 완료가 아니다.** 운영 기능 테스트와 수동 QA는 아직 미실행이다.

- 스텁: 다른 파트가 구현되기 전에 고정 데이터와 실패 응답을 제공하는 대역.
- 드라이버: 테스트 대상에 입력을 넣고 결과를 검증하는 호출 코드.
- `testkit/TestPorts.java`: 테스트 전용 임시 계약과 읽기 전용 화면 데이터. 운영 API가 아니다.
- `testkit/ScenarioStubs.java`: Auth, Catalog, Game, Records 대역. Records는 메모리 fake이며 파일 저장을 하지 않는다.
- `drivers/*Driver.java`: 각 파트에서 재사용할 `verify(...)` 시나리오.
- `drivers/StubHarnessTest.java`: 대역을 드라이버에 넣어 실행하는 JUnit 진입점.

## 실행

JDK 17 이상. 저장소 루트에서 실행한다. 첫 실행은 Maven 의존성 다운로드가 필요하다.

```sh
./mvnw clean verify
./mvnw -Dtest=StubHarnessTest test
./mvnw '-Dtest=StubHarnessTest#eRules' test
```

Windows에서는 `./mvnw` 대신 `mvnw.cmd`를 사용한다. 결과는 `target/surefire-reports`에 생성된다. `*Driver`는 독립 main 클래스나 자동 검색 테스트가 아니므로 `StubHarnessTest`를 실행해야 한다.

## 파트별 활용

| 담당 | 드라이버 / 개별 메서드 | 현재 검증 | 실제 연결 대상 / 후속 작업 |
| --- | --- | --- | --- |
| A | AFlowDriver / aFlow | 로그인→두 퍼즐→열쇠→탈출→저장→초기화 데이터 흐름 | AppController·GamePage 화면 전환, 작업 완료 콜백, 타이머 정리 검증 추가 |
| B | BLoginDriver / bLogin | 정상·오답·없는 계정·빈 입력 | LoginService를 Auth로 연결하고 실제 PasswordVerifier 및 사용자 저장소 검증 |
| C | CMapDriver / cMap | 카드 3개, 플레이 가능 1개, 신규 계정 기록 없음 | MapCatalogService를 Catalog로 연결, 기록 있는 계정 및 준비 중 맵 입장 차단 검증 |
| D | DRoomDriver / dRoomStates | START→DRAWER_OPEN→SAFE_OPEN 화면 데이터 준비 | 각 View를 RoomPanel 렌더링 입력으로 변환, 클릭·리사이즈 수동 QA |
| E | EGameDriver / eRules | 순서 위반·오답·중복 제출·열쇠 조건·시간 고정 | GameFacade/GameSession을 Game으로 연결, 퍼즐 형식 단위 테스트 추가 |
| F | FInputDriver / fInputData | 미획득 선택 거절·단서 선택·스냅샷 불변성 | InventoryPanel·CluePanel·AnswerInputPanel 콜백 연결, 실제 키보드 QA |
| G | GRecordDriver / gRecords | 최고 기록 유지·갱신·중복·계정/맵 격리·음수 거절 | JdbcClearRecordRepository를 Records로 연결, 임시 SQLite 및 결과 패널 검증 |

D/F 드라이버는 화면 데이터를 준비·검사할 뿐 Swing 컴포넌트를 구동하지 않는다. A 드라이버도 앱 화면 전환 통합 테스트는 아니다.

## 공통 테스트 데이터

- 사용자: demo1, demo2, demo3 / 비밀번호: test-only. 테스트 대역에서만 사용하는 평문 비교이며 실제 인증 코드로 복사하지 않는다.
- 맵: 1은 플레이 가능, 2·3은 준비 중. 최초 기록 없음.
- 서랍: DRAWER / 2312. 금고: SAFE / 423312133. 기존 HTML 프로토타입의 예시이며 DB seed 확정값은 아니다.
- 최초 NOTE1 → 서랍 후 NOTE2 추가 → 금고 후 KEY 추가 → KEY 선택 후 탈출.
- Game.escape에는 경과 초를 전달한다. 운영 설계는 nowNanos를 받으므로 어댑터에서 기준 시각+초×1,000,000,000으로 변환해야 한다.
- Game.view의 초는 대역에서 탈출 시 확정하는 값이다. 실시간 타이머를 흉내 내지 않는다.
- Auth/Records의 unavailable=true는 저장 장애를 발생시킨다. 대역 예외는 IllegalStateException이며 실제 StorageException 검증은 별도 작성한다.
- 각 테스트마다 새 인스턴스를 사용한다. Records는 테스트 간 공유하지 않는다.

## 실제 구현 연결 절차

1. B/C/E/G가 기존 설계의 생성자·DTO·저장소 메서드 계약을 먼저 합의한다. 현재 빈 운영 인터페이스를 이 임시 계약으로 대체하지 않는다.
2. 담당 파트 테스트에 `TestPorts.Auth`, `Catalog`, `Game`, `Records`의 어댑터를 작성한다. 어댑터는 실제 객체를 호출하고 DTO·enum·시간 단위만 변환한다. 정답 판정이나 최고 기록 로직을 재구현하면 안 된다.
3. 새 `*Test`의 `@Test`에서 `BLoginDriver.verify(adapter)`처럼 호출한다. 기존 `StubHarnessTest`는 대역 자체 검사로 유지한다.
4. 실제 테스트는 합의한 seed를 격리된 테스트 DB에 넣거나 해당 파트의 저장소 인터페이스를 구현한 대역을 주입한다. 전역/운영 DB는 사용하지 않는다.
5. 구현의 결과 DTO가 실패를 반환하는 경우 어댑터에서 의미를 매핑한다. 임시 계약의 예외나 선택 초기값 때문에 운영 설계를 바꾸지 않는다.
6. 단위 테스트 → 실제 JDBC 통합 테스트 → Swing 수동 QA 순서로 완료하고 결과를 구분해 기록한다.

대역은 지연·동시성·SQL·해시·입력 형식 전체를 구현하지 않는다. B는 null/공백과 해시 검증, E는 4/9자리·앞자리 0·시간 경계, G는 DB 재시작·장애 복구를 실제 구현 테스트로 확장해야 한다.

## 실행 증거

2026-11-02 `./mvnw clean verify`: BUILD SUCCESS, Checkstyle 위반 0, 테스트 9개 / 실패 0 / 오류 0 / 건너뜀 0. 검증 범위는 StubHarnessTest이다.
