package coderoom.entities.puzzle;

import java.util.List;

public interface PuzzleRepository {
    List<Puzzle> findByMapId(long mapId);
}
