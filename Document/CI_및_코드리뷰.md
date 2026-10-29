# CI 및 코드 리뷰

## 실행 범위

검사 대상은 Java Swing 앱이다. `prototype/**`, `Document/**`, README, 다이어그램만 바뀐 PR에서는 CI를 실행하지 않는다. 프로토타입용 Node.js·ESLint·npm audit·npm Dependabot 설정은 사용하지 않는다.

| 검사 | 변경 감지 범위 | 실행 내용 |
| --- | --- | --- |
| Java build, test and lint | Java main/test 소스·리소스, pom.xml, .mvn/**, mvnw/mvnw.cmd, config/checkstyle.xml, ci.yml | Java 17에서 Maven verify: Checkstyle·컴파일·테스트·JAR 생성 |
| Analyze Java | Java main/test 소스, pom.xml, Wrapper, codeql.yml | Java CodeQL 보안 분석. 추출용 compile만 실행하며 린트·테스트 중복 실행 제외 |
| Dependency vulnerabilities | pom.xml, .github/workflows/** | PR에서 새로 추가되는 의존성의 high 이상 취약점 검사 |
| Dependabot | Maven·GitHub Actions | 매주 버전 업데이트 PR 생성 |

빌드와 CodeQL은 해당 경로가 바뀐 PR 및 main/dev push에 실행한다. CodeQL은 코드 변경이 없어도 새 탐지 규칙을 적용하기 위해 매주 월요일 실행한다. 수동 실행도 가능하다. Dependency Review는 PR에서만 실행한다.

예: Java 클래스 수정은 빌드+CodeQL, SQL/이미지 등 앱 리소스 수정은 빌드, pom.xml 수정은 세 검사 모두, 문서나 HTML 프로토타입 수정만 있으면 검사를 실행하지 않는다. 여러 범주의 파일이 함께 바뀌면 해당 검사를 합쳐 실행한다.

## 로컬 실행

JDK 17 이상에서 `./mvnw clean verify`를 실행한다. Windows에서는 `mvnw.cmd clean verify`를 사용한다. Wrapper가 Maven 3.9.11을 내려받으므로 Maven 별도 설치는 필요 없으며 최초 실행에는 인터넷이 필요하다.

현재 Java는 클래스 초안이다. 테스트와 main 실행 진입점이 아직 없으므로 빌드 성공은 게임 동작 검증을 의미하지 않는다. 테스트는 src/test/java에 추가한다. SQLite JDBC는 저장소 구현 때 추가한다.

Checkstyle은 와일드카드/미사용 import, 파일명, equals/hashCode, 문자열 비교를 검사한다. 세로 정렬을 변경하는 포매터는 적용하지 않는다. 현재 Swing/예외 초안의 serial 경고는 제외한다.

## 권한 및 저장소 설정

워크플로는 pull_request를 사용하며 checkout 자격 증명을 보관하지 않는다. 기본 토큰은 읽기 전용이고 CodeQL 업로드만 security-events: write를 사용한다. Actions는 SHA, Maven 플러그인은 버전을 고정한다.

2026-10-29에 Dependency graph를 활성화했다. Dependabot alerts·Secret scanning·Push protection은 별도 저장소 설정이며 YAML만으로 활성화되지 않는다. CodeQL Default setup과 이 Advanced workflow를 중복 사용하지 않는다.

경로 필터로 실행되지 않는 워크플로를 모든 PR의 필수 검사로 지정하면 문서 PR 등이 pending으로 막힐 수 있다. 현재 필수 검사 규칙은 변경하지 않는다. 추후 필수화할 때는 항상 실행하는 판정 작업과 조건부 검사 구조로 전환한다. CodeRabbit 완료 표시는 기능 테스트 성공을 뜻하지 않는다.

## CodeRabbit

2026-10-29에 hhannn001/JavaOOP_repository만 대상으로 GitHub App 설치를 확인했다. 한국어 리뷰, main/dev 대상 자동 리뷰, 초안 PR 제외로 설정한다. prototype/**와 생성된 다이어그램 이미지는 리뷰에서도 제외한다. 실제 리뷰와 CI 결과는 PR에서 확인한다.

공개 저장소 무료 리뷰 조건은 [공식 안내](https://www.coderabbit.ai/FAQ)를 따른다. Coding Agent 등 추가 서비스는 별도다. API 키를 저장소에 넣을 필요는 없다.
