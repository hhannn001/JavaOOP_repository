package coderoom.testkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/** 메모리 대역만 제공한다. 운영 인증·게임·JDBC 구현이나 검증 결과가 아니다. */
public final class ScenarioStubs {
    private ScenarioStubs() { }

    public static final class Auth implements TestPorts.Auth {
        public boolean unavailable;

        @Override
        public boolean login(String id, String password) {
            if (unavailable) {
                throw new IllegalStateException("STUB_STORAGE_UNAVAILABLE");
            }
            return List.of("demo1", "demo2", "demo3").contains(id == null ? "" : id)
                    && "test-only".equals(password);
        }
    }

    public static final class Records implements TestPorts.Records {
        private final Map<String, Long> values = new HashMap<>();
        public boolean unavailable;

        @Override
        public void save(long userId, long mapId, long seconds) {
            checkAvailable();
            if (seconds < 0) {
                throw new IllegalArgumentException("negative time");
            }
            values.merge(userId + ":" + mapId, seconds, Math::min);
        }

        @Override
        public OptionalLong best(long userId, long mapId) {
            checkAvailable();
            Long value = values.get(userId + ":" + mapId);
            return value == null ? OptionalLong.empty() : OptionalLong.of(value);
        }

        private void checkAvailable() {
            if (unavailable) {
                throw new IllegalStateException("STUB_STORAGE_UNAVAILABLE");
            }
        }
    }

    public static final class Catalog implements TestPorts.Catalog {
        private final TestPorts.Records records;

        public Catalog(TestPorts.Records records) {
            this.records = records;
        }

        @Override
        public List<TestPorts.Card> list(long userId) {
            OptionalLong best = records.best(userId, 1);
            return List.of(new TestPorts.Card(1, true, best.isPresent() ? best.getAsLong() : null),
                    new TestPorts.Card(2, false, null), new TestPorts.Card(3, false, null));
        }
    }

    public static final class Game implements TestPorts.Game {
        private int step        = 0;
        private String selected = "NOTE1";
        private long seconds    = 0;

        @Override
        public TestPorts.View view() {
            List<String> items = step == 0 ? List.of("NOTE1")
                    : step == 1 ? List.of("NOTE1", "NOTE2") : List.of("NOTE1", "NOTE2", "KEY");
            return new TestPorts.View(List.of("START", "DRAWER_OPEN", "SAFE_OPEN", "ESCAPED").get(step),
                    items, selected, seconds);
        }

        @Override
        public boolean submit(String object, String answer) {
            if (step == 0 && "DRAWER".equals(object) && "2312".equals(answer)
                    || step == 1 && "SAFE".equals(object) && "423312133".equals(answer)) {
                step++;
                return true;
            }
            return false;
        }

        @Override
        public void select(String item) {
            if (!view().items().contains(item)) {
                throw new IllegalArgumentException("item not owned");
            }
            selected = item;
        }

        @Override
        public boolean escape(long elapsedSeconds) {
            if (elapsedSeconds < 0 || step != 2 || !"KEY".equals(selected)) {
                return false;
            }
            step    = 3;
            seconds = elapsedSeconds;
            return true;
        }

        @Override
        public void reset() {
            step     = 0;
            selected = "NOTE1";
            seconds  = 0;
        }
    }
}
