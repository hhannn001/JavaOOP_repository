package coderoom.features.gameplay;

import coderoom.entities.puzzle.Puzzle;
import coderoom.shared.ui.SpecimenMark;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongFunction;

/** A matched set of clues and device settings for one attempt. */
public enum PuzzleVariant {
    STANDARD, COPPER, BRASS;

    private static final int[] VOLTAGES = {15, 24, 29, 34, 11};
    private static final int[] WATTS    = {20, 40, 35, 20, 15};
    private static final String SLEEVE_BY_JOINT = "ACBA";

    public static LongFunction<PuzzleVariant> rotatingDeck() {
        return new LongFunction<>() {
            private final Map<Long, PuzzleVariant> previous = new HashMap<>();

            @Override public synchronized PuzzleVariant apply(long mapId) {
                PuzzleVariant[] deck = values();
                PuzzleVariant last   = previous.get(mapId);
                int choice = ThreadLocalRandom.current().nextInt(deck.length - (last == null ? 0 : 1));
                if (last != null && choice >= last.ordinal()) { choice++; }
                previous.put(mapId, deck[choice]);
                return deck[choice];
            }
        };
    }

    public static int moduleVoltage(int index) { return VOLTAGES[index]; }
    public static int deviceWatts(int index) { return WATTS[index]; }
    public static boolean operational(int index) { return index != 1; }

    public boolean highGutterInlet() { return this != COPPER; }
    public boolean lowGutterOutlet() { return this != BRASS; }

    public int[][] sleeveReadings() {
        return switch (this) {
            case STANDARD -> new int[][] {{18, 29, 48, 82}, {31, 44, 81, 93}};
            case COPPER -> new int[][] {{21, 37, 52, 76}, {57, 53, 72, 88}};
            case BRASS -> new int[][] {{16, 31, 51, 74}, {28, 70, 73, 90}};
        };
    }

    public int powerThreshold() { return switch (this) {
        case STANDARD -> 15;
        case COPPER -> 30;
        case BRASS -> 10;
    }; }

    public int distributionCapacity() { return switch (this) {
        case STANDARD -> 70;
        case COPPER -> 55;
        case BRASS -> 60;
    }; }

    public String distributionQueue() { return switch (this) {
        case STANDARD -> "CBADE";
        case COPPER -> "EBDAC";
        case BRASS -> "ABECD";
    }; }

    public String pressureOrder() { return switch (this) {
        case STANDARD -> "DCAB";
        case COPPER -> "BDAC";
        case BRASS -> "CADB";
    }; }

    public int pressureDrop(char lever) {
        return switch (pressureOrder().indexOf(lever)) {
            case 0 -> 15;
            case 1 -> 30;
            case 2 -> 10;
            case 3 -> 35;
            default -> throw new IllegalArgumentException("Unknown pressure lever " + lever);
        };
    }

    public int balanceLoad() { return switch (this) {
        case STANDARD -> 13;
        case COPPER -> 9;
        case BRASS -> 17;
    }; }

    public boolean railFlooded() { return this != COPPER; }
    public boolean railBlocked() { return this != BRASS; }

    private static final SpecimenMark[][] SPECIMENS = {
        {SpecimenMark.ROUND, SpecimenMark.DAMAGED, SpecimenMark.TRIANGLE},
        {SpecimenMark.SERRATED, SpecimenMark.INTACT, SpecimenMark.SQUARE},
        {SpecimenMark.ROUND, SpecimenMark.INTACT, SpecimenMark.TRIANGLE},
        {SpecimenMark.SERRATED, SpecimenMark.INTACT, SpecimenMark.WAVE},
        {SpecimenMark.ROUND, SpecimenMark.INTACT, SpecimenMark.WAVE}
    };

    public SpecimenRule specimenRule() {
        return switch (this) {
            case STANDARD -> new SpecimenRule(SpecimenMark.SERRATED, SpecimenMark.TRIANGLE,
                    SpecimenMark.SERRATED, SpecimenMark.WAVE);
            case COPPER -> new SpecimenRule(SpecimenMark.ROUND, SpecimenMark.SQUARE,
                    SpecimenMark.ROUND, SpecimenMark.TRIANGLE);
            case BRASS -> new SpecimenRule(SpecimenMark.SERRATED, SpecimenMark.WAVE,
                    SpecimenMark.ROUND, SpecimenMark.WAVE);
        };
    }

    public SpecimenMark[] specimenMarks(int slot) {
        return SPECIMENS[(slot + ordinal()) % SPECIMENS.length].clone();
    }

    Puzzle apply(Puzzle original) {
        if (this == STANDARD) { return original; }

        String code = original.code();
        String hint = original.hint();
        String answer;
        switch ((int) original.puzzleId()) {
            case 1 -> {
                if (!code.contains("2, 1, -2, 1")) { return original; }
                int[] steps = this == COPPER ? new int[] {3, -1, 2, -3} : new int[] {1, 3, -2, 3};
                code = code.replace("2, 1, -2, 1", java.util.Arrays.toString(steps).replaceAll("[\\[\\]]", ""));
                var digits = new StringBuilder();
                int total = 0;
                for (int step : steps) { digits.append(total += step); }
                answer = digits.toString();
            }
            case 2 -> {
                if (!code.contains("{4, 3, 1}")) { return original; }
                String[] rows = this == COPPER ? new String[] {"214", "432", "143"} : new String[] {"342", "214", "431"};
                code = code.replace("{4, 3, 1}", gridRow(rows[0]))
                        .replace("{2, 1, 3}", gridRow(rows[1])).replace("{3, 2, 3}", gridRow(rows[2]));
                var digits = new StringBuilder();
                for (int col = 0; col < 3; col++) {
                    for (String row : rows) { digits.append(row.charAt(col)); }
                }
                answer = digits.toString();
            }
            case 311 -> {
                if (!code.contains("m.voltage >= 15")) { return original; }
                code = code.replace("m.voltage >= 15", "m.voltage >= " + powerThreshold());
                var powered = new StringBuilder();
                for (int i = 0; i < VOLTAGES.length; i++) {
                    if (operational(i) && moduleVoltage(i) >= powerThreshold()) { powered.append((char) ('A' + i)); }
                }
                answer = powered.toString();
            }
            case 312 -> {
                if (!code.contains("열림 − 닫힘")) { return original; }
                int[][] readings = sleeveReadings();
                int largest = 0;
                for (int i = 1; i < readings[0].length; i++) {
                    if (readings[1][i] - readings[0][i] > readings[1][largest] - readings[0][largest]) {
                        largest = i;
                    }
                }
                answer = new String(new char[] {(char) ('A' + largest), SLEEVE_BY_JOINT.charAt(largest)});
            }
            case 313 -> {
                if (!code.contains("C, B, A, D, E") || !code.contains("int left = 70")) { return original; }
                code = code.replace("C, B, A, D, E", String.join(", ", distributionQueue().split("")))
                        .replace("int left = 70", "int left = " + distributionCapacity());
                var running = new StringBuilder();
                int left = distributionCapacity();
                for (char device : distributionQueue().toCharArray()) {
                    int index = device - 'A';
                    if (operational(index) && deviceWatts(index) <= left) {
                        running.append(device);
                        left -= deviceWatts(index);
                    }
                }
                answer = running.toString();
            }
            case 314 -> {
                if (!code.contains("안전 배출 기록")) { return original; }
                answer = pressureOrder();
            }
            case 401 -> {
                String originalConditions = STANDARD.specimenRule().javaConditions();
                if (!code.contains(originalConditions)) { return original; }
                SpecimenRule rule = specimenRule();
                code = code.replace(originalConditions, rule.javaConditions());
                var selected = new StringBuilder();
                for (int slot = 0; slot < SPECIMENS.length; slot++) {
                    if (rule.keeps(specimenMarks(slot))) { selected.append((char) ('A' + slot)); }
                }
                answer = selected.toString();
            }
            case 402 -> {
                if (!code.contains("홈통의 높이") && !code.contains("홈통 끝을 이어라")) { return original; }
                answer = (highGutterInlet() ? "A" : "B") + "B" + (lowGutterOutlet() ? "C" : "B");
            }
            case 404 -> {
                if (!code.contains("int left = 13")) { return original; }
                code = code.replace("int left = 13", "int left = " + balanceLoad());
                answer = balanceAnswer(balanceLoad());
            }
            case 405 -> {
                if (!code.contains("flooded") || !code.contains("bufferAhead")) { return original; }
                answer = (railFlooded() ? "B" : "A") + (railBlocked() ? "A" : "B") + "AB";
                hint = "빈 차가 출발할 때 낮은 선로에 물이 있는지, 본선에 정지턱이 있는지 각각 확인한다.";
            }
            default -> { return original; }
        }
        return new Puzzle(original.puzzleId(), original.mapId(), original.stepNo(), code,
                original.answerType(), answer, hint);
    }

    private static String balanceAnswer(int load) {
        for (int light = 1; light <= 4; light++) {
            for (int heavy = 1; heavy <= 4; heavy++) {
                if (light != heavy && 2 * light + 3 * heavy == load) {
                    return new String(new char[] {(char) ('A' + light - 1), (char) ('A' + heavy - 1)});
                }
            }
        }
        throw new IllegalArgumentException("No distinct balance hooks for load " + load);
    }

    private static String gridRow(String row) { return "{" + String.join(", ", row.split("")) + "}"; }
}
