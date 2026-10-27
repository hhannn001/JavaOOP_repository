package coderoom.app;

/**
 * 담당: A
 * 역할: 로그인 이후의 화면 전환과 세션 연결.
 * 직접 선언한 상속·구현 관계 없음.
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class AppController {
    // [필드·연결]
    // coderoom.entities.user.LoginUser와 MainFrame, pages의 화면들을 연결한다.
    // LoginPage에는 Consumer<LoginUser>, MapSelectionPage에는 LongConsumer 콜백을 전달한다.

    // [구현 예정]
    // onLogin(user), openMap(mapId), backToMaps(), logout(): 화면 전환을 조율한다.
    // 새 게임마다 GameFacade와 GamePage를 새로 만들고 이전 GamePage를 정리한다.
    // 로그아웃 시 이전 계정의 화면 데이터와 게임 세션을 제거한다.
    // 저장 작업 중 전환을 막고, 기록 미저장 상태의 이탈은 사용자 확인 후 처리한다.
}
