# 게임 대사창

내장 image_gen으로 생성한 RGBA 에셋. 종이 시안은 사용하지 않는다.
대시보드 `images/dashboard/lobby/label-settings.png`를 한글 스타일 참조로 사용했다.

- `panel.png`: 차콜색 시스템 창. 제목 줄과 본문 영역, 금색 픽셀 테두리.
- `player.png`: 이미지에 포함된 한글 ‘플레이어’.
- `system.png`: 이미지에 포함된 한글 ‘시스템’.

대사 본문은 `DialoguePanel`의 동적 텍스트다. 게임 화면 어디든 클릭하면 현재 대사를 넘기고 마지막 대사에서 닫는다. Enter·Space·Escape로도 진행한다.
대사 클릭은 아래 퍼즐·인벤토리에 전달되지 않는다. 퍼즐을 열면 배경을 어둡게 하고 HUD·인벤토리·메모를 감추며, 닫으면 복원한다.
새 조사 시 최신 대사로 교체한다. 게임 타이머는 대사 중에도 진행한다.
도입은 게임을 새로 시작할 때 한 번 재생하고, 반복 렌더링으로 재시작하지 않는다.

## 최종 생성 프롬프트

### panel.png
Use case: ui-mockup. Production 2D pixel-art game SYSTEM DIALOG WINDOW, wide rectangular aspect 4:1, tightly framed on transparent background. Mature charcoal dark grey opaque interior, thin warm muted brass double pixel border, precise straight edges with small stepped corners, restrained top titlebar separated by fine line, clean consistent pixel grid. Large empty interior for Korean runtime dialogue. Modern retro game system window matching walnut/charcoal escape cafe. No text, no icons, no paper, no parchment, no torn edges, no ornaments, no rivets, no scene. Flat front view.

### player.png
Use supplied image ONLY as typography style reference. Create standalone transparent Korean pixel display lettering. Exact text "플레이어". Warm ivory face, muted gold edge, subtle dark-brown pixel extrusion exactly matching reference Korean dashboard lettering. One horizontal line, no signboard, no background, no glow. Tight small transparent margins; do not change syllables. Four clear Korean syllables 플 레 이 어. Output asset used as speaker name in charcoal system dialogue title bar.

### system.png
Use supplied image ONLY as typography style reference. Create standalone transparent Korean pixel display lettering. Exact text "시스템". Warm ivory face, muted gold edge, subtle dark-brown pixel extrusion exactly matching reference Korean dashboard lettering. One horizontal line, no signboard, no background, no glow. Tight small transparent margins; do not change syllables. Three clear Korean syllables 시 스 템. Output asset used as speaker name in charcoal system dialogue title bar.
