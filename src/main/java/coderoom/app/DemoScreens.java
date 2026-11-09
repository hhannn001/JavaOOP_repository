package coderoom.app;

import coderoom.entities.game.ItemId;
import coderoom.entities.game.RoomObjectId;
import coderoom.entities.record.ClearRecord;
import coderoom.entities.user.LoginUser;
import coderoom.features.gameplay.GameSetup;
import coderoom.features.gameplay.GameView;
import coderoom.features.gameplay.InspectResult;
import coderoom.features.gameplay.SubmitResult;
import coderoom.pages.game.GamePage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;

/** A의 화면 전환을 실행해 볼 수 있는 임시 화면. B~G의 기능을 대신 구현하지 않는다. */
final class DemoScreens implements AppController.Screens {
    private static final Color DEEP  = new Color(21, 39, 30);
    private static final Color GOLD  = new Color(229, 194, 134);
    private static final Color PAPER = new Color(233, 231, 206);

    @Override
    public JComponent login(Consumer<LoginUser> onLogin) {
        JPanel page = page();
        JButton enter = button("맵 선택 화면으로 이동");
        // 사용자 값은 화면 전환 계약에만 사용하며 인증 성공으로 취급하지 않는다.
        enter.addActionListener(event -> onLogin.accept(new LoginUser()));
        page.add(actions(enter), BorderLayout.SOUTH);
        return page;
    }

    @Override
    public JComponent maps(LoginUser user, LongConsumer openMap, Runnable logout) {
        JPanel page = page();
        JPanel gallery = new JPanel(new GridBagLayout());
        gallery.setBackground(DEEP);
        gallery.add(new PosterPreview());
        page.add(gallery, BorderLayout.CENTER);
        JButton enter = button("ROOM 01 · 서재 화면 보기");
        enter.addActionListener(event -> openMap.accept(1L));
        JButton exit = button("시작 화면으로 돌아가기");
        exit.addActionListener(event -> logout.run());
        page.add(actions(enter, exit), BorderLayout.SOUTH);
        return page;
    }

    @Override
    public GamePage game(LoginUser user, long mapId, Runnable back, Runnable restart) {
        // GamePage의 비동기 로딩·타이머 수명은 실제 A 코드로 확인한다.
        return new GamePage(new DemoGame(), new DemoWidgets(back, restart));
    }

    private static JPanel page() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(DEEP);
        page.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        JLabel heading = new JLabel("임시 진입점 구현", SwingConstants.CENTER);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
        heading.setForeground(GOLD);
        page.add(heading, BorderLayout.NORTH);
        return page;
    }

    /** 이동 버튼을 창 하단에 고정해 화면 크기가 달라도 찾을 수 있게 한다. */
    private static JPanel actions(JButton... buttons) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        actions.setBackground(DEEP);
        for (JButton button : buttons) {
            actions.add(button);
        }
        return actions;
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
            throw new UnsupportedOperationException("물건 조사는 게임 기능 연동 후 사용할 수 있습니다.");
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

    /** 방 이미지 위에 안내를 얹고, 복귀·재시작 버튼만 A의 콜백에 연결한다. */
    private static final class DemoWidgets implements GamePage.Widgets {
        private final JPanel room     = new RoomBackdrop();
        private final JPanel controls = new JPanel(new BorderLayout());
        private final JLabel status   = new JLabel("임시 진입점 구현", SwingConstants.CENTER);

        private DemoWidgets(Runnable back, Runnable restart) {
            controls.setBackground(DEEP);
            controls.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            status.setForeground(PAPER);
            controls.add(status, BorderLayout.CENTER);
            JButton goBack = button("맵 선택으로");
            goBack.addActionListener(event -> back.run());
            controls.add(goBack, BorderLayout.WEST);
            JButton again = button("화면 다시 열기");
            again.addActionListener(event -> restart.run());
            controls.add(again, BorderLayout.EAST);
        }

        @Override
        public JComponent room() { return room; }
        @Override
        public JComponent controls() { return controls; }
        @Override
        public void bind(GamePage actions) { }
        @Override
        public void render(GameView view) { }
        @Override
        public void inspected(InspectResult result) { }
        @Override
        public void submitted(SubmitResult result) { }
        @Override
        public void status(String message) {
            // 시연 화면에서는 내부 로딩 메시지를 표시하지 않고 요청한 문구를 유지한다.
        }
        @Override
        public void busy(boolean busy) {
            // GamePage가 작업 중 화면 이동을 차단하므로 버튼의 이벤트 경로도 안전하다.
        }
        @Override
        public void result(GamePage.Escape escape, GamePage.SaveState state) { }
    }

    /** 포스터는 장식으로만 표시하며 입장 가능 여부는 화면의 버튼이 결정한다. */
    private static final class PosterPreview extends JPanel {
        private final BufferedImage image;

        private PosterPreview() {
            setPreferredSize(new Dimension(195, 280));
            URL resource = DemoScreens.class.getResource("/images/posters/room01_last_compile.png");
            if (resource == null) {
                throw new IllegalStateException("서재 포스터 이미지가 없습니다.");
            }
            try {
                image = ImageIO.read(resource);
            } catch (IOException exception) {
                throw new IllegalStateException("서재 포스터 이미지를 읽을 수 없습니다.", exception);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            graphics.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        }
    }

    /** 이미지 자체에 클릭 처리를 넣지 않아 D의 RoomPanel과 책임이 겹치지 않게 한다. */
    private static final class RoomBackdrop extends JPanel {
        private final BufferedImage image;

        private RoomBackdrop() {
            setBackground(DEEP);
            setPreferredSize(new Dimension(960, 600));
            URL resource = DemoScreens.class.getResource("/images/room/study_locked.png");
            if (resource == null) {
                throw new IllegalStateException("서재 시연 이미지가 없습니다.");
            }
            try {
                image = ImageIO.read(resource);
            } catch (IOException exception) {
                throw new IllegalStateException("서재 시연 이미지를 읽을 수 없습니다.", exception);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // 창 크기가 바뀌어도 원본 비율을 유지하고 남는 여백을 가운데 정렬한다.
                double scale = Math.min((double) getWidth() / image.getWidth(),
                        (double) getHeight() / image.getHeight());
                int width  = (int) Math.round(image.getWidth() * scale);
                int height = (int) Math.round(image.getHeight() * scale);
                int x      = (getWidth() - width) / 2;
                int y      = (getHeight() - height) / 2;
                canvas.drawImage(image, x, y, width, height, null);
            } finally {
                canvas.dispose();
            }
        }
    }
}
