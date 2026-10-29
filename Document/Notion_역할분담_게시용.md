이 페이지는 현재 저장소의 Java Swing 클래스 초안과 7인 역할 분담 문서를 기준으로 작성했습니다. 아래 경로는 **저장소 루트 기준**입니다. 각 파일에는 아직 상세 구현 대신 담당자와 구현할 내용이 주석으로 적혀 있습니다.

## 먼저 보는 담당 영역

| 담당 | 담당 파트 | 해야 할 일 |
| --- | --- | --- |
| **A** | 앱 실행과 화면 전환, 게임 화면 통합 | 로그인→맵 선택→방 입장 흐름을 연결하고 각 위젯을 GamePage에 조립한다. |
| **B** | 로그인과 사용자 저장 | 로그인 화면, 비밀번호 해시 검증, users 조회를 맡는다. |
| **C** | 포스터형 맵 선택과 맵 저장 | 맵 포스터, 입장 가능 여부, 사용자별 클리어 표시와 maps 조회를 맡는다. |
| **D** | 방 화면 그래픽과 클릭 처리 | 서랍·다이얼·문을 그리고 클릭 영역과 크기 변경 대응을 맡는다. |
| **E** | 퍼즐과 게임 규칙 | GameFacade, 게임 단계·아이템 상태, 암호 판정과 탈출 조건을 맡는다. |
| **F** | 단서·인벤토리·암호 입력 UI | 코드 종이, 아이템 선택, 네 자리/3×3 입력 화면을 맡는다. |
| **G** | SQLite·기록 저장·결과 화면 | DB 연결과 초기화, 퍼즐·클리어 기록 JDBC, 탈출 결과 표시를 맡는다. |

## 수정할 파일 목록

같은 패키지라도 담당자가 다를 수 있습니다. 특히 `JdbcPuzzleRepository.java`는 G, `PuzzleRepository.java`와 `Puzzle.java`는 E가 맡습니다.

| 담당 | 수정할 파일 (저장소 루트 기준) | 파일이 하는 일 |
| --- | --- | --- |
| **A** | `src/main/java/coderoom/app/AppController.java` | 로그인 이후의 화면 전환과 세션 연결 |
| **A** | `src/main/java/coderoom/app/Main.java` | 실행 진입점과 객체 조립 |
| **A** | `src/main/java/coderoom/app/MainFrame.java` | 앱에서 사용하는 하나의 최상위 창 |
| **A** | `src/main/java/coderoom/pages/game/GamePage.java` | 방 화면과 게임 조작의 연결 |
| **B** | `src/main/java/coderoom/entities/user/JdbcUserRepository.java` | users 테이블을 조회해 사용자 객체로 변환 |
| **B** | `src/main/java/coderoom/entities/user/LoginUser.java` | 로그인 후 사용할 사용자 ID·표시 ID만 보관 |
| **B** | `src/main/java/coderoom/entities/user/UserAccount.java` | 사용자 ID·로그인 ID·비밀번호 해시를 보관 |
| **B** | `src/main/java/coderoom/entities/user/UserRepository.java` | 로그인 ID로 사용자를 찾는 저장소 계약 |
| **B** | `src/main/java/coderoom/features/auth/LoginResult.java` | 로그인 성공 여부·안내 문구·로그인 사용자를 전달 |
| **B** | `src/main/java/coderoom/features/auth/LoginService.java` | 사용자 조회와 비밀번호 검증 조율 |
| **B** | `src/main/java/coderoom/features/auth/PasswordVerifier.java` | 비밀번호 해시 생성과 검증 |
| **B** | `src/main/java/coderoom/pages/login/LoginPage.java` | 로그인 폼 |
| **C** | `src/main/java/coderoom/entities/map/GameMap.java` | 맵 제목·설명·난이도·포스터·입장 가능 여부를 보관 |
| **C** | `src/main/java/coderoom/entities/map/JdbcMapRepository.java` | maps 테이블을 조회해 맵 객체로 변환 |
| **C** | `src/main/java/coderoom/entities/map/MapRepository.java` | 맵 목록·개별 맵 조회 저장소 계약 |
| **C** | `src/main/java/coderoom/features/mapcatalog/MapCard.java` | 포스터 표시 내용과 사용자별 클리어 기록을 전달 |
| **C** | `src/main/java/coderoom/features/mapcatalog/MapCatalogService.java` | 맵과 사용자별 최고 기록 결합 |
| **C** | `src/main/java/coderoom/pages/mapselect/MapSelectionPage.java` | 포스터형 맵 선택 화면 |
| **C** | `src/main/resources/images/posters/README.md` | 맵 포스터 이미지가 들어갈 위치와 경로 안내 |
| **D** | `src/main/java/coderoom/widgets/room/RoomPanel.java` | 방 그림과 물건 클릭 영역 |
| **D** | `src/main/resources/images/room/README.md` | 방 배경·장치 상태 이미지가 들어갈 위치와 경로 안내 |
| **E** | `src/main/java/coderoom/entities/game/GameSession.java` | 플레이 한 회의 메모리 상태와 전이 규칙 |
| **E** | `src/main/java/coderoom/entities/game/GameStage.java` | 시작·서랍 해제·다이얼 해제·탈출 단계를 구분 |
| **E** | `src/main/java/coderoom/entities/game/ItemId.java` | 종이 1·종이 2·열쇠 아이템을 구분 |
| **E** | `src/main/java/coderoom/entities/game/RoomObjectId.java` | 서랍·다이얼·문 등 클릭 가능한 물건을 구분 |
| **E** | `src/main/java/coderoom/entities/puzzle/AnswerCheck.java` | 퍼즐 입력의 형식 오류·오답·정답을 구분 |
| **E** | `src/main/java/coderoom/entities/puzzle/AnswerType.java` | 네 자리·3×3 암호의 입력 형식을 구분 |
| **E** | `src/main/java/coderoom/entities/puzzle/Puzzle.java` | 코드 퍼즐과 정답 판정 |
| **E** | `src/main/java/coderoom/entities/puzzle/PuzzleRepository.java` | 맵별 퍼즐을 순서대로 읽는 저장소 계약 |
| **E** | `src/main/java/coderoom/features/gameplay/EscapeResult.java` | 탈출 성공 여부·경과 시간·저장할 기록을 전달 |
| **E** | `src/main/java/coderoom/features/gameplay/GameFacade.java` | 게임 조작의 Facade 진입점 |
| **E** | `src/main/java/coderoom/features/gameplay/GameSetup.java` | 맵과 퍼즐 목록을 담아 게임 시작에 전달 |
| **E** | `src/main/java/coderoom/features/gameplay/GameView.java` | 화면에 보여줄 게임 단계·인벤토리·시간의 읽기 전용 상태 |
| **E** | `src/main/java/coderoom/features/gameplay/InspectResult.java` | 방 물건 조사 결과와 암호 입력창 표시 정보를 전달 |
| **E** | `src/main/java/coderoom/features/gameplay/ItemView.java` | 아이템 이름과 종이 내용·열쇠 설명을 화면에 전달 |
| **E** | `src/main/java/coderoom/features/gameplay/SubmitResult.java` | 암호 제출 판정·안내 문구·갱신된 화면 상태를 전달 |
| **F** | `src/main/java/coderoom/features/gameplay/ui/AnswerInputPanel.java` | 두 종류의 암호 입력 화면 |
| **F** | `src/main/java/coderoom/widgets/clue/CluePanel.java` | 왼쪽에 들고 있는 코드 종이 또는 열쇠 |
| **F** | `src/main/java/coderoom/widgets/inventory/InventoryPanel.java` | 하단 인벤토리 |
| **G** | `src/main/java/coderoom/entities/puzzle/JdbcPuzzleRepository.java` | puzzles 테이블을 조회해 퍼즐 객체로 변환 |
| **G** | `src/main/java/coderoom/entities/record/ClearRecord.java` | 사용자와 맵별 최고 탈출 시간을 보관 |
| **G** | `src/main/java/coderoom/entities/record/ClearRecordRepository.java` | 사용자별 기록 조회·최고 기록 저장 계약 |
| **G** | `src/main/java/coderoom/entities/record/JdbcClearRecordRepository.java` | clear_records 조회와 더 빠른 기록만 남기는 UPSERT |
| **G** | `src/main/java/coderoom/shared/db/Database.java` | SQLite 연결과 초기화 |
| **G** | `src/main/java/coderoom/shared/db/StorageException.java` | 저장소 장애를 상위 기능에 전달하는 예외 |
| **G** | `src/main/java/coderoom/widgets/result/ResultPanel.java` | 탈출 결과와 기록 저장 상태 |
| **G** | `src/main/resources/db/schema.sql` | users·maps·puzzles·clear_records 테이블 생성 SQL 자리 |
| **G** | `src/main/resources/db/seed.sql` | 시연 계정·맵·퍼즐 초기 데이터 SQL 자리 |

## 함께 확인할 경계

- A가 화면과 서비스를 연결한다. B~G는 각자 맡은 기능과 오류 상황을 직접 확인한다.
- D와 F의 위젯은 게임 규칙을 직접 바꾸지 않고 A의 GamePage에 사용자 이벤트를 전달한다.
- E가 게임 규칙과 퍼즐 저장소 계약을 정의하고, G가 JDBC 구현을 연결한다.
- `schema.sql`과 `seed.sql`은 현재 주석 초안이다. G가 작성할 때 B의 비밀번호 해시 형식, C의 맵 데이터, E의 퍼즐 형식과 맞춘다.
- 코드가 수정되면 PR에서 영향받는 담당자와 공개 API 계약을 확인한다.
