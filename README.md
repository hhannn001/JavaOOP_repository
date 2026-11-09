# JavaOOP_repository

Java Swing 기반 코딩 방탈출 게임의 설계 및 소스 초안입니다.

- [기획서](Document/기획서.md)
- [클래스 설계 및 7인 역할 분담](Document/클래스설계_및_7인_역할분담.md)
- [소스 구조와 작성 원칙](Document/소스구조.md)
- [A 임시 대시보드](Document/A_임시_대시보드.md)
- [PR 및 커밋 규칙](Document/PR_및_커밋_규칙.md)

Java 소스는 `src/main/java/coderoom`, 리소스는 `src/main/resources`에 있습니다.
A의 화면 전환과 D의 방 화면 일부가 구현되어 있습니다. B·C·E·F·G 기능은 아직 초안이므로 실제 게임은 플레이할 수 없습니다. Maven 빌드와 CI 설정은 포함되어 있습니다.

## 빌드 및 검사

JDK 17 이상에서 `./mvnw clean verify` (Windows: `mvnw.cmd clean verify`)로 Java 빌드·린트·테스트를 실행합니다.

- [CI·보안 검사·CodeRabbit 설치 안내](Document/CI_및_코드리뷰.md)

## 화면 전환 시연 실행

JDK 17 이상에서 프로젝트 루트에서 다음 명령을 실행합니다.

```sh
./mvnw compile
java -cp target/classes coderoom.app.Main
```

Windows에서는 `./mvnw` 대신 `mvnw.cmd`를 사용합니다. 시작 화면 → 임시 대시보드 → 맵 선택 → 서재 화면으로 이동하고, 맵 선택에서 대시보드로 돌아와 로그아웃할 수 있습니다. 대시보드의 로그인 정보·정답·진도 버튼은 `연동 전` 안내만 표시합니다. 로그인·퍼즐·기록은 작동하지 않는 시연 화면입니다.

## 테스트 개발 및 QA

- [파트별 테스트 스텁·드라이버 실행 및 실제 구현 연결](Document/Test_드라이버_가이드.md)
- [담당별 QA 시나리오와 완료 기준](Document/QA_체크리스트.md)

A의 화면 전환은 실제 컨트롤러와 스텁 화면으로 검증합니다. 로그인·퍼즐·SQLite QA는 각 기능 구현 후 별도로 진행합니다.
