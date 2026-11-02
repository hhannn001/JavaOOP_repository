package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import coderoom.testkit.TestPorts;

/** A 담당 재사용 시나리오. 운영 연결 여부는 호출 테스트에서 구분한다. */
public final class AFlowDriver {
    private AFlowDriver() { }

    public static void verify(TestPorts.Auth auth, TestPorts.Game game, TestPorts.Records records) {
        assertTrue(auth.login("demo1", "test-only"));
        assertTrue(game.submit("DRAWER", "2312"));
        assertTrue(game.submit("SAFE", "423312133"));
        game.select("KEY");
        assertTrue(game.escape(42));
        records.save(1, 1, game.view().seconds());
        assertEquals(42, records.best(1, 1).orElseThrow());
        game.reset();
        assertEquals("START", game.view().stage());
        assertEquals(1, game.view().items().size());
        assertEquals(0, game.view().seconds());
    }
}
