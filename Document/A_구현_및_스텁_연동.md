# A 구현 및 스텁 연동

## 범위

운영 코드 변경은 A 소유의 `app/*`, `pages/game/*`에 한정한다. 기존 설계 주석은 보존하고 구현 설명을 추가했다. B~G의 초안 클래스·메서드·열거형·DB·위젯 내부는 수정하지 않는다.

- `MainFrame`: JFrame 하나, CardLayout 화면 교체, 제거, 종료 콜백.
- `AppController`: 로그인 → 맵 → 게임 → 맵 복귀 → 로그아웃, 새 게임 생성, 이전 화면의 늦은 콜백 무시, 저장 중 이동 차단, 미저장 이탈 확인.
- `GamePage`: 위젯 배치와 이벤트 전달, SwingWorker 조회·저장, EDT 상태 변경, 저장 실패·동일 기록 재시도, 타이머 시작·종료, 폐기한 화면의 늦은 작업 결과 무시.
- `Main.launch`: 초기화는 작업 스레드에서 실행하고 성공했을 때만 EDT에서 컨트롤러를 생성·시작한다.

## 미연동 경계

현재 `GameFacade`, `LoginUser`, `GameView`, `ClearRecord` 등은 구현이 없는 초안이다. 다른 담당자의 API를 임의로 구현하지 않기 위해 A 소유 클래스 안에 다음 인터페이스를 둔다.

| 경계 | 테스트 대역 | 실제 연동 시 맡을 일 |
| --- | --- | --- |
| `AppController.Host` | `AIntegrationStubs.Host` | `MainFrame`이 이미 구현. 프레임 표시와 사용자 확인창은 조립 지점에서 연결 |
| `AppController.Screens` | `AIntegrationStubs.Screens` | B/C 화면 생성, 로그인·맵 선택 콜백, 게임마다 새 Facade·위젯·GamePage 생성 |
| `GamePage.GamePort` | `AIntegrationStubs.Game` | E의 GameFacade와 메서드 연결. 사용자 ID 추출·결과 DTO 변환도 실제 계약 확정 후 어댑터에서 처리 |
| `GamePage.Widgets` | `AIntegrationStubs.Widgets` | D/F/G 위젯 조립, 같은 GameView 전달, 입력·아이템·저장 재시도 콜백 연결 |
| `Main.Bootstrap` | 테스트의 익명 구현 | G의 DB 초기화, JDBC 저장소·서비스 조립, MainFrame 표시 |

`GamePort.loadMap`과 `saveClear`는 작업 스레드에서 호출한다. 나머지 게임 조작과 모든 위젯 호출은 EDT에서 실행한다. `Widgets.bind`로 받은 GamePage의 `inspect`, `submit`, `selectItem`, `useKey`, `retrySave`를 각 위젯 콜백에 연결한다. 맵 복귀·재시작은 `Screens.game`으로 받은 콜백을 사용한다. 미저장 기록 확인은 `AppController` 생성자의 `BooleanSupplier`로 제공한다.

실제 연동 시 `ClearRecord`는 불변 값이어야 한다. A는 탈출 결과에 전달받은 기록을 보관하고 재시도에도 같은 인스턴스를 사용하며 저장 작업에서 게임 상태를 읽지 않는다. 현재 테스트는 빈 DTO의 인스턴스 전달과 수명만 검증한다. 비어 있는 `ItemId`·`RoomObjectId` 열거형 대신 테스트에서 null을 전달하는 것도 콜백 전달 검증에 한정한다.

`Main.main`은 A의 시연용 화면을 띄운다. `LoginPage`·`MapSelectionPage`와 게임용 임시 포트의 화면으로 전환과 타이머 정리를 눈으로 확인할 수 있다. 시연용 버튼은 인증·정답 판정·기록 저장을 하지 않으며, 실제 플레이 가능한 게임이나 B~G 통합 완료를 의미하지 않는다.

## 검증 실행

```sh
./mvnw -Djava.awt.headless=true clean verify
```

`AIntegrationTest`는 실제 A 컨트롤러와 GamePage에 스텁을 주입하는 통합 드라이버다. 기존 `StubHarnessTest`가 테스트 대역 자체를 확인하는 것과 구분된다.

검증 항목: 전체 화면 흐름과 계정 변경, 조회 중복·이동 차단, 조회 실패 재시도, 저장 실패·동일 기록 재시도, 모든 이탈 경로 차단과 미저장 확인, 탈출 실패 시 타이머 유지, 폐기된 조회 결과 무시, 재시작 시 이전 타이머 종료, 오래된 화면 콜백 무시, 초기화 성공·실패와 EDT 경계.

디스플레이가 있는 환경에서는 아래 명령으로 실제 MainFrame의 카드 교체와 창 종료 차단도 검증한다. 헤드리스 실행에서는 해당 테스트 하나만 건너뛴다.

```sh
./mvnw -Dtest=AIntegrationTest clean verify
```

이 테스트는 그래픽 배치·이미지·실제 퍼즐·SQL·사용자 인증의 품질을 검증하지 않는다. 해당 구현이 제공되면 어댑터를 연결한 뒤 전체 게임 시나리오를 추가 검증해야 한다.
