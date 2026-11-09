package coderoom.pages.mapselect;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;
import java.util.function.LongConsumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 담당: C
 * 역할: 포스터형 맵 선택 화면
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class MapSelectionPage extends JPanel {
    // [연결]
    // coderoom.features.mapcatalog.MapCatalogService와 현재 사용자 ID를 전달받는다.
    // 맵 선택은 LongConsumer 콜백으로 상위에 알린다.

    // [구현 예정]
    // render(cards): 플레이 가능한 포스터 1개와 준비 중 포스터 2개를 표시한다.
    // 클리어 여부·최고 기록을 표시하고 준비 중 맵의 입장을 막는다.
    // 목록 조회는 SwingWorker로 처리하고 게임 복귀 시 기록을 다시 읽는다.

    // [경계]
    // app.AppController나 다른 pages 슬라이스를 직접 import하지 않는다.

    /** 맵 서비스 연동 전에는 첫 맵 선택과 로그아웃 화면 전환만 제공한다. */
    public MapSelectionPage(LongConsumer openMap, Runnable logout) {
        Objects.requireNonNull(openMap);
        Objects.requireNonNull(logout);
        setLayout(new BorderLayout());
        add(new JLabel("임시 진입점 구현", SwingConstants.CENTER), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout());
        JButton enter = new JButton("ROOM 01 · 서재 화면 보기");
        enter.addActionListener(event -> openMap.accept(1L));
        JButton exit = new JButton("시작 화면으로 돌아가기");
        exit.addActionListener(event -> logout.run());
        actions.add(enter);
        actions.add(exit);
        add(actions, BorderLayout.SOUTH);
    }
}
