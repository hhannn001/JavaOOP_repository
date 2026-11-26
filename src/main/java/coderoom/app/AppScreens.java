package coderoom.app;

import coderoom.shared.audio.GameAudio;
import coderoom.entities.game.ItemId;
import coderoom.shared.db.StorageException;
import coderoom.shared.ui.HandCursor;
import coderoom.shared.ui.GameOverlayLayout;
import coderoom.entities.game.GameSession;
import coderoom.entities.game.GameStage;
import coderoom.entities.game.WorkshopRoom;
import coderoom.entities.puzzle.AnswerType;
import coderoom.features.gameplay.GameFacade;
import coderoom.features.gameplay.EscapeResult;
import coderoom.features.gameplay.ItemView;
import coderoom.features.gameplay.RoomStory;
import coderoom.features.gameplay.ui.AnswerInputPanel;
import coderoom.widgets.result.ResultPanel;
import coderoom.widgets.result.PuzzleProgress;
import coderoom.widgets.result.TimeLimitBar;
import coderoom.widgets.dialogue.DialoguePanel;
import coderoom.widgets.dialogue.DialoguePanel.Line;
import coderoom.widgets.dialogue.DialoguePanel.Speaker;
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
import java.awt.Component;
import java.awt.Container;
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
import javax.swing.JOptionPane;
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
    private final coderoom.features.dashboard.DashboardRecords dashboardRecords;
    private final Supplier<GameFacade> newGame;
    private boolean introSeen;

    AppScreens(LoginService loginService, MapCatalogService mapCatalog, Supplier<GameFacade> newGame) {
        this(loginService, mapCatalog, newGame, null);
    }

    AppScreens(LoginService loginService, MapCatalogService mapCatalog, Supplier<GameFacade> newGame,
            coderoom.features.dashboard.DashboardRecords records) {
        this.dashboardRecords = records;
        this.loginService     = Objects.requireNonNull(loginService, "로그인 서비스가 필요합니다.");
        this.mapCatalog       = Objects.requireNonNull(mapCatalog);
        this.newGame          = Objects.requireNonNull(newGame);
    }

    @Override
    public JComponent login(Consumer<LoginUser> onLogin) {
        GameAudio.playMusic(GameAudio.Music.MENU);
        return new LoginPage(loginService, user -> {
            introSeen = false;
            onLogin.accept(user);
        });
    }

    @Override
    public boolean clearRememberedLogin() {
        try {
            loginService.clearRememberedLogin();
            return true;
        } catch (StorageException exception) {
            showMessage(null, "자동 로그인을 해제하지 못했습니다. 다시 시도하세요.",
                    "로그아웃", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    @Override
    public JComponent dashboard(LoginUser user, Runnable openMaps, Runnable logout) {
        GameAudio.playMusic(GameAudio.Music.MENU);
        // 대시보드의 읽기 전용 기록 조회를 현재 로그인 세션과 연결한다.
        return new DashboardPage(user, openMaps, logout, !introSeen, () -> introSeen = true, dashboardRecords);
    }

    @Override
    public JComponent maps(LoginUser user, LongConsumer openMap, Runnable dashboard) {
        GameAudio.playMusic(GameAudio.Music.MENU);
        // DB 조회는 화면의 작업 스레드에서 수행한다.
        MapSelectionPage page = new MapSelectionPage(openMap, dashboard);
        page.load(mapCatalog, user.userId());
        return page;
    }

    @Override
    public GamePage game(LoginUser user, long mapId, Runnable back, Runnable restart) {
        GamePage page = new GamePage(new GameAdapter(newGame.get()), new GameWidgets(mapId, back, restart));
        page.bindWidgets();
        return page;
    }

    static JButton musicToggle() {
        return audioToggle("배경음", GameAudio::musicEnabled, GameAudio::setMusicEnabled);
    }

    static JButton effectsToggle() {
        return audioToggle("효과음", GameAudio::effectsEnabled, GameAudio::setEffectsEnabled);
    }

    private static JButton audioToggle(String label, java.util.function.BooleanSupplier enabled,
            Consumer<Boolean> setEnabled) {
        JButton toggle = button(label + (enabled.getAsBoolean() ? " 켜짐" : " 꺼짐"));
        toggle.addActionListener(event -> {
            boolean next = !enabled.getAsBoolean();
            setEnabled.accept(next);
            toggle.setText(label + (next ? " 켜짐" : " 꺼짐"));
        });
        return toggle;
    }

    static boolean confirmExit(Component owner) {
        var pane = new JOptionPane("기록이 저장되지 않았습니다. 나가시겠습니까?",
                JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        showOptionPane(owner, "기록 저장 확인", pane);
        return Integer.valueOf(JOptionPane.YES_OPTION).equals(pane.getValue());
    }

    static void showMessage(Component owner, Object message, String title, int type) {
        showOptionPane(owner, title, new JOptionPane(message, type));
    }

    private static void showOptionPane(Component owner, String title, JOptionPane pane) {
        JDialog dialog = pane.createDialog(owner, title);
        dialog.setCursor(HandCursor.openHand());
        installOptionButtons(pane);
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
    }

    private static void installOptionButtons(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button) { HandCursor.install(button); }
            else if (child instanceof Container container) { installOptionButtons(container); }
        }
    }

    private static void showAudioCredits(JDialog owner) {
        try (var source = AppScreens.class.getResourceAsStream("/audio/CREDITS.txt")) {
            if (source == null) { return; }
            String credits = new String(source.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            var text = new javax.swing.JTextArea(credits, 17, 48);
            text.setEditable(false);
            text.setCaretPosition(0);
            showMessage(owner, new javax.swing.JScrollPane(text),
                    "오디오 출처", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException exception) {
            showMessage(owner, "오디오 출처를 읽을 수 없습니다.", "오디오 출처", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JButton button(String title) {
        JButton button = new JButton(title);
        button.setFont(coderoom.shared.ui.PixelTheme.font(17).deriveFont(Font.BOLD));
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        HandCursor.install(button);
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
        @Override public GameSetup loadMap(long mapId, long userId) { return facade.loadMap(mapId, userId); }
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

    /** 방 화면의 설정, 오디오, 장치와 결과 위젯을 연결한다. */
    private static final class GameWidgets implements GamePage.Widgets {
        private GamePage actions;
        private final RoomPanel room;
        private final long mapId;
        private final WorkshopRoom workshop;
        private GameView latest;
        private final AnswerInputPanel answer  = new AnswerInputPanel();
        private final ResultPanel resultPanel  = new ResultPanel();
        private final JLabel message          = new JLabel();
        private final PuzzleProgress progress = new PuzzleProgress();
        private final PuzzleProgress attempts = new PuzzleProgress(true);
        private final TimeLimitBar elapsed     = new TimeLimitBar();
        private final JPanel resultHolder     = new JPanel(new java.awt.GridBagLayout());
        private ItemId displayedItem;
        private RoomObjectId activeObject;
        private boolean busy;
        private final JPanel spacer            = new JPanel();
        private final InventoryPanel inventory = new InventoryPanel();
        private final CluePanel clue           = new CluePanel();
        private final DialoguePanel dialogue  = new DialoguePanel();
        private final JPanel header           = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 16));
        private final JPanel bottom           = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        private final JPanel dialogueClick    = new JPanel();
        private final JPanel dimmer           = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                graphics.setColor(new Color(0, 0, 0, 195));
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        private boolean puzzleFocused;
        private boolean restoreClue;
        private boolean puzzleHasClue;
        private final java.util.Map<java.awt.Component, Boolean> inputEnabled = new java.util.IdentityHashMap<>();

        private boolean outcomeSoundPlayed;

        private GameWidgets(long mapId, Runnable back, Runnable restart) {
            this.mapId    = mapId;
            this.workshop = WorkshopRoom.forMap(mapId);
            room       = new RoomPanel(object -> {
                if (actions != null) {
                    if (object == RoomObjectId.DOOR) {
                        actions.useKey();
                    } else {
                        actions.inspect(object);
                    }
                }
            }, true, mapId);
            room.initialize();
            // 투명한 상단 컨테이너가 설정 버튼만 우상단에 겹쳐 표시한다.
            room.setLayout(new GameOverlayLayout(room.hasSurround()));
            room.setSceneBottomInset(GameOverlayLayout.roomBottomInset());
            header.setOpaque(false);
            message.setForeground(GOLD);
            header.add(message);
            header.add(settingsButton(() -> openSettings(back)));
            room.add(header, "header");
            room.add(elapsed, "timer");
            progress.setName("puzzleProgress");
            room.add(progress, "progress");
            attempts.setName("wrongAttempts");
            room.add(attempts, "attempts");
            inventory.setName("gameInventory");
            clue.setName("gameClue");
            bottom.setOpaque(false);
            bottom.add(inventory);
            room.add(bottom, "inventory");
            room.add(clue, "clue");
            answer.setVisible(false);
            answer.setOnClose(() -> { activeObject = null; updateFocus(); });
            clue.setOnClose(this::syncInvestigation);
            room.add(answer, "input");
            resultHolder.setOpaque(false);
            resultHolder.setVisible(false);
            resultHolder.add(resultPanel);
            room.add(resultHolder, "result");
            room.add(dialogue, "dialogue");
            dimmer.setName("puzzleDimmer");
            dimmer.setOpaque(false);
            dimmer.setVisible(false);
            dimmer.addMouseListener(new java.awt.event.MouseAdapter() { });
            room.add(dimmer, "dimmer");
            dialogueClick.setName("dialogueClickSurface");
            dialogueClick.setOpaque(false);
            dialogueClick.setVisible(false);
            dialogueClick.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseReleased(java.awt.event.MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        GameAudio.play(GameAudio.Cue.CLICK);
                        dialogue.advance();
                    }
                }
            });
            HandCursor.install(dialogueClick);
            room.add(dialogueClick, "dialogueClick");
            dialogue.setOnChanged(this::updateFocus);
            room.setComponentZOrder(dimmer, 0);
            room.setComponentZOrder(elapsed, 0);
            room.setComponentZOrder(progress, 0);
            room.setComponentZOrder(attempts, 0);
            room.setComponentZOrder(clue, 0);
            room.setComponentZOrder(answer, 0);
            room.setComponentZOrder(dialogue, 0);
            room.setComponentZOrder(dialogueClick, 0);
            resultPanel.setOnBackToMaps(back);
            resultPanel.setOnRestart(restart);
            // GamePage의 SOUTH 슬롯은 높이를 차지하지 않게 해 사진이 창 아래까지 닿게 한다.
            spacer.setPreferredSize(new Dimension(0, 0));
            spacer.setOpaque(false);
        }

        /** 퍼즐만 밝게 남기고, 대사를 닫는 클릭이 아래 잠금장치에 전달되지 않게 한다. */
        private void updateFocus() {
            boolean puzzle   = answer.isVisible();
            boolean finished = resultHolder.isVisible();
            boolean speaking = dialogue.isVisible();
            boolean wasSpeaking = dialogueClick.isVisible();
            if (puzzle && !puzzleFocused) {
                restoreClue = clue.isVisible();
            }
            if (puzzle) {
                if (!puzzleFocused) { clue.setVisible(puzzleHasClue); }
            } else if (puzzleFocused) {
                clue.setVisible(restoreClue && !finished && displayedItem != null && displayedItem != ItemId.KEY);
                if (clue.isVisible()) { showNote(displayedItem); }
            }
            puzzleFocused = puzzle;
            dimmer.setVisible(puzzle || finished);
            header.setVisible(!puzzle && !finished);
            elapsed.setVisible(!finished);
            boolean escaping = latest != null && latest.stage() == GameStage.ESCAPED;
            boolean showInventory = !finished && !puzzle && !speaking && !escaping;
            bottom.setVisible(showInventory);
            inventory.setVisible(showInventory);
            dialogueClick.setVisible(speaking);
            if (speaking && !wasSpeaking) {
                suspendInput(answer);
            } else if (!speaking && wasSpeaking) {
                inputEnabled.forEach(java.awt.Component::setEnabled);
                inputEnabled.clear();
            }
            if (puzzle && wasSpeaking && !speaking) { answer.focusInput(); }
            syncInvestigation();
            room.setPuzzleOpen(puzzle && !finished);
            room.revalidate();
            room.repaint();
        }

        private void syncInvestigation() {
            boolean showGauges = !resultHolder.isVisible();
            progress.setVisible(showGauges);
            attempts.setVisible(showGauges);
            room.setInvestigationOpen(answer.isVisible() || clue.isVisible());
        }

        private void suspendInput(java.awt.Container parent) {
            for (java.awt.Component child : parent.getComponents()) {
                inputEnabled.put(child, child.isEnabled());
                child.setEnabled(false);
                if (child instanceof java.awt.Container container) { suspendInput(container); }
            }
        }

        /** 방 안에서 배경음과 효과음을 따로 설정한다. */
        private void openSettings(Runnable back) {
            if (busy) {
                return;
            }
            Window owner = SwingUtilities.getWindowAncestor(room);
            JDialog dialog = new JDialog(owner, "설정", Dialog.ModalityType.DOCUMENT_MODAL);
            JPanel content = new JPanel(new BorderLayout(0, 24));
            content.setBackground(DEEP);
            content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
            JPanel options = new JPanel(new GridLayout(3, 1, 0, 12));
            options.setOpaque(false);
            JButton credits = button("오디오 출처");
            credits.addActionListener(event -> showAudioCredits(dialog));
            options.add(musicToggle());
            options.add(effectsToggle());
            options.add(credits);
            content.add(options, BorderLayout.CENTER);
            JButton exit = button("나가기");
            exit.addActionListener(event -> {
                dialog.dispose();
                back.run();
            });
            content.add(exit, BorderLayout.SOUTH);
            dialog.setCursor(HandCursor.openHand());
            dialog.setContentPane(content);
            dialog.setSize(360, 335);
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
                if (dialogue.isVisible() || answer.isVisible()) { return; }
                GameAudio.play(item == ItemId.KEY ? GameAudio.Cue.METAL : GameAudio.Cue.PAPER);
                actions.selectItem(item);
                if (item == ItemId.KEY) { clue.closeClue(); }
                else { clue.openClue(); }
                syncInvestigation();
            });
            answer.setOnSubmit(value -> actions.submit(activeObject, value));
            answer.setOnBridgeChanged(room::renderBridge);
            resultPanel.setOnRetrySave(actions::retrySave);
        }
        @Override
        public void render(GameView view) {
            latest = view;
            room.setProgress(view.solvedCount(), view.puzzleCount(), false);
            progress.setProgress(view.solvedCount(), view.puzzleCount());
            attempts.setProgress(view.attemptsRemaining(), GameSession.MAX_WRONG_ATTEMPTS);
            inventory.setItems(view.items().stream().map(ItemView::id).toList(),
                    view.items().stream().map(ItemView::title).toList());
            inventory.setSelectedItem(view.selectedItem());
            room.setStage(view.stage());
            room.renderWorkshop(view);
            elapsed.setElapsedSeconds(view.elapsedSeconds());
            if (view.selectedItem() != displayedItem) {
                displayedItem = view.selectedItem();
                if (displayedItem == ItemId.KEY) {
                    clue.closeClue();
                } else {
                    view.items().stream().filter(item -> item.id() == displayedItem).findFirst().ifPresent(item -> {
                        clue.setClueData(item);
                        clue.openClue();
                    });
                }
                syncInvestigation();
            }
            if (view.stage() == GameStage.ESCAPED) {
                answer.setVisible(false);
                dialogue.close();
                clue.closeClue();
                updateFocus();
            }
        }
        private void showNote(ItemId id) {
            if (latest != null) {
                latest.items().stream().filter(item -> item.id() == id).findFirst()
                        .ifPresent(item -> clue.setClueData(item));
            }
        }

        @Override
        public int escapeRevealDelay() {
            return room.escapeRevealDelay();
        }

        @Override
        public void inspected(InspectResult result) {
            status("");
            int evidence = result.object() == null ? -1 : result.object().evidenceIndex();
            if (workshop != null && evidence >= 0) {
                GameAudio.play(GameAudio.Cue.PAPER);
                if (latest != null && latest.evidence().contains(evidence)) {
                    ItemId note = ItemId.notes().get(evidence);
                    actions.selectItem(note);
                    showNote(note);
                    clue.openClue();
                    syncInvestigation();
                }
                return;
            }
            GameAudio.play(result.object() == RoomObjectId.HINT_NOTE ? GameAudio.Cue.PAPER : GameAudio.Cue.CLICK);
            if (result.answerType() == null) {
                if (workshop != null) { status(result.message()); }
                else { dialogue.showLines(new Line(Speaker.PLAYER, result.message())); }
            } else if (dialogue.isVisible()) {
                dialogue.close();
            }
            if (result.object() == RoomObjectId.HINT_NOTE) {
                if (latest != null && displayedItem != null && displayedItem != ItemId.KEY) {
                    SwingUtilities.invokeLater(() -> showNote(displayedItem));
                }
                return;
            }
            if (result.answerType() != null) {
                activeObject = result.object();
                if (result.answerType() == AnswerType.WORKSHOP && workshop != null) {
                    int index = java.util.stream.IntStream.range(0, 5)
                            .filter(value -> workshop.task(value).device() == activeObject).findFirst().orElseThrow();
                    answer.showWorkshop(workshop, index, result.variant(), latest == null ? 0 : latest.platformDirection());
                    puzzleHasClue = result.instructions() != null;
                    if (puzzleHasClue) { clue.setClueData(result.instructions()); }
                } else {
                    puzzleHasClue = latest != null;
                    answer.showPuzzle(result.answerType());
                    if (latest != null && latest.solvedCount() < latest.puzzleCount()) {
                        showNote(ItemId.notes().get(latest.solvedCount()));
                    }
                }
                answer.setVisible(true);
                updateFocus();
            }
        }
        @Override
        public void submitted(SubmitResult result) {
            switch (result.verdict()) {
                case SOLVED -> GameAudio.play(GameAudio.Cue.SOLVED);
                case WRONG, INVALID, BLOCKED -> GameAudio.play(GameAudio.Cue.LOCKED);
                default -> { }
            }
            answer.showMessage(result.message());
            message.setText("");
            if (workshop != null) {
                if (result.verdict() == SubmitResult.Verdict.WRONG) { answer.rattleLock(); }
                if (result.verdict() == SubmitResult.Verdict.SOLVED) {
                    answer.setVisible(false);
                    RoomObjectId solved = activeObject;
                    activeObject = null;
                    restoreClue  = false;
                    clue.closeClue();
                    dialogue.showLines(new Line(Speaker.SYSTEM, result.message()),
                            new Line(Speaker.PLAYER, RoomStory.afterRepair(workshop, solved, result.view().solvedSteps())));
                    return;
                }
            } else if (result.verdict() == SubmitResult.Verdict.WRONG) {
                answer.showMessage(answer.rattleLock() ? "" : "이건 아닌 것 같아. 메모의 코드를 다시 따라가 보자.");
            } else if (result.verdict() == SubmitResult.Verdict.SOLVED) {
                dialogue.showLines(new Line(Speaker.SYSTEM, result.message()),
                        new Line(Speaker.PLAYER, result.view().stage() == GameStage.DRAWER_OPEN
                                ? (mapId == 1 ? "또 다른 메모야. 이번엔 벽의 금고를 열 수 있겠어."
                                        : "다음 장치에 쓸 메모를 확인하자.")
                                : "드디어 열쇠를 찾았어! 인벤토리에서 선택하고 문을 열자."));
            } else {
                dialogue.showLines(new Line(Speaker.SYSTEM, result.message()));
            }
            if (result.verdict() == SubmitResult.Verdict.SOLVED
                    || result.verdict() == SubmitResult.Verdict.ALREADY_SOLVED) {
                answer.setVisible(false);
                activeObject = null;
                updateFocus();
            }
        }
        @Override
        public void status(String value) { message.setText(value); }
        @Override
        public void escapeFailed(String value) {
            GameAudio.play(GameAudio.Cue.LOCKED);
            dialogue.showLines(new Line(Speaker.PLAYER, workshop == null
                    ? "잠겨 있어… 열쇠를 찾아서 선택한 뒤 다시 열어 보자." : value));
            message.setText("");
        }
        @Override
        public void started() {
            outcomeSoundPlayed = false;
            GameAudio.playRoom(mapId);
            clue.closeClue();
            room.setInvestigationOpen(false);
            if (workshop != null) {
                dialogue.showLines(new Line(Speaker.SYSTEM, workshop == WorkshopRoom.ENGINE
                        ? "야간 정비 중 증기가 터졌다. 비상 차단기가 내려가고 중앙 제어기가 출구를 잠근다."
                        : "폭우에 온실 레일이 잠겼다. 새벽에 옮기기로 한 씨앗이 아직 작업대에 남아 있다."),
                        new Line(Speaker.PLAYER, workshop == WorkshopRoom.ENGINE
                                ? "문부터 열 순 없겠어. 왼쪽 전력 랙을 살려야 배관도 분배함도 점검할 수 있어."
                                : "여기 두면 씨앗까지 젖겠어. 쓸 수 있는 봉투를 골라 높은 출구까지 옮기자."));
                return;
            }
            if (mapId != 1) {
                dialogue.showLines(new Line(Speaker.SYSTEM, mapId == 3
                        ? "중앙 제어 시스템이 멈췄다. 다섯 장치를 차례로 복구해 출구를 열자."
                        : "사라진 기록을 추적한다. 서류함에서 시작해 증거를 정리하고 출구를 열자."),
                        new Line(Speaker.PLAYER, "접힌 메모를 조사하면 막힌 장치의 힌트를 주울 수 있어."));
                return;
            }
            dialogue.showLines(
                    new Line(Speaker.SYSTEM, "오래된 연구자의 서재. 당신은 ‘마지막 코드를 풀어 달라’는 초대장을 따라 이곳에 왔다."),
                    new Line(Speaker.PLAYER, "들어오자마자 문이 잠겼어. 초대장에는 이런 얘기가 없었는데…"),
                    new Line(Speaker.SYSTEM, "출입문 위 장치가 켜진다. ‘보안 시험 시작. 제한시간은 %d분입니다. 메모의 코드로 서랍과 금고를 열고, 열쇠를 찾아 탈출하십시오.’"
                            .formatted(GameSession.TIME_LIMIT_SECONDS / 60)),
                    new Line(Speaker.PLAYER, "우선 손에 든 메모부터 읽어 보자. 이 방의 물건들에 단서가 있을 거야."));
        }
        @Override
        public void busy(boolean value) {
            busy = value;
            resultPanel.setBusy(value);
        }
        @Override
        public void timeExpired() {
            playOutcome(GameAudio.Cue.TIMEOUT);
            activeObject = null;
            answer.setVisible(false);
            dialogue.close();
            clue.closeClue();
            updateFocus();
            bottom.setVisible(false);
            inventory.setVisible(false);
        }

        @Override
        public void result(GamePage.Escape escape, GamePage.SaveState state) {
            GameAudio.Cue cue = escape.record() != null ? GameAudio.Cue.ESCAPED
                    : escape.view().attemptsExhausted() ? GameAudio.Cue.LOCKED : GameAudio.Cue.TIMEOUT;
            playOutcome(cue);
            resultHolder.setVisible(true);
            clue.closeClue();
            answer.setVisible(false);
            dialogue.close();
            inventory.setVisible(false);
            syncInvestigation();
            room.setInvestigationOpen(true);
            if (escape.record() == null) {
                if (escape.view().attemptsExhausted()) {
                    resultPanel.renderFailure("남은 오답 기회 0/" + GameSession.MAX_WRONG_ATTEMPTS, escape.message());
                } else {
                    resultPanel.renderFailure();
                }
            } else {
                resultPanel.render(escape.record().getBestSeconds(), ResultPanel.SaveState.valueOf(state.name()));
            }
            room.setComponentZOrder(resultHolder, 0);
            resultHolder.setVisible(true);
            room.revalidate();
            room.repaint();
        }

        @Override
        public void disposed() {
            GameAudio.stopMusic();
        }

        private void playOutcome(GameAudio.Cue cue) {
            if (outcomeSoundPlayed) { return; }
            outcomeSoundPlayed = true;
            GameAudio.stopMusic();
            GameAudio.play(cue);
        }
    }
}
