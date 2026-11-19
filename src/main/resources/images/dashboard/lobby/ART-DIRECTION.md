# CODE ROOM · 로비 에셋

승인된 방향: 세밀한 픽셀 아트 방탈출 카페 로비. 월넛·웜그레이·차콜·황동, 검은 벨벳 커튼. 밝은 민트/화이트 시안은 폐기.

## 현재 사용하는 에셋과 재생

- 내장 image_gen으로 제작·수정했다. 원본 프롬프트는 `generation-prompts.json`, 수정 프롬프트는 `repair-prompts.json`에 있다.
- `concept.png`: 승인된 전체 화면 시안.
- `background-v2.png`: 문틀·봉을 포함한 고정 배경. 방 선택하기 글자는 실제 간판 안에 그려져 있다. 오른쪽 포스터는 캘린더 자리를 위해 제거했다.
- `window.png`: 고정 창문. `window-leaves.png`에서 가지와 낙엽을 분리해 표시한다. 가지는 640ms 간격으로 천천히 왕복하며, 낙엽은 최초 10초 뒤 한두 장이 약 3.2초에 걸쳐 내려온다. 이후 10~18초 쉬고 다시 떨어진다. 40ms 타이머로 같은 낙엽 이미지를 이동하며 유리 밖은 그리지 않는다. 화면을 벗어나면 타이머를 중지한다. 프롬프트는 `window-animation-prompts.json`.
- `curtain.png`: 3열 × 2행 6프레임. 각 셀에서 천 영역 `(116, 94, 294, 362)`만 표시한다. 문틀과 봉을 덧그리지 않는다.
- `bookshelf.png`: 첫 프레임만 사용한다. 클릭하면 애니메이션 없이 미구현 안내창을 즉시 연다.
- `plaque-bookshelf.png`: 책장 중간 선반에 부착한 나무 표찰. 글자를 별도 입체 UI처럼 띄우지 않는다.
- `calendar.png`: 벽에 걸린 진척도 캘린더. 목재·바인더·제목은 첫 프레임으로 고정하고 종이 영역 `(84, 130, 354, 329)`만 재생한다. 이전 일지 사물 이미지는 정리했다.
- `leaderboard-main.png`: 로비에 놓인 실제 칠판. 제목과 구분선이 이미지 안에 포함되어 있다.
- `leaderboard.png`: DB 팝업의 배경판. `label-*.png`는 시작 화면·설정·팝업에서 사용하는 이미지 문구다.
- 좌표는 512 × 512 셀 기준이다. 커튼·캘린더는 프레임당 60ms, 마지막 프레임 뒤 콜백을 실행한다. 움직이는 모양을 도형으로 생성하거나 변형하지 않고 이미지 에셋의 해당 영역을 재생한다.
- 고정 사물의 픽셀이 프레임별로 달라지지 않는지 회귀 테스트로 확인한다. 사물의 종횡비는 유지하고, 커튼 천만 기존 입구 안쪽 크기에 맞춘다.
- 팝업이 열려 있는 동안 모든 사물의 강조를 억제한다. 닫힌 뒤 포커스 복귀나 합성 mouse-enter로 강조가 남지 않고 실제 포인터 이동/키보드 탐색 때 다시 켜진다.
- 커튼은 호버 강조를 사용하지 않는다. 공통 이미지 버튼은 클릭 포커스 사각 테두리를 그리지 않는다.
- 이름·시간·순위·조회 상태 등 실행 중 바뀌는 DB 정보는 동적 텍스트다.

## 승인 시안 프롬프트

Use attached pixel scene as the STRICT rendering-style reference: convincing authored pixel-art adventure-game environment, many carefully placed small details, consistent pixel grid, readable clusters, subtle stepped wood grain, layered lighting drawn with discrete pixel colors. Create a DIFFERENT environment: a tasteful modern Korean escape-room cafe LOBBY, front-facing side-on point-and-click room composition, landscape 16:10. Mature walnut wood, warm grey plaster, charcoal, muted olive and aged brass; warm ambient light, welcoming but substantial, not white pastel cafe and not medieval fantasy. At CENTER is a tall wide passage concealed by CLOSED BLACK velvet CURTAINS, in a walnut surround, brass curtain rail; realistic fine pixel folds, absolutely no door. LEFT: broad amber-lit street window, below it low walnut archive bookcase with many individually detailed books, labels and a plant. RIGHT: standing charcoal/brass leaderboard display with a blank interior, farther right a small modern reception desk with open logbook, desk bell, a small monitor and wall theme posters. Wooden floor, pendant or track lamps, warm localized pools of light, enough negative space between interactive objects. At top center a modest blank business sign. No people. No text or letters anywhere (all lettering will be separate assets). The detail density must match reference: small books, corners, stems, trim, curtain folds, page lines drawn with actual crisp square pixels. Avoid icon-like oversized geometric objects, vector illustration, smooth gradient washes, airbrush, bevels, photorealism. Do not reuse the reference room layout or contents literally.
