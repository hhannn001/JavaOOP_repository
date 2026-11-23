# 방 리소스

방 배경과 서재 겹침 이미지는 1586×992이며, 클릭 영역도 이 좌표를 기준으로 한다. 개별 장치와 메모는 투명 PNG로 따로 배치한다.

| 파일 | 용도 |
| --- | --- |
| `study_locked.png` | 시작 상태 배경 |
| `drawer_open.png` | 서랍이 열린 뒤(DRAWER_OPEN 이상) 배경 위에 겹침 |
| `dial_solved.png` | 다이얼이 풀린 뒤(SAFE_OPEN 이상) 겹침 |
| `door_open.png` | 탈출 성공(ESCAPED) 때 겹침 |

겹침 그림은 투명 PNG이며 `RoomPanel`이 읽는다. 파일이 없으면 그 단계 그림만 생략하고 로그를 남긴다.

기록보관소와 기계실은 각각 `archive.png`, `engine.png`를 사용한다. 장치는 `archive_*`와 `engine_*` 에셋에서 상태별 그림을 선택한다.
