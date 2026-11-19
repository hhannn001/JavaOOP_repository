package coderoom.shared.db;

/**
 * 담당: G
 * 역할: 저장소 장애를 상위 기능에 전달하는 예외.
 * 상위 타입: java.lang.RuntimeException
 */
public class StorageException extends RuntimeException {
    // [동작]
    // 메시지와 원인 예외를 보존한다.
    // 사용자 오답·계정 없음 같은 정상적인 실패에는 사용하지 않는다.
    // 화면에는 사용자용 안내를 표시하고 DB 접속 정보·해시 등 민감한 값은 노출하지 않는다.

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
