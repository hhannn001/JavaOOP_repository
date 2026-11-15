package coderoom.app;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.GameStage;
import coderoom.entities.puzzle.AnswerType;
import coderoom.features.gameplay.GameFacade;
import coderoom.features.gameplay.EscapeResult;
import coderoom.features.gameplay.ItemView;
import coderoom.features.gameplay.ui.AnswerInputPanel;
import coderoom.widgets.result.ResultPanel;
import java.util.function.Supplier;
import javax.swing.JLabel;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.record.ClearRecord;
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
 * 인증·맵·게임 서비스와 위젯을 연결한다.
 * 다른 담당자의 인증·맵 조회·게임 규칙은 이 클래스에서 구현하지 않는다.
 */
final class AppScreens implements AppController.Screens {
    private static final Color DEEP  = new Color(21, 39, 30);
    private static final Color GOLD  = new Color(229, 194, 134);

    private final LoginService loginService;
    private final MapCatalogService mapCatalog;
    private final Supplier<GameFacade> newGame;
    private boolean introSeen;

    AppScreens(LoginService loginService, MapCatalogService mapCatalog, Supplier<GameFacade> newGame) {
        this.loginService = Objects.requireNonNull(loginService);
        this.mapCatalog   = Objects.requireNonNull(mapCatalog);
        this.newGame      = Objects.requireNonNull(newGame);
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
        // DB 조회는 화면의 작업 스레드에서 수행한다.
        MapSelectionPage page = new MapSelectionPage(openMap, dashboard);
        page.load(mapCatalog, user.userId());
        return page;
    }

    @Override
    public GamePage game(LoginUser user, long mapId, Runnable back, Runnable restart) {
        return new GamePage(new GameAdapter(newGame.get()), new GameWidgets(back, restart));
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

    /** Swing의 게임 포트와 화면에 의존하지 않는 Facade를 연결한다. */
    private record GameAdapter(GameFacade facade) implements GamePage.GamePort {
        @Override public GameSetup loadMap(long mapId) { return facade.loadMap(mapId); }
        @Override public GameView start(LoginUser user, GameSetup setup, long nowNanos) {
            return facade.start(user.userId(), setup, nowNanos);
        }
        @Override public GameView view(long nowNanos) { return facade.view(nowNanos); }
        @Override public InspectResult inspect(RoomObjectId object) { return facade.inspect(object); }
        @Override public SubmitResult submit(RoomObjectId object, String answer) { return facade.submit(object, answer); }
        @Override public GameView selectItem(ItemId item) { return facade.selectItem(item); }
        @Override public GamePage.Escape useKey(long nowNanos) {
            EscapeResult escaped = facade.useKey(nowNanos);
            return new GamePage.Escape(escaped.view(), escaped.record(), escaped.message());
        }
        @Override public void saveClear(ClearRecord record) { facade.saveClear(record); }
    }

    /** 방 사진 위의 설정 버튼으로 중앙 메뉴를 열고 나가기만 A의 복귀 콜백에 연결한다. */
    private static final class GameWidgets implements GamePage.Widgets {
        private GamePage actions;
        private final RoomPanel room = new RoomPanel(object -> {
            if (actions != null) {
                if (object == RoomObjectId.DOOR) {
                    actions.useKey();
                } else {
                    actions.inspect(object);
                }
            }
        }, true);
        private final AnswerInputPanel answer = new AnswerInputPanel();
        private final ResultPanel resultPanel = new ResultPanel();
        private final JLabel message          = new JLabel();
        private final JLabel elapsed          = new JLabel();
        private final JPanel resultHolder     = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 80));
        private ItemId displayedItem;
        private RoomObjectId activeObject;
        private boolean busy;
        private final JPanel spacer            = new JPanel();
        private final InventoryPanel inventory = new InventoryPanel();
        private final CluePanel clue           = new CluePanel();

        private GameWidgets(Runnable back, Runnable restart) {
            // 투명한 상단 컨테이너가 설정 버튼만 우상단에 겹쳐 표시한다.
            room.setLayout(new BorderLayout());
            JPanel overlay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 16));
            overlay.setOpaque(false);
            message.setForeground(GOLD);
            elapsed.setForeground(GOLD);
            overlay.add(message);
            overlay.add(elapsed);
            overlay.add(settingsButton(() -> openSettings(back)));
            room.add(overlay, BorderLayout.NORTH);
            inventory.setName("gameInventory");
            clue.setName("gameClue");
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
            bottom.setOpaque(false);
            bottom.add(inventory);
            room.add(bottom, BorderLayout.SOUTH);
            room.add(clue, BorderLayout.WEST);
            answer.setVisible(false);
            answer.setOnClose(() -> activeObject = null);
            room.add(answer, BorderLayout.EAST);
            resultHolder.setOpaque(false);
            resultHolder.setVisible(false);
            resultHolder.add(resultPanel);
            room.add(resultHolder, BorderLayout.CENTER);
            resultPanel.setOnBackToMaps(back);
            resultPanel.setOnRestart(restart);
            // GamePage의 SOUTH 슬롯은 높이를 차지하지 않게 해 사진이 창 아래까지 닿게 한다.
            spacer.setPreferredSize(new Dimension(0, 0));
            spacer.setOpaque(false);
        }

        /** 중앙 팝업에는 추후 메뉴 자리를 마련하고 나가기만 동작하게 한다. */
        private void openSettings(Runnable back) {
            if (busy) {
                return;
            }
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
            inventory.setOnItemSelected(item -> {
                actions.selectItem(item);
                clue.openClue();
            });
            answer.setOnSubmit(value -> actions.submit(activeObject, value));
            resultPanel.setOnRetrySave(actions::retrySave);
        }
        @Override
        public void render(GameView view) {
            inventory.setItems(view.items().stream().map(ItemView::id).toList(),
                    view.items().stream().map(ItemView::title).toList());
            inventory.setSelectedItem(view.selectedItem());
            room.setStage(view.stage());
            elapsed.setText(String.format(java.util.Locale.ROOT, "%02d:%02d",
                    view.elapsedSeconds() / 60, view.elapsedSeconds() % 60));
            if (view.selectedItem() != displayedItem) {
                displayedItem = view.selectedItem();
                view.items().stream().filter(item -> item.id() == displayedItem).findFirst().ifPresent(item -> {
                    clue.setClueData(item);
                    clue.openClue();
                });
            }
            if (view.stage() == GameStage.ESCAPED) {
                clue.closeClue();
                answer.setVisible(false);
            }
        }
        @Override
        public void inspected(InspectResult result) {
            status(result.message());
            if (result.answerType() != null) {
                activeObject = result.object();
                if (result.answerType() == AnswerType.DIGITS4) {
                    answer.showFourDigits();
                } else {
                    answer.showGrid3x3();
                }
                answer.setVisible(true);
                room.revalidate();
            }
        }
        @Override
        public void submitted(SubmitResult result) {
            answer.showMessage(result.message());
            status(result.message());
            if (result.verdict() == SubmitResult.Verdict.SOLVED
                    || result.verdict() == SubmitResult.Verdict.ALREADY_SOLVED) {
                answer.setVisible(false);
                activeObject = null;
                room.revalidate();
            }
        }
        @Override
        public void status(String value) { message.setText(value); }
        @Override
        public void busy(boolean value) {
            busy = value;
            resultPanel.setBusy(value);
        }
        @Override
        public void result(GamePage.Escape escape, GamePage.SaveState state) {
            resultPanel.render(escape.record().getBestSeconds(), ResultPanel.SaveState.valueOf(state.name()));
            resultHolder.setVisible(true);
            room.revalidate();
            room.repaint();
        }
    }
}
