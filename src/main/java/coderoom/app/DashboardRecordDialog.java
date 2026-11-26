package coderoom.app;

import coderoom.shared.ui.HandCursor;
import coderoom.features.dashboard.DashboardRecords;
import coderoom.widgets.clue.RecordedPuzzleReview;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** 화면 수명과 함께 취소되는 DB 조회. 빠른 방 전환 시 이전 응답을 표시하지 않는다. */
final class DashboardRecordDialog extends JDialog {
    private static final Color INK   = new Color(239, 216, 174);
    private static final Color PAPER = new Color(40, 34, 28);
    private final DashboardRecords records;
    private final String loginId;
    private final JPanel rows                            = new JPanel(new GridLayout(9, 1, 0, 4));
    private final JLabel status                           = label("기록을 불러오는 중…", 16);
    private final JLabel personal                         = label("내 등수 — | 기록 —", 20);
    private final JComboBox<DashboardRecords.Room> rooms  = new JComboBox<>();
    private final JButton retry                          = DashboardPage.imageButton("새로고침", "retry");
    private final JPanel body                            = new JPanel(new BorderLayout(0, 12));
    private SwingWorker<?, ?> worker;
    private boolean loadingRooms;

    DashboardRecordDialog(JComponent owner, DashboardRecords records, String loginId, String title) {
        super(SwingUtilities.getWindowAncestor(owner), title, ModalityType.DOCUMENT_MODAL);
        this.records = records;
        this.loginId = loginId;
        setCursor(HandCursor.openHand());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        boolean ranking  = "리더보드".equals(title);
        boolean calendar = "진척도".equals(title);
        JPanel surface   = ranking ? boardSurface() : new DashboardRecordSurface(calendar);
        if (calendar) {
            status.setForeground(new Color(54, 47, 37));
        }
        surface.setLayout(new BorderLayout(0, 12));
        surface.setBackground(PAPER);
        surface.setBorder(BorderFactory.createEmptyBorder(ranking ? 30 : 64, ranking ? 32 : 36, 32, ranking ? 32 : 36));
        JLabel heading = ranking ? label(title, 28)
                : DashboardPage.imageLabel(title, calendar ? "journal" : "bookshelf");
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(heading, BorderLayout.NORTH);
        rooms.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected javax.swing.plaf.basic.ComboPopup createPopup() {
                var popup = super.createPopup();
                HandCursor.install(popup.getList());
                return popup;
            }

            @Override protected JButton createArrowButton() {
                JButton arrow = new JButton("▼");
                arrow.setFocusPainted(false);
                arrow.setMargin(new java.awt.Insets(0, 0, 0, 0));
                arrow.setBorderPainted(false);
                arrow.setFont(SceneObjectButton.PixelFont.at(16));
                arrow.setBackground(PAPER);
                arrow.setForeground(INK);
                HandCursor.install(arrow);
                return arrow;
            }
        });
        rooms.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setBackground(selected ? new Color(87, 73, 44) : PAPER);
                setForeground(INK);
                return this;
            }
        });
        HandCursor.install(rooms);
        rooms.setFont(SceneObjectButton.PixelFont.at(18));
        rooms.setBackground(PAPER);
        rooms.setForeground(INK);
        rooms.setEnabled(false);
        rooms.getAccessibleContext().setAccessibleName("방 선택");
        if (ranking) {
            north.add(rooms, BorderLayout.CENTER);
        }
        north.add(status, BorderLayout.SOUTH);
        surface.add(north, BorderLayout.NORTH);
        body.setOpaque(false);
        rows.setOpaque(false);
        body.add(rows, BorderLayout.CENTER);
        if (ranking) {
            personal.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            body.add(personal, BorderLayout.SOUTH);
        }
        surface.add(body, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(300, 42));
        JButton close = DashboardPage.imageButton("닫기", "close");
        close.setToolTipText("닫기 · ESC");
        for (JButton button : List.of(retry, close)) {
            button.setFont(SceneObjectButton.PixelFont.at(18));
            button.setBackground(new Color(66, 54, 36));
            button.setForeground(INK);
            actions.add(button);
        }
        close.addActionListener(event -> dispose());
        surface.add(actions, BorderLayout.SOUTH);
        setContentPane(surface);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                if (worker != null) {
                    worker.cancel(true);
                }
            }
        });
        rooms.addActionListener(event -> {
            if (!loadingRooms) {
                loadRanking();
            }
        });
        retry.addActionListener(event -> {
            if (ranking) {
                loadRooms();
            } else {
                loadProgress(title);
            }
        });
        setSize(ranking ? 560 : 620, ranking ? 700 : 620);
        setMinimumSize(new Dimension(500, ranking ? 600 : 560));
        setLocationRelativeTo(owner);
        if (ranking) {
            loadRooms();
        } else {
            loadProgress(title);
        }
    }

    private JPanel boardSurface() {
        BufferedImage board = SceneObjectButton.read("leaderboard-window.png");
        return new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(board, 0, 0, getWidth(), getHeight(), null);
                g.dispose();
            }
        };
    }

    private void loadRooms() {
        rooms.setEnabled(false);
        DashboardRecords.Room previous = (DashboardRecords.Room) rooms.getSelectedItem();
        query(() -> records.rooms(), result -> {
            loadingRooms = true;
            rooms.removeAllItems();
            for (DashboardRecords.Room room : result) {
                rooms.addItem(room);
                if (previous != null && room.id() == previous.id()) {
                    rooms.setSelectedItem(room);
                }
            }
            loadingRooms = false;
            rooms.setEnabled(!result.isEmpty());
            if (result.isEmpty()) {
                status.setText("등록된 방이 없습니다.");
            } else {
                loadRanking();
            }
        });
    }

    private void loadRanking() {
        DashboardRecords.Room room = (DashboardRecords.Room) rooms.getSelectedItem();
        if (room == null) {
            return;
        }
        query(() -> records.ranking(room.id(), loginId), ranking -> {
            rows.removeAll();
            for (int index = 0; index < 9; index++) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(index % 2 == 0 ? new Color(55, 45, 34) : PAPER);
                row.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (index < ranking.leaders().size()) {
                    DashboardRecords.Entry entry = ranking.leaders().get(index);
                    JLabel name                  = label(entry.rank() + "위  " + entry.loginId(), 18);
                    name.setToolTipText(entry.loginId());
                    row.add(name, BorderLayout.CENTER);
                    row.add(label(DashboardRecords.time(entry.seconds()), 18), BorderLayout.EAST);
                    if (entry.loginId().equals(loginId)) {
                        row.setBackground(new Color(87, 73, 44));
                    }
                } else {
                    row.add(label("—   아직 기록 없음", 17));
                }
                rows.add(row);
            }
            personal.setText(ranking.mine().map(entry -> "내 등수 " + entry.rank() + "위 | "
                    + DashboardRecords.time(entry.seconds())).orElse("내 등수 — | 아직 클리어 기록 없음"));
            status.setText(ranking.leaders().isEmpty() ? "첫 번째 탈출 기록을 남겨보세요!" : "최고 기록순 · 동률은 공동 순위");
            rows.revalidate();
            rows.repaint();
            body.revalidate();
            body.repaint();
        });
    }

    private void loadProgress(String title) {
        boolean showProblems = "풀이한 문제".equals(title);
        query(() -> records.roomProgress(loginId), progress -> {
            rows.setLayout(new GridLayout(0, 1, 0, 10));
            for (DashboardRecords.RoomProgress entry : progress) {
                rows.add(showProblems
                        ? DashboardRecordSurface.problemRow(entry, this::showAnswer)
                        : DashboardRecordSurface.progressRow(entry));
            }
            status.setText(progress.isEmpty() ? "등록된 방이 없습니다." : "");
            status.setVisible(progress.isEmpty());
            refreshRows();
        });
    }

    private void showAnswer(DashboardRecords.Solved entry) {
        var review = new JDialog(this, "문제와 풀이", ModalityType.DOCUMENT_MODAL);
        review.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        review.setContentPane(new RecordedPuzzleReview(
                entry.room(), entry.stepNo(), entry.puzzleId(), entry.code(), entry.answer()));
        review.getRootPane().registerKeyboardAction(event -> review.dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        review.pack();
        var screen = getGraphicsConfiguration().getBounds();
        review.setSize(Math.min(review.getWidth(), screen.width - 48),
                Math.min(review.getHeight(), screen.height - 90));
        review.setLocationRelativeTo(this);
        review.setVisible(true);
    }

    private void refreshRows() {
        rows.revalidate();
        rows.repaint();
        body.revalidate();
        body.repaint();
    }

    private <T> void query(Callable<T> operation, Consumer<T> render) {
        if (worker != null) {
            worker.cancel(true);
        }
        rows.removeAll();
        rows.revalidate();
        rows.repaint();
        personal.setText("내 기록 —");
        status.setText("기록을 불러오는 중…");
        status.setVisible(true);
        body.revalidate();
        body.repaint();
        if (records == null) {
            status.setText("기록 서비스가 연결되지 않았습니다.");
            retry.setEnabled(false);
            return;
        }
        SwingWorker<T, Void> request = new SwingWorker<>() {
            @Override protected T doInBackground() throws Exception { return operation.call(); }
            @Override protected void done() {
                if (isCancelled() || worker != this) {
                    return;
                }
                try {
                    render.accept(get());
                } catch (CancellationException ignored) {
                    // 새 방 선택 또는 창 닫기로 취소된 응답은 표시하지 않는다.
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    status.setText("기록 조회 실패 · 새로고침으로 다시 시도하세요.");
                    personal.setText("내 기록을 확인할 수 없습니다.");
                }
            }
        };
        worker = request;
        request.execute();
    }

    private static JLabel label(String text, float size) {
        JLabel label = new JLabel(text);
        label.setFont(SceneObjectButton.PixelFont.at(size));
        label.setForeground(INK);
        return label;
    }
}
