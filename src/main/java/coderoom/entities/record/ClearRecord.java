package coderoom.entities.record;

/**
 * 담당: G
 * 역할: 도메인 데이터.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class ClearRecord {
    // userId, mapId, bestSeconds(0 이상).
    private final long userId;
    private final long mapId;
    private final long bestSeconds;

    // 저장할 후보 기록 또는 조회한 최고 기록을 담는다.
    // 다른 엔티티 객체 대신 ID만 참조한다. 최고 기록 비교는 저장소가 담당한다.
    public ClearRecord(long userId, long mapId, long bestSeconds){
        if(bestSeconds < 0){

        }
        this.userId = userId;
        this.mapId = mapId;
        this.bestSeconds = bestSeconds;

    }
    
    // 필수값 검증과 읽기 전용 접근자를 작성한다. Swing·SQL 코드는 넣지 않는다.

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
