package coderoom.features.gameplay;

import coderoom.entities.game.ItemId;
import java.util.Objects;

/**
 * 담당: E
 * 역할: 게임 처리용 값 전달 객체.
 * 직접 선언한 상속·구현 관계 없음.
 */
public final class ItemView {
    // [표시 데이터]
    // 아이템 식별자, 제목, 내용과 힌트를 제공한다.
    // content는 Java 코드 종이 또는 열쇠 설명이며 정답은 아니다.

    // [동작]
    // 생성자에서 null을 거절하며 정답 판정이나 상태 변경은 하지 않는다.
    private final ItemId id;
    private final String title;
    private final String content;
    private final String hint;

    /** 단서 패널에 필요한 공개 정보만 보관한다. */
    public ItemView(ItemId id, String title, String content) {
        this(id, title, content, "");
    }

    public ItemView(ItemId id, String title, String content, String hint) {
        this.hint    = Objects.requireNonNull(hint);
        this.id      = Objects.requireNonNull(id, "아이템 ID가 필요합니다.");
        this.title   = Objects.requireNonNull(title, "제목이 필요합니다.");
        this.content = Objects.requireNonNull(content, "내용이 필요합니다.");
    }

    public ItemId id() { return id; }
    public String title() { return title; }
    public String hint() { return hint; }
    public String content() { return content; }
}
