package coderoom.entities.game;

import java.util.List;
import java.util.Set;

/** The scene's devices, evidence and dependencies in the same order as its seeded puzzles. */
public enum WorkshopRoom {
    ENGINE(3, List.of(
            new Task(RoomObjectId.DRAWER, "비상 전력 랙", "전력 기록", Set.of()),
            new Task(RoomObjectId.WALL_DIAL, "배관 누출", null, Set.of(0)),
            new Task(RoomObjectId.THIRD_DEVICE, "전력 분배함", "분배표", Set.of(0)),
            new Task(RoomObjectId.FOURTH_DEVICE, "압력 피트", null, Set.of(1, 2)),
            new Task(RoomObjectId.FIFTH_DEVICE, "중앙 제어기", "점검표", Set.of(0, 1, 2, 3)))),
    GREENHOUSE(4, List.of(
            new Task(RoomObjectId.DRAWER, "씨앗 보관함", "표본표", Set.of()),
            new Task(RoomObjectId.WALL_DIAL, "창 아래 물받이", null, Set.of()),
            new Task(RoomObjectId.THIRD_DEVICE, "저지대 발판", null, Set.of(1)),
            new Task(RoomObjectId.FOURTH_DEVICE, "운반차 적재대", "균형표", Set.of(0, 2)),
            new Task(RoomObjectId.FIFTH_DEVICE, "출구 분기기", "레일 지도", Set.of(3))));

    public record Task(RoomObjectId device, String title, String evidence, Set<Integer> requires) {
        public boolean hasEvidence() { return evidence != null; }
    }

    private final long id;
    private final List<Task> tasks;

    WorkshopRoom(long id, List<Task> tasks) {
        this.id    = id;
        this.tasks = tasks;
    }

    public static WorkshopRoom forMap(long mapId) {
        for (WorkshopRoom room : values()) {
            if (room.id == mapId) { return room; }
        }
        return null;
    }

    public String outcome(int index) {
        return switch (this) {
            case ENGINE -> switch (index) {
                case 0 -> "전력 랙이 깨어났다. 분배함과 배관으로 전력이 돌아온다.";
                case 1 -> "슬리브가 이음쇠에 잠긴다. 새던 증기가 멎었다.";
                case 2 -> "분배함이 가동된다. 압력 피트로 전력이 흐른다.";
                case 3 -> "바늘이 내려앉는다. 중앙 제어기의 안전 회로에 신호가 들어왔다.";
                case 4 -> "제어기 너머에서 철컥 소리가 난다. 출구의 빗장이 밀려났다.";
                default -> throw new IndexOutOfBoundsException(index);
            };
            case GREENHOUSE -> switch (index) {
                case 0 -> "봉투를 열자 마른 씨앗이 드러난다. 이 씨앗을 출구까지 옮기자.";
                case 1 -> "고여 있던 물이 배수구로 빨려든다. 바닥의 레일이 드러났다.";
                case 2 -> "발판이 받침에 잠기자 끊겼던 레일이 이어졌다. 이제 운반차가 지나갈 수 있다.";
                case 3 -> "저울이 수평에서 멈추고 걸쇠가 잠긴다. 씨앗 상자가 운반차에 실렸다.";
                case 4 -> "운반차가 출구에 닿자 빗장이 풀렸다. 이제 씨앗을 가지고 나갈 수 있다.";
                default -> throw new IndexOutOfBoundsException(index);
            };
        };
    }

    public Task task(int index) { return tasks.get(index); }
}
