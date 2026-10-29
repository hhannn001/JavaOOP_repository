# CI 및 코드 리뷰

## 로컬 실행

JDK 17 이상을 설치한다. Maven은 Wrapper가 지정 버전(3.9.11)을 내려받으므로 별도 설치하지 않아도 된다. 최초 실행에는 인터넷이 필요하다.

```sh
./mvnw clean verify
npm ci --ignore-scripts
npm run lint
npm audit --audit-level=high
```

Windows에서는 `mvnw.cmd clean verify`를 실행한다. CI의 기준 환경은 JDK 17 / Node.js 22다.

`verify`는 Checkstyle, Java 컴파일 경고 검사, JUnit 테스트, JAR 생성을 수행한다. 테스트는 `src/test/java`에 추가한다. 현재 Java는 클래스 초안으로 테스트와 실행 진입점이 아직 없으므로, 빌드 성공을 게임 동작 검증으로 해석하지 않는다. SQLite JDBC 의존성은 저장소 구현 시 `pom.xml`에 추가한다.

## 자동 검사

| 검사 | 실행 시점 | 역할 |
| --- | --- | --- |
| Java build, test and lint | 모든 PR, main/dev push | Maven verify 및 컴파일 경고를 오류로 처리 |
| Prototype lint and dependency audit | 모든 PR, main/dev push | ESLint 오류 및 npm 의존성 high 이상 취약점 차단 |
| CodeQL | 모든 PR, main/dev push, 매주 월요일 | Java·JavaScript 정적 보안 분석, Security 탭에 결과 게시 |
| Dependency vulnerabilities | 모든 PR | 새로 추가되는 Maven/npm 등의 취약 의존성 중 high 이상 차단 |
| Dependabot | 매주 | Maven·npm·GitHub Actions 업데이트 PR 생성 |

Checkstyle은 와일드카드/미사용 import, 파일명, equals/hashCode, 문자열 비교를 검사한다. 세로 정렬을 바꾸는 자동 포매터는 적용하지 않는다. Java 직렬화를 사용하지 않는 Swing/예외 초안에 한해 `serial` 컴파일 경고는 제외한다. CodeQL 알림의 병합 차단 여부는 GitHub 규칙 설정에 따라 별도로 결정된다.

워크플로는 `pull_request`를 사용하고 checkout 자격 증명을 보관하지 않는다. 기본 토큰은 읽기 전용이며 CodeQL 결과 업로드 작업만 `security-events: write`를 받는다. 액션은 커밋 SHA, npm은 lockfile, Maven 플러그인은 버전을 고정한다.

## CodeRabbit 무료 리뷰 연결

공개 저장소는 CodeRabbit의 무료 오픈소스 리뷰 대상이다. 이 저장소는 공개 상태로 확인했다. 비공개로 바꾸면 같은 무료 리뷰 조건이 유지되지 않을 수 있다. [공식 무료 이용 안내](https://www.coderabbit.ai/FAQ)

1. 저장소 관리자가 [CodeRabbit GitHub App](https://github.com/apps/coderabbitai)을 설치하고 `hhannn001/JavaOOP_repository`를 선택한다.
2. `.coderabbit.yaml`을 포함한 변경을 GitHub에 올린다. 설정은 한국어 리뷰, main/dev 대상 자동 리뷰, 초안 PR 제외다.
3. PR에서 실제 리뷰가 게시되는지 확인한다. 기존 PR은 `@coderabbitai review`로 요청할 수 있다.

2026-10-29에 GitHub App을 이 저장소만 대상으로 설치했으며, 저장소의 Installed GitHub Apps에서 확인했다. 원격 CI 및 실제 PR 리뷰 결과는 각 PR의 Checks와 리뷰 댓글에서 확인한다. API 키를 저장소나 GitHub Actions Secrets에 넣을 필요는 없다.

## 저장소 관리자 설정

- Settings → Actions에서 워크플로 실행을 허용한다.
- Settings → Security에서 Dependency graph와 Dependabot alerts, Secret scanning 및 Push protection을 켠다. 이 항목들은 YAML 파일로 자동 활성화되지 않는다.
- CodeQL Default setup이 이미 켜져 있다면 이 워크플로의 Advanced setup과 중복되지 않게 전환한다.
- 첫 실행 성공 후 main/dev 규칙에 Java·Prototype·Dependency Review 검사를 필수로 지정한다. 사람 리뷰 1명도 유지한다.
- CodeRabbit 의견은 사람이 판단하고 반영한다. AI 리뷰 완료 자체를 테스트 통과로 보지 않는다.

참고: [CodeRabbit 설정](https://docs.coderabbit.ai/reference/configuration), [Dependency Review](https://github.com/actions/dependency-review-action), [CodeQL 워크플로](https://docs.github.com/en/code-security/reference/code-scanning/workflow-configuration-options).
