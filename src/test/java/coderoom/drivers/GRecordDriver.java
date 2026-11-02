package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coderoom.testkit.TestPorts;

/** G 담당 재사용 시나리오. 운영 연결 여부는 호출 테스트에서 구분한다. */
public final class GRecordDriver {
    private GRecordDriver() { }

    public static void verify(TestPorts.Records target) {
        assertTrue(target.best(1, 1).isEmpty());
        target.save(1, 1, 90);
        target.save(1, 1, 120);
        assertEquals(90, target.best(1, 1).orElseThrow());
        target.save(1, 1, 60);
        target.save(1, 1, 60);
        assertEquals(60, target.best(1, 1).orElseThrow());
        assertTrue(target.best(2, 1).isEmpty());
        assertTrue(target.best(1, 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> target.save(1, 1, -1));
    }
}
