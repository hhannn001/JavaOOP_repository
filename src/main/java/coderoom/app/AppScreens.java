package coderoom.app;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.user.LoginUser;
import coderoom.features.auth.LoginService;
import coderoom.features.gameplay.GameSetup;
import coderoom.features.gameplay.GameView;
import coderoom.features.gameplay.InspectResult;
import coderoom.features.gameplay.SubmitResult;
import coderoom.pages.game.GamePage;
import coderoom.pages.login.LoginPage;
import coderoom.pages.mapselect.MapSelectionPage;
import coderoom.widgets.room.RoomPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * 담당: A
 * 역할: AppController가 요청하는 화면의 생성과 연결.
 * B·C의 실제 화면 클래스를 조립하고, 미구현 게임 기능에는 임시 포트를 제공한다.
 * 다른 담당자의 인증·맵 조회·게임 규칙은 이 클래스에서 구현하지 않는다.
 */
final class AppScreens implements AppController.Screens {
    private static final Color DEEP  = new Color(21, 39, 30);
    private static final Color GOLD  = new Color(229, 194, 134);

    private final LoginService loginService;

    AppScreens(LoginService loginService) {
        this.loginService = Objects.requireNonNull(loginService, "로그인 서비스가 필요합니다.");
    }

    @Override
    public JComponent login(Consumer<LoginUser> onLogin) {
        return new LoginPage(loginService, onLogin);
    }

    @Override
    public JComponent dashboard(LoginUser user, Runnable openMaps, Runnable logout) {
        // A의 대시보드는 현재 사용자 세션만 받고 기록 데이터는 연동 전으로 표시한다.
        return new DashboardPage(user, openMaps, logout);
    }

    @Override
    public JComponent maps(LoginUser user, LongConsumer openMap, Runnable dashboard) {
        // C의 기존 하단 버튼을 대시보드 복귀 콜백에 연결한다.
        return new MapSelectionPage(openMap, dashboard);
    }

    @Override
    public GamePage game(LoginUser user, long mapId, Runnable back, Runnable restart) {
        // GamePage의 비동기 로딩·타이머 수명은 실제 A 코드로 확인한다.
        return new GamePage(new DemoGame(), new DemoWidgets(back));
    }

    private static JButton button(String title) {
        JButton button = new JButton(title);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBackground(GOLD);
        button.setForeground(DEEP);
        button.setBorder(BorderFactory.createEmptyBorder(13, 22, 13, 22));
        return button;
    }

    /** 화면마다 새 인스턴스를 만들면서 임시 설정 버튼의 모양만 공유한다. */
    private static JButton settingsButton(Runnable openSettings) {
        JButton settings = button("설정");
        settings.setPreferredSize(new Dimension(64, 64));
        settings.setBackground(Color.WHITE);
        settings.setBorder(BorderFactory.createLineBorder(DEEP));
        settings.addActionListener(event -> openSettings.run());
        return settings;
    }

    /** 빈 DTO만 전달하여 A의 화면 수명과 전환을 확인하는 게임 포트다. */
    private static final class DemoGame implements GamePage.GamePort {
        private final GameView snapshot = new GameView();

        @Override
        public GameSetup loadMap(long mapId) {
            return new GameSetup();
        }

        @Override
        public GameView start(LoginUser user, GameSetup setup, long nowNanos) {
            return snapshot;
        }

        @Override
        public GameView view(long nowNanos) {
            return snapshot;
        }

        @Override
        public InspectResult inspect(RoomObjectId object) {
            // D의 클릭 콜백은 A까지 전달하되 E의 조사 결과가 구현될 때까지 빈 결과를 돌려준다.
            return new InspectResult();
        }

        @Override
        public SubmitResult submit(RoomObjectId object, String answer) {
            throw new UnsupportedOperationException("퍼즐 제출은 게임 기능 연동 후 사용할 수 있습니다.");
        }

        @Override
        public GameView selectItem(ItemId item) {
            throw new UnsupportedOperationException("아이템 선택은 게임 기능 연동 후 사용할 수 있습니다.");
        }

        @Override
        public GamePage.Escape useKey(long nowNanos) {
            throw new UnsupportedOperationException("탈출은 게임 기능 연동 후 사용할 수 있습니다.");
        }

        @Override
        public void saveClear(ClearRecord record) {
            throw new UnsupportedOperationException("기록 저장은 게임 기능 연동 후 사용할 수 있습니다.");
        }
    }

    /** 방 사진 위의 설정 버튼으로 중앙 메뉴를 열고 나가기만 A의 복귀 콜백에 연결한다. */
    private static final class DemoWidgets implements GamePage.Widgets {
        private GamePage actions;
        private final RoomPanel room = new RoomPanel(object -> {
            if (actions != null) {
                actions.inspect(object);
            }
        }, true);
        private final JPanel spacer = new JPanel();

        private DemoWidgets(Runnable back) {
            // 투명한 상단 컨테이너가 설정 버튼만 우상단에 겹쳐 표시한다.
            room.setLayout(new BorderLayout());
            JPanel overlay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 16));
            overlay.setOpaque(false);
            overlay.add(settingsButton(() -> openSettings(back)));
            room.add(overlay, BorderLayout.NORTH);
            // GamePage의 SOUTH 슬롯은 높이를 차지하지 않게 해 사진이 창 아래까지 닿게 한다.
            spacer.setPreferredSize(new Dimension(0, 0));
            spacer.setOpaque(false);
        }

        /** 중앙 팝업에는 추후 메뉴 자리를 마련하고 나가기만 동작하게 한다. */
        private void openSettings(Runnable back) {
            Window owner = SwingUtilities.getWindowAncestor(room);
            JDialog dialog = new JDialog(owner, "설정", Dialog.ModalityType.DOCUMENT_MODAL);
            JPanel content = new JPanel(new BorderLayout(0, 24));
            content.setBackground(DEEP);
            content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
            JPanel options = new JPanel(new GridLayout(2, 1, 0, 12));
            options.setOpaque(false);
            JButton sound = button("소리 · 준비 중");
            sound.setEnabled(false);
            JButton help = button("도움말 · 준비 중");
            help.setEnabled(false);
            options.add(sound);
            options.add(help);
            content.add(options, BorderLayout.CENTER);
            JButton exit = button("나가기");
            exit.addActionListener(event -> {
                dialog.dispose();
                back.run();
            });
            content.add(exit, BorderLayout.SOUTH);
            dialog.setContentPane(content);
            dialog.setSize(360, 300);
            dialog.setLocationRelativeTo(room);
            dialog.setVisible(true);
        }

        @Override
        public JComponent room() { return room; }
        @Override
        public JComponent controls() { return spacer; }
        @Override
        public void bind(GamePage actions) {
            // D의 물건 클릭은 A의 게임 조작 경계로 전달한다.
            this.actions = actions;
        }
        @Override
        public void render(GameView view) { }
        @Override
        public void inspected(InspectResult result) {
            // E의 조사 결과 DTO가 완성되면 여기에 안내·입력창 표시를 연결한다.
        }
        @Override
        public void submitted(SubmitResult result) { }
        @Override
        public void status(String message) {
            // 게임 기능이 아직 연결되지 않아 작업 상태는 화면에 표시하지 않는다.
        }
        @Override
        public void busy(boolean busy) {
            // GamePage가 작업 중 화면 이동을 차단하므로 버튼의 이벤트 경로도 안전하다.
        }
        @Override
        public void result(GamePage.Escape escape, GamePage.SaveState state) { }
    }

}
