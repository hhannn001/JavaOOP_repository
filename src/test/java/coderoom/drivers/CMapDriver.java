package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import coderoom.testkit.TestPorts;

/** C 담당 재사용 시나리오. 운영 연결 여부는 호출 테스트에서 구분한다. */
public final class CMapDriver {
    private CMapDriver() { }

    public static void verify(TestPorts.Catalog target) {
        var cards = target.list(1);
        assertEquals(3, cards.size());
        assertEquals(1, cards.stream().filter(TestPorts.Card::playable).count());
        assertNull(cards.get(0).bestSeconds());
    }
}
