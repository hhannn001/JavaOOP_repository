package coderoom.entities.record;

/**
 * 담당: G
 * 역할: 도메인 데이터.
 * 직접 선언한 상속·구현 관계 없음.
 */
public class ClearRecord {
    // userId, mapId, bestSeconds(0 이상).
    private final long userId;
    private final long mapId;
    private final long bestSeconds;

    // 저장할 후보 기록 또는 조회한 최고 기록을 담는다.
    // 다른 엔티티 객체 대신 ID만 참조한다. 최고 기록 비교는 저장소가 담당한다.
    public ClearRecord(long userId, long mapId, long bestSeconds){
        if (userId <= 0 || mapId <= 0 || bestSeconds < 0) {
            throw new IllegalArgumentException("사용자·맵 ID는 양수, 기록은 0 이상이어야 합니다.");
        }
        this.userId      = userId;
        this.mapId       = mapId;
        this.bestSeconds = bestSeconds;
    }

    // 검증된 사용자·맵 ID와 최고 기록을 읽기 전용 접근자로 제공한다.

    public long getUserId() {
        return userId;
    }

    public long getMapId() {
        return mapId;
    }

    public long getBestSeconds() {
        return bestSeconds;
    }
}
