package coderoom.drivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coderoom.testkit.ScenarioStubs;
import org.junit.jupiter.api.Test;

/** 이 테스트의 통과는 테스트 대역과 드라이버의 실행 가능성만 의미한다. */
class StubHarnessTest {
    @Test void aFlow() {
        AFlowDriver.verify(new ScenarioStubs.Auth(), new ScenarioStubs.Game(), new ScenarioStubs.Records());
    }
    @Test void bLogin() { BLoginDriver.verify(new ScenarioStubs.Auth()); }
    @Test void cMap() { CMapDriver.verify(new ScenarioStubs.Catalog(new ScenarioStubs.Records())); }
    @Test void dRoomStates() { DRoomDriver.verify(new ScenarioStubs.Game()); }
    @Test void eRules() { EGameDriver.verify(new ScenarioStubs.Game()); }
    @Test void fInputData() { FInputDriver.verify(new ScenarioStubs.Game()); }
    @Test void gRecords() { GRecordDriver.verify(new ScenarioStubs.Records()); }

    @Test void storageFailureAndRetry() {
        var records = new ScenarioStubs.Records();
        records.unavailable = true;
        assertThrows(IllegalStateException.class, () -> records.save(1, 1, 42));
        assertThrows(IllegalStateException.class, () -> records.best(1, 1));
        records.unavailable = false;
        records.save(1, 1, 42);
        assertEquals(42, records.best(1, 1).orElseThrow());
    }

    @Test void loginStorageFailure() {
        var auth = new ScenarioStubs.Auth();
        auth.unavailable = true;
        assertThrows(IllegalStateException.class, () -> auth.login("demo1", "test-only"));
    }
}
