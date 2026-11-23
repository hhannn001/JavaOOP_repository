package coderoom.features.gameplay;

import java.util.Map;
import java.util.regex.Pattern;

/** Localizes the visible note after the current variant has been applied. */
final class ClueText {
    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

    private ClueText() { }

    static String korean(long puzzleId, String code) {
        if (puzzleId < 1 || puzzleId > Integer.MAX_VALUE) { return code; }
        Map<String, String> labels = switch ((int) puzzleId) {
            case 1 -> Map.of("steps", "변화량", "step", "한단계", "total", "누적합");
            case 2 -> Map.of("grid", "숫자표", "row", "행", "col", "열");
            case 311 -> Map.of("Module", "모듈", "modules", "모듈목록", "m", "모듈하나",
                    "ok", "정상", "voltage", "전압", "connect", "연결");
            case 313 -> Map.of("Device", "장비", "queue", "장비순서", "d", "장비하나",
                    "left", "남은전력", "ok", "정상", "watts", "사용전력", "turnOn", "켜기");
            case 315 -> Map.of("running", "가동등켜짐", "safe", "안전등켜짐",
                    "powerOk", "전력정상", "loadOk", "부하정상", "pressureOk", "압력정상", "sealed", "밀폐완료");
            case 401 -> Map.of("leaf", "잎모양", "state", "손상상태", "stamp", "도장",
                    "validShape", "선별조건충족", "reject", "제외", "keep", "남기기", "sample", "봉투");
            case 404 -> Map.of("lightSlot", "가벼운추위치", "heavySlot", "무거운추위치", "left", "왼쪽",
                    "right", "오른쪽", "torque", "회전력", "weight", "무게", "slot", "거리", "balanced", "균형");
            case 405 -> Map.of("flooded", "낮은선로침수", "bufferAhead", "본선정지턱있음",
                    "highTrack", "높은선로", "lowTrack", "낮은선로", "sideTrack", "우회선로", "mainTrack", "본선");
            default -> Map.of();
        };
        if (labels.isEmpty()) { return code; }
        String text = WORD.matcher(code).replaceAll(word -> labels.getOrDefault(word.group(), word.group()));
        if (puzzleId == 405) {
            return "낮은 선로에 물이 차 있으면 높은 선로로 간다.\n"
                    + "본선 앞에 정지턱이 있으면 우회선로로 간다.\n\n" + text;
        }
        if (puzzleId == 401) {
            return text.replaceFirst("봉투의 표식을[^\\n]*", "봉투마다 잎 모양, 손상 상태, 도장을 왼쪽부터 읽는다.");
        }
        if (puzzleId == 404) {
            return text.replaceFirst("[^\\n]*고리 번호다\\.", "무게 2와 3인 추를 각각 어느 고리에 걸지 정한다.");
        }
        return text;
    }
}
