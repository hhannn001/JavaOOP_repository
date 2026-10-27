package coderoom.widgets.inventory;

import javax.swing.JPanel;

/**
 * 담당: F
 * 역할: 하단 인벤토리
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class InventoryPanel extends JPanel {
    // [연결]
    // 표시 데이터와 콜백을 GamePage에서 받는다. 다른 widgets 슬라이스를 참조하지 않는다.

    // [구현 예정]
    // GameView.items와 selectedItem을 표시한다.
    // 선택한 entities.game.ItemId를 콜백으로 전달한다.
    // 획득하지 않은 아이템은 선택할 수 없고 선택 상태는 반환된 view로만 갱신한다.
}
