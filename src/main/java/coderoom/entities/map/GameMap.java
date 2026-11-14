package coderoom.entities.map;

/**
 * 담당: C
 * 역할: 도메인 데이터.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */

public class GameMap {
	
	// [필드 예정]
    // mapId, title, description, topic, difficulty, posterPath, playable.
    // Java 컬렉션 Map과 이름을 구분한다.

    // [구현 예정]
    // 필수값 검증과 읽기 전용 접근자를 작성한다. Swing·SQL 코드는 넣지 않는다.
	
    // 맵의 고유 번호
    private int mapId;

    // 맵 이름
    private String title;

    // 맵 설명
    private String description;

    // 맵 주제
    private String topic;

    // 난이도
    private String difficulty;

    // 포스터 이미지 경로
    private String posterPath;

    // 플레이 가능 여부
    private boolean playable;

    // 생성자
    public GameMap(int mapId, String title, String description,
                   String topic, String difficulty,
                   String posterPath, boolean playable) {

        this.mapId = mapId;
        this.title = title;
        this.description = description;
        this.topic = topic;
        this.difficulty = difficulty;
        this.posterPath = posterPath;
        this.playable = playable;
    }

    // 맵 번호 반환
    public int getMapId() {
        return mapId;
    }

    // 맵 이름 반환
    public String getTitle() {
        return title;
    }

    // 맵 설명 반환
    public String getDescription() {
        return description;
    }

    // 주제 반환
    public String getTopic() {
        return topic;
    }

    // 난이도 반환
    public String getDifficulty() {
        return difficulty;
    }

    // 포스터 경로 반환
    public String getPosterPath() {
        return posterPath;
    }

    // 플레이 가능 여부 반환
    public boolean isPlayable() {
        return playable;
    }
}
