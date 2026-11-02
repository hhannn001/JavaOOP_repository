# JavaOOP_repository

Java Swing 기반 코딩 방탈출 게임의 설계 및 소스 초안입니다.

- [기획서](Document/기획서.md)
- [클래스 설계 및 7인 역할 분담](Document/클래스설계_및_7인_역할분담.md)
- [소스 구조와 작성 원칙](Document/소스구조.md)
- [PR 및 커밋 규칙](Document/PR_및_커밋_규칙.md)

Java 소스는 `src/main/java/coderoom`, 리소스는 `src/main/resources`에 있습니다.
현재는 타입 선언과 상속·구현 관계만 작성된 초안입니다. 필드·메서드·생성자·SQL은 주석에 적힌 내용을 바탕으로 각 담당자가 구현합니다. 실행 가능한 앱은 아직 없습니다. Maven 빌드와 CI 설정은 포함되어 있습니다.

## 빌드 및 검사

JDK 17 이상에서 `./mvnw clean verify` (Windows: `mvnw.cmd clean verify`)로 Java 빌드·린트·테스트를 실행합니다.

- [CI·보안 검사·CodeRabbit 설치 안내](Document/CI_및_코드리뷰.md)

## 테스트 개발 및 QA

- [파트별 테스트 스텁·드라이버 실행 및 실제 구현 연결](Document/Test_드라이버_가이드.md)
- [담당별 QA 시나리오와 완료 기준](Document/QA_체크리스트.md)

현재 테스트는 테스트 전용 대역을 검증합니다. 실제 Java 기능·Swing·SQLite QA는 구현 후 별도로 진행합니다.
