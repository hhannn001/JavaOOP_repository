# 방 리소스

모든 그림은 1586×992 크기이고, 클릭 영역도 이 좌표를 기준으로 한다.

| 파일 | 용도 |
| --- | --- |
| `study_locked.png` | 시작 상태 배경 |
| `drawer_open.png` | 서랍이 열린 뒤(DRAWER_OPEN 이상) 배경 위에 겹침 |
| `dial_solved.png` | 다이얼이 풀린 뒤(SAFE_OPEN 이상) 겹침 |
| `door_open.png` | 탈출 성공(ESCAPED) 때 겹침 |

겹침 그림은 투명 PNG이며 `RoomPanel`이 읽는다. 파일이 없으면 그 단계 그림만 생략하고 로그를 남긴다.
