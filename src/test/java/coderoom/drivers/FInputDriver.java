package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coderoom.testkit.TestPorts;

/** F 담당 재사용 시나리오. 운영 연결 여부는 호출 테스트에서 구분한다. */
public final class FInputDriver {
    private FInputDriver() { }

    public static void verify(TestPorts.Game target) {
        var initial = target.view();
        assertThrows(UnsupportedOperationException.class, () -> initial.items().add("KEY"));
        assertThrows(IllegalArgumentException.class, () -> target.select("KEY"));
        assertTrue(target.submit("DRAWER", "2312"));
        target.select("NOTE2");
        assertEquals("NOTE2", target.view().selected());
        assertEquals(1, initial.items().size());
    }
}
