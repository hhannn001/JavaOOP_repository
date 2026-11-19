# A 구현 및 스텁 연동

## 범위

화면 전환과 대시보드는 A 소유의 `app/*`, 게임 수명 관리는 `pages/game/*`에 둔다. 대시보드의 상세 계약은 [A 임시 대시보드](A_임시_대시보드.md)를 따른다.

- `MainFrame`: JFrame 하나, CardLayout 화면 교체, 제거, 종료 콜백.
- `AppController`: 로그인 → 대시보드 → 맵 → 게임 → 맵 복귀, 대시보드 복귀·로그아웃, 새 게임 생성, 이전 화면의 늦은 콜백 무시, 저장 중 이동 차단, 미저장 이탈 확인.
- `DashboardPage`: 픽셀 카페 로비, 제목·고양이·벨·창문 애니메이션과 사물 메뉴. 맵 이동·로그아웃·DB 기록 조회를 연결한다.
- `GamePage`: 위젯 배치와 이벤트 전달, SwingWorker 조회·저장, EDT 상태 변경, 저장 실패·동일 기록 재시도, 타이머 시작·종료, 폐기한 화면의 늦은 작업 결과 무시.
- `Main.launch`: 초기화는 작업 스레드에서 실행하고 성공했을 때만 EDT에서 컨트롤러를 생성·시작한다.

## 운영 구현과 테스트 대역의 경계

운영 모델과 서비스는 구현되어 있다. 다음 인터페이스는 실제 구현과 테스트 대역을 교체하면서 화면 수명을 검증하는 경계다. `AppScreens.GameAdapter`는 `GameFacade`를 호출하고 `GameWidgets`는 실제 위젯을 조립한다.

| 경계 | 테스트 대역 | 현재 운영 연결 |
| --- | --- | --- |
| `AppController.Host` | `AIntegrationStubs.Host` | `MainFrame`이 이미 구현. 프레임 표시와 사용자 확인창은 조립 지점에서 연결 |
| `AppController.Screens` | `AIntegrationStubs.Screens` | A 대시보드와 B/C 화면 생성, 로그인·대시보드·맵 선택 콜백, 게임마다 새 Facade·위젯·GamePage 생성 |
| `GamePage.GamePort` | `AIntegrationStubs.Game` | E의 GameFacade와 메서드 연결. 사용자 ID 추출·결과 DTO 변환은 GameAdapter에서 처리 |
| `GamePage.Widgets` | `AIntegrationStubs.Widgets` | D/F/G 위젯 조립, 같은 GameView 전달, 입력·아이템·저장 재시도 콜백 연결 |
| `Main.Bootstrap` | 테스트의 익명 구현 | G의 DB 초기화, JDBC 저장소·서비스 조립, MainFrame 표시 |

`GamePort.loadMap`과 `saveClear`는 작업 스레드에서 호출한다. 나머지 게임 조작과 모든 위젯 호출은 EDT에서 실행한다. `Widgets.bind`로 받은 GamePage의 `inspect`, `submit`, `selectItem`, `useKey`, `retrySave`를 각 위젯 콜백에 연결한다. 맵 복귀·재시작은 `Screens.game`으로 받은 콜백을 사용한다. 미저장 기록 확인은 `AppController` 생성자의 `BooleanSupplier`로 제공한다.

`ClearRecord`는 final 필드와 읽기 전용 접근자를 가진 값 객체다. A는 탈출 결과의 기록을 보관하고 저장 재시도에도 같은 값을 사용하며, 저장 작업에서 게임 상태를 읽지 않는다. 테스트에는 스텁 수명 검증과 실제 서비스·위젯·SQLite 연동 검증이 함께 있다.

`Main.main`은 실제 SQLite 저장소와 서비스를 조립하여 로그인·로비·맵 선택·첫 방 플레이·기록 저장을 연결한다. 로비 기록 창도 DB에서 조회한다. 아래 스텁은 테스트용 경로이며 실제 실행 구성과 구분한다. 현재 연결 범위와 미완료 항목은 [DB·게임 연동](DB_게임_연동.md)을 따른다.

## 검증 실행

```sh
./mvnw -Djava.awt.headless=true clean verify
```

`AIntegrationTest`는 실제 A 컨트롤러와 GamePage에 스텁을 주입하는 통합 드라이버다. 기존 `StubHarnessTest`가 테스트 대역 자체를 확인하는 것과 구분된다.

검증 항목: 전체 화면 흐름과 계정 변경, 조회 중복·이동 차단, 조회 실패 재시도, 저장 실패·동일 기록 재시도, 모든 이탈 경로 차단과 미저장 확인, 탈출 실패 시 타이머 유지, 폐기된 조회 결과 무시, 재시작 시 이전 타이머 종료, 오래된 화면 콜백 무시, 초기화 성공·실패와 EDT 경계.

디스플레이가 있는 환경에서는 아래 명령으로 실제 MainFrame의 카드 교체와 창 종료 차단도 검증한다. 헤드리스 실행에서는 실제 창 테스트는 건너뛴다.

```sh
./mvnw -Dtest=AIntegrationTest clean verify
```

자동 테스트는 실제 서비스와 위젯 연동도 검사한다. 그래픽 배치·이미지·애니메이션의 시각 품질은 별도 수동 QA로 확인한다. 테스트별 범위는 [테스트 가이드](Test_드라이버_가이드.md)를 따른다.
