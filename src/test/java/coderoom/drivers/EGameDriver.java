package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import coderoom.testkit.TestPorts;

/** E 담당 재사용 시나리오. 운영 연결 여부는 호출 테스트에서 구분한다. */
public final class EGameDriver {
    private EGameDriver() { }

    public static void verify(TestPorts.Game target) {
        assertFalse(target.submit("SAFE", "423312133"));
        assertFalse(target.submit("DRAWER", ""));
        assertFalse(target.submit("DRAWER", "0000"));
        assertEquals("START", target.view().stage());
        assertTrue(target.submit("DRAWER", "2312"));
        assertFalse(target.submit("DRAWER", "2312"));
        assertEquals(2, target.view().items().size());
        assertTrue(target.submit("SAFE", "423312133"));
        assertFalse(target.escape(10));
        target.select("KEY");
        assertTrue(target.escape(42));
        assertFalse(target.escape(99));
        assertEquals(42, target.view().seconds());
    }
}
