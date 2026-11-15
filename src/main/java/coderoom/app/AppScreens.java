package coderoom.app;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.map.GameMap;
import coderoom.entities.map.MapRepository;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.record.ClearRecordRepository;
import coderoom.entities.user.LoginUser;
import coderoom.features.auth.LoginService;
import coderoom.features.mapcatalog.MapCatalogService;
import coderoom.features.gameplay.GameSetup;
import coderoom.features.gameplay.GameView;
import coderoom.features.gameplay.InspectResult;
import coderoom.features.gameplay.SubmitResult;
import coderoom.pages.game.GamePage;
import coderoom.pages.login.LoginPage;
import coderoom.pages.mapselect.MapSelectionPage;
import coderoom.widgets.room.RoomPanel;
import coderoom.widgets.inventory.InventoryPanel;
import coderoom.widgets.clue.CluePanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
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
    private final MapCatalogService mapCatalog;
    private boolean introSeen;

    AppScreens(LoginService loginService) {
        this.loginService = Objects.requireNonNull(loginService, "로그인 서비스가 필요합니다.");
        this.mapCatalog   = createDemoMapCatalog();
    }

    @Override
    public JComponent login(Consumer<LoginUser> onLogin) {
        return new LoginPage(loginService, user -> {
            introSeen = false;
            onLogin.accept(user);
        });
    }

    @Override
    public JComponent dashboard(LoginUser user, Runnable openMaps, Runnable logout) {
        // A의 대시보드는 현재 사용자 세션만 받고 기록 데이터는 연동 전으로 표시한다.
        return new DashboardPage(user, openMaps, logout, !introSeen, () -> introSeen = true);
    }

    @Override
    public JComponent maps(LoginUser user, LongConsumer openMap, Runnable dashboard) {
        // C의 카탈로그 결과를 C의 포스터 화면에 전달한다. 현재 실행은 시연용 메모리 데이터다.
        MapSelectionPage page = new MapSelectionPage(openMap, dashboard);
        page.render(mapCatalog.listForUser(user.userId()));
        return page;
    }

    /** DB 시드와 JDBC 드라이버가 준비되기 전까지 C 서비스의 화면 연결을 확인하는 데이터. */
    private static MapCatalogService createDemoMapCatalog() {
        List<GameMap> maps = List.of(
                new GameMap(1, "The Last Compile", "마지막 컴파일을 완성하라.",
                        "Java", "보통", "/images/posters/room01_last_compile.png", true),
                new GameMap(2, "Missing Notes", "흩어진 단서를 모아라.",
                        "Java", "어려움", "/images/posters/room02_missing_notes.png", false),
                new GameMap(3, "Stopped Machine", "멈춘 장치를 다시 켜라.",
                        "Java", "어려움", "/images/posters/room03_stopped_machine.png", false));
        MapRepository mapRepository = new MapRepository() {
            @Override public List<GameMap> findAll() { return maps; }
            @Override public Optional<GameMap> findById(int mapId) {
                return maps.stream().filter(map -> map.getMapId() == mapId).findFirst();
            }
        };
        ClearRecordRepository recordRepository = new ClearRecordRepository() {
            @Override public List<ClearRecord> findByUserId(long userId) { return List.of(); }
            @Override public void saveBest(ClearRecord record) {
                throw new UnsupportedOperationException("시연 카탈로그는 기록을 저장하지 않습니다.");
            }
        };
        return new MapCatalogService(mapRepository, recordRepository);
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

    /** 대시보드와 같은 회색 설정 아이콘 버튼을 게임 사진 위에도 겹쳐 표시한다. */
    private static JButton settingsButton(Runnable openSettings) {
        JButton settings = DashboardPage.settingsIconButton(openSettings);
        settings.setPreferredSize(new Dimension(64, 64));
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
        private final JPanel spacer            = new JPanel();
        private final InventoryPanel inventory = new InventoryPanel();
        private final CluePanel clue           = new CluePanel();

        private DemoWidgets(Runnable back) {
            // 투명한 상단 컨테이너가 설정 버튼만 우상단에 겹쳐 표시한다.
            room.setLayout(new BorderLayout());
            JPanel overlay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 16));
            overlay.setOpaque(false);
            overlay.add(settingsButton(() -> openSettings(back)));
            room.add(overlay, BorderLayout.NORTH);
            inventory.setName("gameInventory");
            clue.setName("gameClue");
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
            bottom.setOpaque(false);
            bottom.add(inventory);
            room.add(bottom, BorderLayout.SOUTH);
            room.add(clue, BorderLayout.WEST);
            // ponytail: 빈 ItemId와 힌트 API가 완성되면 데이터만 전달한다. A에서 대체 구현하지 않는다.
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
            inventory.setOnItemSelected(actions::selectItem);
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
