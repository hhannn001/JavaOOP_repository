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
HTML 프로토타입은 Node.js 22에서 `npm ci --ignore-scripts` 후 `npm run lint`로 검사합니다.

- [CI·보안 검사·CodeRabbit 설치 안내](Document/CI_및_코드리뷰.md)
