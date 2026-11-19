package coderoom.features.gameplay;

import coderoom.entities.game.GameStage;
import coderoom.entities.game.RoomObjectId;

/** 서재의 설정과 혼잣말. 정답이나 게임 상태를 변경하지 않는다. */
public final class RoomStory {
    private RoomStory() { }

    public static String inspection(RoomObjectId object, GameStage stage) {
        return switch (object) {
            case WINDOW -> "손잡이가 꼼짝도 안 해. 이 창문으로는 못 나갈 것 같아. 다른 길을 찾아보자.";
            case BOOKS -> "책마다 Java 코드가 적혀 있어. 손에 든 메모도… 코드의 실행 결과가 암호인 걸까?";
            case DOOR -> "문이 잠겼어. 이 방 어딘가에 열쇠가 있을 거야.";
            case DRAWER -> stage == GameStage.START
                    ? "네 자리 잠금장치네. 첫 번째 메모에 답이 있을 것 같아."
                    : "서랍은 열려 있어. 여기서 찾은 두 번째 메모를 살펴보자.";
            case WALL_DIAL -> stage == GameStage.START
                    ? "아홉 칸짜리 잠금장치… 아직 단서가 부족해. 서랍부터 확인하자."
                    : stage == GameStage.DRAWER_OPEN
                            ? "두 번째 메모의 결과를 행 순서대로 넣으면 되겠어."
                            : "금고는 비었어. 이제 열쇠로 문을 열면 돼.";
        };
    }
}
