package coderoom.entities.record;

import java.util.List;

/**
 * 담당: G
 * 역할: 도메인 저장소 계약.
 * 직접 선언한 상속·구현 관계 없음.
 */
public interface ClearRecordRepository {
    // [조회 계약]
    // findByUserId(userId): List<ClearRecord>.
    List<ClearRecord> findByUserId(long userId);

    // saveBest(record): void.
    void saveBest(ClearRecord record);
    
    // [실패 처리]
    // 조회·저장 장애는 coderoom.shared.db.StorageException으로 전달한다.
}
