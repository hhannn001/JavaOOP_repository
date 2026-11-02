package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import coderoom.testkit.TestPorts;

/** D 담당 재사용 시나리오. 운영 연결 여부는 호출 테스트에서 구분한다. */
public final class DRoomDriver {
    private DRoomDriver() { }

    public static void verify(TestPorts.Game target) {
        assertEquals("START", target.view().stage());
        assertTrue(target.submit("DRAWER", "2312"));
        assertEquals("DRAWER_OPEN", target.view().stage());
        assertTrue(target.submit("SAFE", "423312133"));
        assertEquals("SAFE_OPEN", target.view().stage());
    }
}
