# 인게임 물체 UI

완성된 조사 화면의 구현과 재사용 규칙을 정리한다. 경로는 저장소 루트 기준이다.

## 물체 에셋과 공통 렌더링

- `src/main/resources/images/game-ui/`의 `drawer.png`, `dial.png`, `paper.png`는 새로 생성한 투명 물체 일러스트다. 방 장면을 잘라 확대하거나 일반 패널 프레임으로 대체한 이미지가 아니다.
- 생성 도구와 원본 프롬프트는 같은 폴더의 [README](../src/main/resources/images/game-ui/README.md)에 기록되어 있다. 숫자·본문·버튼·클릭 영역은 이미지에 굽지 않고 Swing으로 표시한다.
- `src/main/java/coderoom/shared/ui/PixelTheme.java`는 Pretendard 공통 한글 글꼴, 잉크·종이 색상, 공통 버튼, 에셋 캐시와 nearest-neighbor 렌더링을 제공한다. 새 물체 UI도 이 진입점을 재사용한다.
- `src/main/java/coderoom/shared/ui/ItemSprite.java`는 메모와 열쇠를 직접 그리는 공통 `Icon`이다. 인벤토리 외 화면에서도 같은 아이콘을 재사용할 수 있다.

## 고정 오버레이 슬롯

`src/main/java/coderoom/shared/ui/GameOverlayLayout.java`가 방 위에 `header`, `clue`, `input`, `inventory`, `result` 슬롯을 배치한다. 메모는 왼쪽, 입력 물체는 그 오른쪽 영역 중앙, 인벤토리는 하단 중앙에 놓인다. 패널을 열어도 방 자체의 크기는 바뀌지 않는다.

슬롯 위치는 창 크기에 맞춰 계산되며, 권장 크기는 1280×800, 최소 크기는 900×650이다. 새 패널을 연결할 때는 `src/main/java/coderoom/app/AppScreens.java`의 슬롯 연결 방식을 따르고 개별 화면에서 별도 배치 좌표를 중복 관리하지 않는다.

## 메모와 힌트

`src/main/java/coderoom/widgets/clue/CluePanel.java`는 종이 에셋 위에 읽기 전용 코드와 힌트를 표시한다. 힌트는 처음에 9×9 블러로 가려지며 클릭하면 공개된다. 빈 힌트는 표시하지 않는다.

공개한 메모의 `ItemId`를 패널 내부 `EnumSet`에 보관하므로 같은 게임에서 메모를 접거나 다른 메모로 바꿨다가 돌아와도 공개 상태가 유지된다. 새 게임은 새 `GameWidgets`와 `CluePanel`을 생성하므로 상태가 초기화된다. 이 상태는 메모리 내 게임 단위 상태이며 DB 저장이나 앱 재실행 후 복원은 구현하지 않았다.

## 인벤토리와 입력

`src/main/java/coderoom/widgets/inventory/InventoryPanel.java`는 Minecraft에서 영감을 받은 다섯 칸의 핫바다. 선택된 칸은 밝은 테두리로 표시하며, 빈 칸은 비활성화한다. 마우스 클릭, 숫자열 `1`–`5`, 숫자 키패드 `1`–`5`로 아이템을 선택한다. 열쇠를 선택할 때는 선택 표시만 갱신하고 열려 있던 메모를 닫는다. 메모 창은 NOTE1·NOTE2에만 사용한다. 포커스가 `JTextComponent`에 있으면 단축키 선택을 건너뛰어 암호 입력을 방해하지 않는다.

`src/main/java/coderoom/features/gameplay/ui/AnswerInputPanel.java`는 서랍의 네 자리 암호와 벽의 3×3 다이얼을 담당한다. 숫자는 투명 텍스트 필드로 물체 위에 배치하며, 직접 입력과 클릭 회전을 지원한다. 왼쪽 클릭은 한 칸 증가, 오른쪽 클릭은 한 칸 감소하며 0–9를 순환한다. Enter 또는 제출 버튼으로 콜백을 전달한다. UI는 입력 형식만 검사하고 정답 판정은 게임 로직에 맡긴다.

## 힌트 제한: 설정 준비만 완료

`src/main/resources/db/schema.sql`의 `maps.hint_limit`은 `INTEGER NOT NULL DEFAULT 2 CHECK (hint_limit >= 0)`로 선언되어 있다. `src/main/java/coderoom/shared/db/Database.java`는 기존 DB에서 해당 열의 존재를 확인한 뒤, 없을 때만 추가한다. 기존 맵·계정·기록은 유지된다.

`src/main/java/coderoom/entities/map/JdbcMapRepository.java`가 이 값을 읽어 `GameMap.getHintLimit()`으로 제공한다. **현재 실제 힌트 사용 횟수 제한은 적용하지 않는다.** 기본값 2는 준비된 설정값이며, 사용 횟수 차감·제한 도달 시 차단·공개 상태 DB 저장까지 구현된 것으로 해석하면 안 된다.
