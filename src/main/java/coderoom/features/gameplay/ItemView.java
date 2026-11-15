package coderoom.features.gameplay;

import coderoom.entities.game.ItemId;
import java.util.Objects;

/**
 * 담당: E
 * 역할: 게임 처리용 값 전달 객체.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public final class ItemView {
    // [필드 예정]
    // entities.game.ItemId id, String title, String content.
    // content는 Java 코드 종이 또는 열쇠 설명이며 정답은 아니다.

    // [구현 예정]
    // 읽기 전용 값 객체로 작성한다. 필요한 생성자·접근자만 구현하고 업무 로직은 두지 않는다.
    private final ItemId id;
    private final String title;
    private final String content;

    /** 단서 패널에 필요한 공개 정보만 보관한다. */
    public ItemView(ItemId id, String title, String content) {
        this.id      = Objects.requireNonNull(id, "아이템 ID가 필요합니다.");
        this.title   = Objects.requireNonNull(title, "제목이 필요합니다.");
        this.content = Objects.requireNonNull(content, "내용이 필요합니다.");
    }

    public ItemId id() { return id; }
    public String title() { return title; }
    public String content() { return content; }
}
