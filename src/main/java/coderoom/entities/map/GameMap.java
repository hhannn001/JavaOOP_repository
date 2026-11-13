
package coderoom.entities.map;

public class GameMap {

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
