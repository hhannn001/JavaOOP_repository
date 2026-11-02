package coderoom.testkit;

import java.util.List;
import java.util.OptionalLong;

/** 테스트 전용 임시 계약. 실제 API 확정 후 각 담당자가 어댑터로 연결한다. */
public final class TestPorts {
    private TestPorts() { }

    public record Card(long id, boolean playable, Long bestSeconds) { }
    public record View(String stage, List<String> items, String selected, long seconds) {
        public View {
            items = List.copyOf(items);
        }
    }
    public interface Auth {
        boolean login(String id, String password);
    }
    public interface Catalog {
        List<Card> list(long userId);
    }
    public interface Records {
        void save(long userId, long mapId, long seconds);
        OptionalLong best(long userId, long mapId);
    }
    public interface Game {
        View view();
        boolean submit(String object, String answer);
        void select(String item);
        boolean escape(long seconds);
        void reset();
    }
}
