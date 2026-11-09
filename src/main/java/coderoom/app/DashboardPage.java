package coderoom.app;

import coderoom.entities.user.LoginUser;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * 담당: A
 * 역할: 로그인과 맵 선택 사이의 임시 대시보드.
 * 실제 데이터 서비스가 연결되기 전에는 흰 화면과 버튼만 표시한다.
 */
final class DashboardPage extends JPanel {
    /** 각 기능의 자리는 마련하되 계정·기록 데이터를 임의로 만들어 보여 주지 않는다. */
    DashboardPage(LoginUser user, Runnable openMaps, Runnable logout) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(openMaps);
        Objects.requireNonNull(logout);
        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());

        JPanel buttons = new JPanel(new GridLayout(5, 1, 0, 12));
        buttons.setBackground(Color.WHITE);
        buttons.add(pendingButton("로그인 정보 확인"));
        buttons.add(pendingButton("맞힌 문제와 정답 확인"));
        buttons.add(pendingButton("클리어 진도 확인"));
        JButton maps = new JButton("맵 선택으로 이동");
        maps.addActionListener(event -> openMaps.run());
        buttons.add(maps);
        JButton exit = new JButton("로그아웃");
        exit.addActionListener(event -> logout.run());
        buttons.add(exit);
        add(buttons);
    }

    /** 다른 담당자의 데이터 계약이 완성되기 전에는 기능별 연결 대기만 알린다. */
    private JButton pendingButton(String title) {
        JButton button = new JButton(title);
        button.addActionListener(event -> JOptionPane.showMessageDialog(this, "연동 전"));
        return button;
    }
}
