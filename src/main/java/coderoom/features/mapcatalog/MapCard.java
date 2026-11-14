package coderoom.features.mapcatalog;

/**
 * 담당: C
 * 역할: 맵 포스터 표시용 값.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */

public class MapCard {

	// [필드 예정]
    // mapId, title, description, topic, difficulty, posterPath, playable.
    // 클리어 여부, 미클리어 시 값이 없는 최고 기록(초).

    // [구현 예정]
    // 읽기 전용 값 객체로 구현한다. 별도 DB 테이블은 만들지 않는다.
	
    // 맵 기본 정보
    private int mapId;
    private String title;
    private String description;
    private String topic;
    private String difficulty;
    private String posterPath;
    private boolean playable;

    // 사용자별 클리어 기록
    private boolean cleared;
    private Integer bestSeconds;

    // 생성자
    public MapCard(int mapId, String title, String description,
                   String topic, String difficulty, String posterPath,
                   boolean playable, boolean cleared, Integer bestSeconds) {

        this.mapId = mapId;
        this.title = title;
        this.description = description;
        this.topic = topic;
        this.difficulty = difficulty;
        this.posterPath = posterPath;
        this.playable = playable;
        this.cleared = cleared;
        this.bestSeconds = bestSeconds;
    }

    public int getMapId() {
        return mapId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public boolean isPlayable() {
        return playable;
    }

    public boolean isCleared() {
        return cleared;
    }

    public Integer getBestSeconds() {
        return bestSeconds;
    }
}
