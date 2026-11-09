package coderoom.entities.game;

/**
 * 담당: E
 * 역할: 방에서 조사 가능한 물건
 * 직접 선언한 상속·구현 관계 없음.
 * 설계: 서랍·벽 다이얼·문은 퍼즐 장치이고, 책·창문은 설명만 보여 주는 조사 대상이다.
 */
public enum RoomObjectId {
    DRAWER, WALL_DIAL, DOOR, BOOKS, WINDOW
}
