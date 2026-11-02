package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import coderoom.testkit.TestPorts;

/** B 담당 재사용 시나리오. 운영 연결 여부는 호출 테스트에서 구분한다. */
public final class BLoginDriver {
    private BLoginDriver() { }

    public static void verify(TestPorts.Auth target) {
        assertTrue(target.login("demo1", "test-only"));
        assertFalse(target.login("demo1", "wrong"));
        assertFalse(target.login("unknown", "test-only"));
        assertFalse(target.login("", ""));
    }
}
