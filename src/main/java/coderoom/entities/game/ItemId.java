package coderoom.entities.game;

import java.util.List;

public enum ItemId {
    NOTE1, NOTE2, KEY, NOTE3, NOTE4, NOTE5;

    private static final List<ItemId> NOTES = List.of(NOTE1, NOTE2, NOTE3, NOTE4, NOTE5);

    public static List<ItemId> notes() { return NOTES; }
}
