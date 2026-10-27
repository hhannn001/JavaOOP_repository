package coderoom.features.mapcatalog;

/**
 * 담당: C
 * 역할: 맵과 사용자별 최고 기록 결합.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class MapCatalogService {
    // [연결]
    // coderoom.entities.map.MapRepository와 coderoom.entities.record.ClearRecordRepository를 받는다.

    // [구현 예정]
    // listForUser(userId): 맵 목록과 해당 계정의 기록을 합쳐 List<MapCard>를 반환한다.
    // DB 장애를 빈 목록이나 미완료 기록으로 숨기지 않는다.
}
