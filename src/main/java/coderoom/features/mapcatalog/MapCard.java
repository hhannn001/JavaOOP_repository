
package coderoom.features.mapcatalog;

public class MapCard {

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
