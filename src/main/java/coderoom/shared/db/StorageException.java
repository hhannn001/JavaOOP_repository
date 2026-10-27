package coderoom.shared.db;

import java.lang.RuntimeException;

/**
 * 담당: G
 * 역할: 저장소 장애를 상위 기능에 전달하는 예외.
 * 상위 타입: java.lang.RuntimeException
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class StorageException extends RuntimeException {
    // [구현 예정]
    // 메시지와 원인 예외를 받는 생성자를 추가하고 super(message, cause)를 호출한다.
    // 사용자 오답·계정 없음 같은 정상적인 실패에는 사용하지 않는다.
    // 화면에는 사용자용 안내를 표시하고 DB 접속 정보·해시 등 민감한 값은 노출하지 않는다.
}
