package coderoom.features.gameplay;

import coderoom.entities.game.GameStage;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.game.WorkshopRoom;
import java.util.Set;

/** 방을 조사하거나 장치를 복구할 때의 플레이어 대사. */
public final class RoomStory {
    private RoomStory() { }

    public static String afterRepair(WorkshopRoom room, RoomObjectId device, Set<Integer> solved) {
        return switch (room) {
            case ENGINE -> switch (device) {
                case DRAWER -> "불이 돌아왔어. 증기가 새는 곳을 막고 장비에 전력을 나눠 줘야 해.";
                case WALL_DIAL -> solved.contains(2)
                        ? "쉭쉭거리던 소리가 멎었어. 이제 아래 압력 피트를 열자."
                        : "쉭쉭거리던 소리가 멎었어. 분배함을 살리면 아래 압력 피트를 열 수 있겠어.";
                case THIRD_DEVICE -> solved.contains(1)
                        ? "환풍기가 도는 소리가 들려. 이제 아래 압력 피트에서 남은 압력을 빼자."
                        : "환풍기가 도는 소리가 들려. 배관까지 막으면 아래 압력 피트를 열 수 있겠어.";
                case FOURTH_DEVICE -> "바늘이 드디어 내려왔어. 전력이 돌아왔고 압력도 안전하다는 걸 제어기에 알려 주자.";
                case FIFTH_DEVICE -> "출구가 열렸어. 증기가 걷힌 틈에 어서 나가자.";
                default -> throw new IllegalArgumentException("정비 장치가 아닙니다.");
            };
            case GREENHOUSE -> switch (device) {
                case DRAWER -> solved.contains(2)
                        ? "이 봉투는 아직 말라 있어. 이제 운반차에 실어 보자."
                        : "이 봉투는 아직 말라 있어. 바닥 통로를 확보하면 운반차에 싣자.";
                case WALL_DIAL -> "물이 빠지니 바닥 레일이 보여. 저 발판을 옮기면 건널 수 있겠다.";
                case THIRD_DEVICE -> solved.contains(0)
                        ? "이제 운반차가 건널 수 있어. 씨앗 상자가 쏠리지 않게 균형을 잡자."
                        : "이제 운반차가 건널 수 있어. 작업대로 돌아가 씨앗을 챙기자.";
                case FOURTH_DEVICE -> "상자가 흔들리지 않아. 이제 침수된 갈래를 피해서 출구로 밀자.";
                case FIFTH_DEVICE -> "씨앗이 무사해. 이대로 나가면 새벽에 다시 심을 수 있겠어.";
                default -> throw new IllegalArgumentException("운반 장치가 아닙니다.");
            };
        };
    }

    public static String inspection(RoomObjectId object, GameStage stage) {
        return switch (object) {
            case WINDOW -> "손잡이가 꼼짝도 안 해. 이 창문으로는 못 나갈 것 같아. 다른 길을 찾아보자.";
            case BOOKS -> "책마다 Java 코드가 적혀 있어. 손에 든 메모도… 코드의 실행 결과가 암호인 걸까?";
            case DOOR -> "문이 잠겼어. 이 방 어딘가에 열쇠가 있을 거야.";
            case DRAWER -> stage == GameStage.START
                    ? "네 자리 잠금장치네. 첫 번째 메모에 답이 있을 것 같아."
                    : "서랍은 열려 있어. 여기서 찾은 두 번째 메모를 살펴보자.";
            case HINT_NOTE, EVIDENCE_1, EVIDENCE_2, EVIDENCE_3, EVIDENCE_4, EVIDENCE_5 -> "접힌 메모에 장치의 힌트가 적혀 있어.";
            case THIRD_DEVICE, FOURTH_DEVICE, FIFTH_DEVICE -> "장치 옆에 남은 단서를 살펴보자.";
            case WALL_DIAL -> stage == GameStage.START
                    ? "아홉 칸짜리 잠금장치… 아직 단서가 부족해. 서랍부터 확인하자."
                    : stage == GameStage.DRAWER_OPEN
                            ? "두 번째 메모의 결과를 행 순서대로 넣으면 되겠어."
                            : "금고는 비었어. 이제 열쇠로 문을 열면 돼.";
        };
    }
}
