package coderoom.widgets.clue;

import coderoom.entities.game.WorkshopRoom;
import java.util.stream.Collectors;

/** The equipment and the meaning of the saved controls for each supported puzzle. */
record RecordedPuzzleGuide(String title, String task, String solution) {
    static RecordedPuzzleGuide forPuzzle(long id, String code, String answer) {
        if (id == 1) {
            return new RecordedPuzzleGuide("서랍 잠금장치",
                    "코드의 출력에 맞춰 서랍의 숫자 바퀴를 왼쪽부터 돌리세요.", spaced(answer));
        }
        if (id == 2) {
            return new RecordedPuzzleGuide("벽 다이얼",
                    "코드의 출력 한 줄을 다이얼 한 행에 맞추세요.",
                    answer.matches("[0-9]{9}") ? answer.replaceAll("([0-9]{3})(?!$)", "$1 / ") : answer);
        }
        if (!(id >= 311 && id <= 315) && !(id >= 401 && id <= 405)) { return unknown(answer); }
        int roomId = (int) (id / 100);
        int step   = (int) (id % 10);
        WorkshopRoom room = WorkshopRoom.forMap(roomId);
        String[] guide = switch ((int) id) {
            case 311 -> new String[] {"정상 장비 중 전압이 기준 이상인 모듈을 연결하세요.", "연결할 모듈  " + spaced(answer)};
            case 312 -> new String[] {"검침값의 차이가 가장 큰 이음쇠에 같은 줄 표시의 슬리브를 끼우세요.",
                    answer.matches("[A-D]{2}") ? "이음쇠 " + answer.charAt(0) + "에 "
                            + pick(answer.charAt(1), "한 줄", "두 줄", "세 줄", "넓은 띠") + " 슬리브 끼우기" : answer};
            case 313 -> new String[] {"분배표를 보고 한도 안에서 가동할 정상 장비의 순서를 정하세요.",
                    code.contains("분배 작업표") ? "저장된 장비 순서  " + order(answer)
                            : "가동 순서  " + names(answer, "환풍기", "냉각기", "펌프", "예비 장치", "작업등")};
            case 314 -> new String[] {"안전 배출 기록에 맞춰 네 레버의 작동 순서를 정하세요.", "레버  " + order(answer)};
            case 315 -> new String[] {"왼쪽 가동등과 오른쪽 안전등에 필요한 신호를 연결하세요.",
                    code.contains("[입력]") || !answer.matches("[A-D]{4}") ? "저장된 연결 순서  " + spaced(answer)
                            : "왼쪽 " + names(answer.substring(0, 2), "전력", "밀폐", "부하", "압력")
                            + " / 오른쪽 " + names(answer.substring(2), "전력", "밀폐", "부하", "압력")};
            case 401 -> new String[] {"표본표의 조건을 만족하는 씨앗 봉투를 골라 수거하세요.", "남길 봉투  " + spaced(answer)};
            case 402 -> new String[] {"유입구부터 배수구까지 세 홈통의 높이를 맞춰 물길을 이으세요.",
                    code.contains("왼쪽 화분") ? "세 홈통을 맞춰 물을 낮은 왼쪽 화분까지 보내기 · " + spaced(answer)
                            : "왼쪽부터 홈통  " + names(answer, "높은 입구", "평평한 홈통", "낮은 출구")};
            case 403 -> new String[] {"가로·세로 발판을 옮기고 가운데를 연결해 레일 끝을 맞추세요.",
                    "가로 발판은 오른쪽, 세로 발판은 아래. 가운데에 연결판을 끼워 레일을 맞춥니다."};
            case 404 -> new String[] {"양쪽의 무게 × 거리가 같도록 오른쪽 고리에 추를 거세요.",
                    code.contains("seedWeight") || !answer.matches("[A-D]{2}") ? "저장된 배치  " + spaced(answer)
                            : "추 2는 " + (answer.charAt(0) - 'A' + 1) + "칸, 추 3은 "
                            + (answer.charAt(1) - 'A' + 1) + "칸 고리에 걸기"};
            case 405 -> new String[] {"선로의 침수와 정지턱을 확인해 운반차가 지나갈 방향을 정하세요.",
                    answer.matches("[AB]{4}") ? "점검: " + route(answer.substring(0, 2))
                            + "\n운반: " + route(answer.substring(2))
                            : legacyRoute(code, answer)};
            default -> throw new IllegalArgumentException("Unknown review puzzle " + id);
        };
        return new RecordedPuzzleGuide(room.task(step - 1).title(), guide[0], guide[1]);
    }

    private static RecordedPuzzleGuide unknown(String answer) {
        return new RecordedPuzzleGuide("문제 기록", "", answer);
    }

    private static String legacyRoute(String code, String answer) {
        if ("BA".equals(answer) && code.contains("takeHighTrack()") && code.contains("takeDoorBranch()")) {
            return "첫 분기에서 높은 선로, 두 번째 분기에서 출구 쪽 선로로 이동";
        }
        return "저장된 분기 순서  " + order(answer);
    }

    private static String route(String answer) {
        return pick(answer.charAt(0), "낮은 선로", "높은 선로")
                + " → " + pick(answer.charAt(1), "우회선로", "본선");
    }

    private static String names(String answer, String... labels) {
        return answer.chars().mapToObj(key -> (char) key + " " + pick((char) key, labels)).collect(Collectors.joining(" · "));
    }

    private static String pick(char key, String... labels) {
        int index = key - 'A';
        return index >= 0 && index < labels.length ? labels[index] : "";
    }

    private static String spaced(String value) { return String.join("  ", value.split("")); }
    private static String order(String value) { return String.join(" → ", value.split("")); }
}
