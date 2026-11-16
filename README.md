# JavaOOP_repository

Java Swing과 SQLite 기반 코딩 방탈출 게임입니다.

- [기획서](Document/기획서.md)
- [클래스 설계 및 7인 역할 분담](Document/클래스설계_및_7인_역할분담.md)
- [소스 구조와 작성 원칙](Document/소스구조.md)
- [A 대시보드 구현 현황](Document/A_임시_대시보드.md)
- [PR 및 커밋 규칙](Document/PR_및_커밋_규칙.md)

Java 소스는 `src/main/java/coderoom`, 리소스는 `src/main/resources`에 있습니다.
DB 로그인, 맵 조회, 첫 번째 방의 퍼즐 2개, 아이템 획득, 탈출 및 최고 기록 저장이 연결되어 있습니다. 나머지 맵 2개는 준비 중입니다.

## 빌드 및 검사

JDK 17에서 `./mvnw clean verify` (Windows: `mvnw.cmd clean verify`)로 Java 빌드·린트·테스트를 실행합니다.

- [CI·보안 검사·CodeRabbit 설치 안내](Document/CI_및_코드리뷰.md)

## 실행 및 DB 초기화

JDK 17에서 프로젝트 루트에서 다음 명령을 실행합니다.

```sh
./mvnw package
java -cp "target/classes:target/dependency/*" coderoom.app.Main
```

Windows에서는 `mvnw.cmd package`로 빌드한 뒤 아래처럼 클래스패스 구분자로 `;`를 사용합니다.

```bat
java -cp "target/classes;target/dependency/*" coderoom.app.Main
```

첫 실행 시 프로젝트 실행 디렉터리의 `coderoom.db`를 생성하고 스키마와 시드 데이터를 넣습니다. 재실행해도 기존 계정·퍼즐·최고 기록을 덮어쓰지 않습니다. DB 파일은 Git에서 제외하고 SQL 시드를 커밋합니다.

| 계정 | 비밀번호 | 용도 |
| --- | --- | --- |
| `test` | `test1234` | 로컬 테스트 |
| `demo1`, `demo2`, `demo3` | `test-only` | 계정별 기록 분리 시연 |

계정은 SQLite에 저장되며 비밀번호는 PBKDF2 해시로 보관합니다. 맵 3개, 첫 방 퍼즐 2개를 함께 준비하고 클리어 기록은 실제 탈출 시 생성합니다.

화면 없이 데이터만 준비하려면 다음 명령을 실행합니다.

```sh
java -cp "target/classes:target/dependency/*" coderoom.app.Main --init-db
```

다른 DB 파일을 쓰려면 `java -Dcoderoom.db=/absolute/path/game.db ...`로 경로를 지정합니다.

플레이 순서: 로그인 → Enter → 방 선택 → 첫 번째 방 입장 → 메모 1 확인 → 서랍 암호 입력 → 메모 2 확인 → 벽 다이얼 입력 → 열쇠 선택 → 문 클릭. 탈출 결과에서 저장 상태, 재시도, 다시 시작, 맵 목록 복귀를 확인할 수 있습니다. 맵 목록에는 계정별 최고 기록이 표시됩니다.

- [DB·게임 연동 범위와 검증](Document/DB_게임_연동.md)

## 테스트 개발 및 QA

- [파트별 테스트 스텁·드라이버 실행 및 실제 구현 연결](Document/Test_드라이버_가이드.md)
- [담당별 QA 시나리오와 완료 기준](Document/QA_체크리스트.md)

화면 수명·저장 실패 재시도는 스텁 테스트로, 계정·퍼즐·기록 저장은 임시 SQLite 파일을 사용하는 통합 테스트로 검증합니다. 디스플레이가 없는 환경에서는 `./mvnw -Djava.awt.headless=true verify`를 사용하며 실제 창 테스트 2개는 제외됩니다.

## 진행 중인 변경

현재 작업 트리에는 픽셀 카페 로비·사물 애니메이션, DB 기반 리더보드·방별 기록 조회, 단서·암호 입력·인벤토리 픽셀 UI가 포함되어 있습니다. 개별 문제 풀이 이력은 아직 저장하지 않으며, 맵별 힌트 허용 횟수는 DB 저장·조회만 연결되어 UI 차감은 미구현입니다.

상세 구현 범위와 남은 작업은 [대시보드](Document/A_임시_대시보드.md), [DB·게임 연동](Document/DB_게임_연동.md), [소스 구조](Document/소스구조.md)를 확인하세요.
