# 한글 테마 포스터

서재와 기계실은 `src/main/resources/db/seed.sql`, 온실은 `src/main/resources/db/workshops.sql`에서 포스터를 지정한다.
앱 시작 시 이전 영문 기본 테마를 갱신하며 계정, 기록, 퍼즐, 플레이 가능 여부, 힌트 제한은 유지한다. 사용자 정의 이름의 맵은 덮어쓰지 않는다.

| 방 | 주제 | 난이도 | 포스터 | 상태 |
| --- | --- | --- | --- | --- |
| 서재의 마지막 암호 | 추리 · 암호 | 보통 | room01_study_cipher.png | 입장 가능 |
| 새벽 온실의 마지막 씨앗 | 조건 분기 · 물리 퍼즐 | 보통 | room02_last_seed.png | 입장 가능 |
| 자정의 기계실 | 기계 · 모험 | 어려움 | room03_midnight_engine.png | 입장 가능 |

## 생성 방식과 프롬프트

Built-in Imagegen. 2:3 세로 포스터, 큰 한글 제목과 한 줄 소개 포함. 사용자 제공 방탈출 포스터들의 제목·소개 중심 구성을 참고한 새 이미지다. 요금, 제한시간, 인원, 평점은 임의로 추가하지 않았다.

공통 프롬프트: Original professional Korean escape-room cinematic poster, portrait 2:3, photographic illustration and worn print texture, not pixel art. Large legible integrated Korean title in lower third, exact subtitle below, small genre line at top. No English, logos, ratings, timing, prices or extra copy. Safe print margins and edge-to-edge art.

1. 서재의 마지막 암호: Mysterious antique study at midnight, rainy window, brass key on coded notebook, locked drawer and brass dial, forest green and amber. Title '서재의 마지막 암호'. Subtitle '책장이 닫히기 전, 마지막 암호를 풀어라.' Genre '추리 · 암호'.
2. 새벽 온실의 마지막 씨앗. Rainy greenhouse at dawn, a lit seed case and specimen card, flooded rails, and a raised exit. Title '새벽 온실의 마지막 씨앗'. Subtitle '물이 차오르기 전에 씨앗을 옮겨라.' Genre '탐색 · 조작'.
3. 자정의 기계실: Abandoned underground machinery, enormous brass gears, clock stopped at midnight, red emergency light, steel-blue steam and half-open industrial door. Title '자정의 기계실'. Subtitle '멈춘 톱니가 움직이면, 출구가 열린다.' Genre '기계 · 모험'.

기존 포스터 파일은 호환을 위해 보관하되 기본 테마와 로비 미리보기는 위 신규 파일을 참조한다.
