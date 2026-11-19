# 방 선택 공간 에셋

Built-in Imagegen으로 생성. 기존 `dashboard/lobby/concept.png`의 픽셀 카페 스타일을 계승한다.

- `gallery.png`: 실제 목재 진열대 4열 × 액자 2개. 내부 포스터는 비워 두고 실제 맵 데이터로 합성한다.
- `detail-surface.png`: 현재 상세 팝업 배경. 포스터·정보·시작 버튼은 코드에서 배치한다. 이전 팝업 시안 파일은 정리했으며 아래 프롬프트는 생성 이력이다.

생성 프롬프트의 핵심 제약:

Gallery: "Actual tall wooden ladder-like wall racks bearing framed posters. FOUR vertical wooden display racks, each holding TWO large empty portrait poster frames. Same warm detailed pixel-art physical cafe interior, amber lamps, walnut wood, brass. All openings empty for real posters. NO game menu, UI cards, HUD or text."

Discarded physical-board direction: "Close frontal view of a physical wall-mounted room information display in that cafe. Left large brass-edged portrait poster frame; right dark walnut information board with title area, brass divider, description space, wide dark red wooden admission plaque below. Reference layout: left poster, right name/difficulty/description/start. NO BOOK, parchment, baked-in text or ratings."

좌표 기준: 1536 × 1024. 화면 크기에 맞춰 배경과 클릭 영역을 동일 비율로 축소한다. 비율이 다른 창은 레터박스로 보호한다. 목록은 8개 단위로 넘긴다. 준비 중 맵은 상세 조회만 허용한다. 인원·공포도처럼 데이터가 없는 사실은 만들지 않는다.

최종 팝업 프롬프트 (built-in Imagegen 편집):

"Preserve the exact flat popup UI composition: large empty left poster area, right title area, thin divider, metadata band, description space, wide bottom action button, small top right close area. Change ONLY the material palette to harmonize with the cafe dashboard: near-black warm espresso brown backdrop (#241b15), subtle dark brown paper texture, dark coffee metadata band and poster insert (#19120e), muted antique brass/golden ochre (#99733e) accents, warm walnut brown action button with thin brass edge. Leave all regions empty for programmatic cream text. Flat UI popup, not a physical scene. No beams, furniture, lamps, book or room. No text, icons, ratings or poster imagery. Wide 16:9."

상세는 같은 화면 위의 모달 오버레이로 표시한다. 배경을 어둡게 처리하고 팝업 중 진열대 입력을 차단한다. 닫기와 Escape는 진열대를 그대로 복원하고, 시작은 팝업을 닫은 뒤 게임으로 이동한다.

## 가독성 수정

`detail-surface.png`는 built-in Imagegen으로 기존 팝업 에셋을 편집했다.
프롬프트: "Remove all rectangles, poster block, metadata block, close square, divider and button. Leave a continuous subtle warm medium walnut-brown paper/leather texture around #60432e, substantially brighter, low contrast texture, no black vignette or shadows. No text, objects, shapes or decoration. Landscape 16:9."

구조는 코드에서 배치한다. 상세 포스터는 544×816 (원본 2:3), 오른쪽 정보의 시작점은 x=636, 버튼도 같은 정렬선이다. 배경 딤은 알파 75/255. 로비 복귀는 좌상단 화살표 버튼. 공통 런타임 글꼴은 프로젝트에 포함된 Pretendard-Regular.otf이며 제목은 굵게 표시한다.

## Imagegen 내비게이션·제목

- `back-button.png`: 투명 PNG. 프롬프트: "One isolated back-navigation button, dark walnut plaque with antique brass rim and a single cream left arrow. No words, frontal view, transparent background, compact 2:1 rectangle, readable at 80×40."
- `title-theme.png`: 투명 PNG. 프롬프트: "Exactly 테마고르기 in one horizontal line, five syllables 테 마 고 르 기. Warm ivory lettering, subtle antique gold edge, dark brown depth, clean bold Korean sans-serif akin to Pretendard Bold. Only glyphs, no plaque, border, backdrop or ornaments. Genuine alpha transparency."

두 에셋 모두 built-in Imagegen으로 생성. 텍스트 이름과 툴팁은 접근성을 위해 코드에 유지하며, 화면에는 이미지로 표시한다.
