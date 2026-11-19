# Java 앱 이미지 리소스

기준: 2026-11-16 현재 작업 트리. Swing은 클래스패스 `/images/...`에서 이미지를 읽는다. 폴더별 README의 생성 프롬프트와 QA는 제작 이력이며 앱 전체 QA 완료를 뜻하지 않는다.

| 경로 | 현재 용도 |
| --- | --- |
| `dashboard/lobby/` | 픽셀 카페 로비 배경·사물·제목과 장식 |
| `dashboard/cat/` | 고양이 반응·보행·휴식 프레임, 벨·카운터 |
| `mapselect/` | 맵 진열대·상세 오버레이·뒤로가기·제목 |
| `posters/room01_study_cipher.png` | 서재의 마지막 암호 포스터 |
| `posters/room02_lost_records.png` | 사라진 기록 포스터 (준비 중) |
| `posters/room03_midnight_engine.png` | 자정의 기계실 포스터 (준비 중) |
| `room/study_locked.png` | 첫 방 배경. 별도 클릭 영역과 상태 표시를 얹는다 |
| `game-ui/` | 단서 종이·서랍·3×3 다이얼의 물체 이미지 |
| `app/` | 앱 아이콘 |

현재 기본 포스터 경로는 `db/seed.sql`이 관리한다. 기존 영문 포스터는 호환용으로 남겨두고 초기 `inventory/`, `clue/` 이미지는 미사용 리소스로 제거했다. 현재 아이템 표시는 `ItemSprite`, 단서·입력 물체는 `PixelTheme`과 각 위젯이 담당한다. 포스터에는 한글 제목이 포함되며 암호 입력값·힌트·클릭 영역은 Swing 컴포넌트로 처리한다.

배경과 클릭 영역은 같은 좌표 변환을 사용해야 한다. 실제 상태별 표시와 접근성·리사이즈는 [QA 체크리스트](../../../../Document/QA_체크리스트.md)에서 확인한다.
