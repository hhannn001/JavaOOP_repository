# 다이어그램 원본과 생성물

기준: **2026-11-16 현재 작업 트리**. 모든 그림은 현재 구현의 주요 관계를 요약한다. 모든 필드·메서드·import를 나열한 명세는 아니다.

| 그림 | 편집 원본 | 보기 |
| --- | --- | --- |
| 앱과 화면 조립 | [01_overview.mmd](01_overview.mmd) | [SVG](01_overview.svg) · [PNG](01_overview.png) |
| 게임 도메인 | [02_game_domain.mmd](02_game_domain.mmd) | [SVG](02_game_domain.svg) · [PNG](02_game_domain.png) |
| 저장소와 JDBC | [03_repository_adapter.mmd](03_repository_adapter.mmd) | [SVG](03_repository_adapter.svg) · [PNG](03_repository_adapter.png) |
| ERD | [04_erd.dot](04_erd.dot) | [SVG](04_erd.svg) · [PNG](04_erd.png) |
| 패키지 | [05_package.dot](05_package.dot) | [SVG](05_package.svg) · [PNG](05_package.png) |

게임 상태도와 제출·저장 시퀀스는 [클래스 구조 문서](../클래스설계_및_7인_역할분담.md)에 Mermaid로 관리한다.

## 재생성

Python 3과 Graphviz의 `dot`이 필요하다. 저장소 루트에서 실행한다.

```sh
python3 Document/diagrams/render.py
```

01~03은 `.mmd`가 원본이며 스크립트가 `.dot`을 생성한다. 이 생성 DOT를 직접 편집하지 않는다. 04~05는 `.dot`이 원본이다. 모든 DOT에서 SVG·PNG를 다시 만든다. 스크립트는 현재 사용하는 제한된 classDiagram 문법만 처리하며 지원하지 않는 문법은 오류로 알린다.

실선 화살표는 참조, 점선은 생성/호출 의존, 빈 삼각형은 인터페이스 구현, 채운 마름모는 소유, 빈 마름모는 집합을 뜻한다. ERD의 1 대 0..*는 외래키 관계이며 퍼즐 2개라는 게임 로드 조건과 구분한다.
