package coderoom.pages.mapselect;

import coderoom.shared.ui.HandCursor;
import coderoom.features.mapcatalog.MapCard;
import coderoom.features.mapcatalog.MapCatalogService;
import coderoom.shared.ui.PixelTheme;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.LongConsumer;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/** 실제 카페의 액자와 안내판 위에 방 정보와 입력 영역을 겹친다. */
public final class MapSelectionPage extends JPanel {
    private static final BufferedImage GALLERY = read("/images/mapselect/gallery.png");
    private static final BufferedImage DETAIL  = read("/images/mapselect/detail-surface.png");
    private static final Color TEXT_PRIMARY   = new Color(255, 243, 220);
    private static final Color TEXT_SECONDARY = new Color(234, 213, 181);
    private static final Color ACTION         = new Color(232, 193, 126);
    private static final Color ACTION_INK     = new Color(58, 36, 20);
    private static final BufferedImage BACK   = read("/images/mapselect/back-button.png");
    private static final BufferedImage TITLE  = read("/images/mapselect/title-theme.png");
    private static final int[] COLUMNS         = {305, 567, 827, 1089};
    private final LongConsumer openMap;
    private final Runnable dashboard;
    private SwingWorker<List<MapCard>, Void> loading;
    private List<MapCard> cards = List.of();
    private MapCard selected;
    private int galleryPage;
    private JPanel popup;
    private final java.util.Map<JComponent, Boolean> enabledBeforePopup = new java.util.HashMap<>();

    public MapSelectionPage() { this(null, null); }

    public MapSelectionPage(LongConsumer openMap, Runnable dashboard) {
        setCursor(HandCursor.openHand());
        this.openMap   = openMap;
        this.dashboard = dashboard;
        setLayout(null);
        setBackground(PixelTheme.DARK);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (popup != null) { closePopup(); }
                else if (selected != null) { showGallery(); }
                else if (dashboard != null) { dashboard.run(); }
            }
        });
    }
    /** 매번 DB에서 최고 기록을 다시 읽고, 실패 시 재시도와 복귀를 제공한다. */
    public void load(MapCatalogService catalog, long userId) {
        if (loading != null) { loading.cancel(true); }
        showLoadStatus("방 목록을 불러오는 중입니다.", null);
        loading = new SwingWorker<>() {
            @Override protected List<MapCard> doInBackground() { return catalog.listForUser(userId); }
            @Override protected void done() {
                if (loading != this || isCancelled()) { return; }
                loading = null;
                try {
                    render(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadStatus("조회가 중단되었습니다.", () -> load(catalog, userId));
                } catch (ExecutionException exception) {
                    showLoadStatus("방 목록을 불러오지 못했습니다.", () -> load(catalog, userId));
                }
            }
        };
        loading.execute();
    }

    private void showLoadStatus(String message, Runnable retry) {
        closePopup();
        selected = null;
        removeAll();
        place(label(message, 27), 350, 420, 840, 70);
        if (retry != null) { place(control("다시 시도", retry), 620, 510, 290, 60); }
        addBack();
        refresh();
    }

    @Override public void removeNotify() {
        if (loading != null) {
            loading.cancel(true);
            loading = null;
        }
        super.removeNotify();
    }

    public void render(List<MapCard> values) {
        cards       = values == null ? List.of() : values.stream().filter(java.util.Objects::nonNull).toList();
        galleryPage = 0;
        showGallery();
    }

    private void showGallery() {
        closePopup();
        selected = null;
        removeAll();
        JLabel title = new JLabel("테마고르기") {
            @Override protected void paintComponent(Graphics graphics) {
                paintAsset(graphics, TITLE, getWidth(), getHeight());
            }
        };
        title.getAccessibleContext().setAccessibleName("테마고르기");
        place(title, 628, 91, 280, 82);
        for (int slot = 0; slot < 8; slot++) {
            int index    = galleryPage * 8 + slot;
            MapCard card = index < cards.size() ? cards.get(index) : null;
            Poster frame = new Poster(card, true);
            if (card != null) { frame.addActionListener(event -> showDetails(card)); }
            place(frame, COLUMNS[slot % 4], slot < 4 ? 309 : 591, 151, 204);
            if (card != null) {
                JLabel status = label(card.isPlayable() ? "입장 가능" : "준비 중", 15);
                status.setForeground(TEXT_PRIMARY);
                status.setBackground(new Color(42, 25, 15));
                status.setOpaque(true);
                place(status, COLUMNS[slot % 4] + 13, slot < 4 ? 519 : 801, 125, 27);
            }
        }
        addBack();
        if (cards.size() > 8) {
            if (galleryPage > 0) {
                place(control("이전 진열대", () -> { galleryPage--; showGallery(); }), 920, 949, 210, 50);
            }
            if ((galleryPage + 1) * 8 < cards.size()) {
                place(control("다음 진열대", () -> { galleryPage++; showGallery(); }), 1150, 949, 210, 50);
            }
        }
        refresh();
    }

    private void showDetails(MapCard card) {
        if (popup != null) { return; }
        MapSelectionPage content = new MapSelectionPage(id -> {
            closePopup();
            if (openMap != null) { openMap.accept(id); }
        }, null);
        content.buildDetails(card, this::closePopup, openMap != null);
        popup = new JPanel(null) {
            @Override protected void paintComponent(Graphics graphics) {
                graphics.setColor(new Color(24, 17, 12, 75));
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
            @Override public void doLayout() {
                int width  = (int) Math.min(getWidth() * 0.97, getHeight() * 0.97 * 16 / 9);
                int height = width * 9 / 16;
                content.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            }
        };
        popup.setOpaque(false);
        popup.setFocusCycleRoot(true);
        popup.setFocusTraversalPolicy(new javax.swing.LayoutFocusTraversalPolicy());
        popup.addMouseListener(new java.awt.event.MouseAdapter() { });
        popup.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });
        popup.add(content);
        for (java.awt.Component child : getComponents()) {
            JComponent component = (JComponent) child;
            enabledBeforePopup.put(component, component.isEnabled());
            component.setEnabled(false);
        }
        content.getActionMap().put("back", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { closePopup(); }
        });
        add(popup, 0);
        refresh();
        javax.swing.SwingUtilities.invokeLater(() -> { if (popup != null) { popup.transferFocusDownCycle(); } });
    }

    private void closePopup() {
        if (popup == null) { return; }
        remove(popup);
        popup = null;
        enabledBeforePopup.forEach(JComponent::setEnabled);
        enabledBeforePopup.clear();
        refresh();
        for (java.awt.Component child : getComponents()) {
            if (child instanceof JButton button && button.isEnabled()) {
                button.requestFocusInWindow();
                break;
            }
        }
    }

    private void buildDetails(MapCard card, Runnable close, boolean canStart) {
        selected = card;
        removeAll();
        place(new Poster(card, false), 24, 24, 544, 816);
        detailText(text(card.getTitle(), "이름 없는 방"), 48, true, TEXT_PRIMARY, 636, 100, 790, 72);
        detailText(text(card.getTopic(), "주제 미정"), 25, false, TEXT_SECONDARY, 636, 181, 600, 40);
        detailText(card.isPlayable() ? "입장 가능" : "준비 중", 22, true,
                card.isPlayable() ? new Color(226, 236, 193) : TEXT_SECONDARY, 1270, 181, 194, 40);
        detailText("난이도", 21, false, TEXT_SECONDARY, 636, 272, 225, 34);
        detailText(text(card.getDifficulty(), "미정"), 31, true, TEXT_PRIMARY, 636, 310, 225, 46);
        detailText("클리어", 21, false, TEXT_SECONDARY, 912, 272, 225, 34);
        detailText(card.isCleared() ? "완료" : "미완료", 31, true, TEXT_PRIMARY, 912, 310, 225, 46);
        detailText("나의 최고 기록", 21, false, TEXT_SECONDARY, 1188, 272, 276, 34);
        detailText(card.getBestSeconds() == null ? "기록 없음" : card.getBestSeconds() + "초",
                31, true, TEXT_PRIMARY, 1188, 310, 276, 46);
        detailText("방 이야기", 23, true, TEXT_PRIMARY, 636, 398, 828, 36);
        JTextArea story = writing(text(card.getDescription(), "방 이야기를 준비하고 있습니다."), 27);
        story.setName("roomStory");
        place(story, 636, 441, 828, 287);
        JButton start = control(card.isPlayable() ? "이 방 시작하기" : "아직 준비 중이에요", () -> {
            if (card.isPlayable() && openMap != null) { openMap.accept(card.getMapId()); }
        });
        start.putClientProperty("primary", true);
        start.putClientProperty("bold", true);
        start.putClientProperty("sceneFont", 28);
        start.setEnabled(card.isPlayable() && canStart);
        place(start, 636, 744, 828, 84);
        JButton closeButton = control("닫기", close);
        closeButton.putClientProperty("sceneFont", 25);
        place(closeButton, 1415, 18, 91, 58);
        refresh();
        start.requestFocusInWindow();
    }

    private void detailText(String text, int size, boolean bold, Color color,
            int x, int y, int width, int height) {
        JLabel value = label(text, size);
        value.setHorizontalAlignment(SwingConstants.LEFT);
        value.setForeground(color);
        value.putClientProperty("bold", bold);
        place(value, x, y, width, height);
    }

    private void addBack() {
        if (dashboard != null) {
            JButton back = new JButton("로비로 돌아가기") {
                @Override protected void paintComponent(Graphics graphics) {
                    paintAsset(graphics, BACK, getWidth(), getHeight());
                    if (isEnabled() && (getModel().isRollover() || hasFocus())) {
                        graphics.setColor(PixelTheme.LIGHT);
                        graphics.drawRoundRect(5, 5, getWidth() - 11, getHeight() - 11, 8, 8);
                    }
                }
            };
            back.setContentAreaFilled(false);
            back.setBorderPainted(false);
            HandCursor.install(back);
            back.setToolTipText("로비로 돌아가기");
            back.getAccessibleContext().setAccessibleName("로비로 돌아가기");
            back.addActionListener(event -> dashboard.run());
            place(back, 38, 30, 132, 74);
        }
    }

    private static void paintAsset(Graphics graphics, BufferedImage asset, int width, int height) {
        if (asset == null) { return; }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale   = Math.min(width / (double) asset.getWidth(), height / (double) asset.getHeight());
        int drawWidth  = (int) (asset.getWidth() * scale);
        int drawHeight = (int) (asset.getHeight() * scale);
        g.drawImage(asset, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight, null);
        g.dispose();
    }

    private static JTextArea writing(String text, int size) {
        JTextArea area = new JTextArea(text);
        area.setFont(PixelTheme.font(size));
        area.setForeground(TEXT_SECONDARY);
        area.setOpaque(false);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.putClientProperty("sceneFont", size);
        return area;
    }

    private static JLabel label(String text, int size) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(PixelTheme.LIGHT);
        label.setFont(PixelTheme.font(size));
        label.putClientProperty("sceneFont", size);
        return label;
    }

    private static JButton control(String text, Runnable action) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean primary = Boolean.TRUE.equals(getClientProperty("primary"));
                if (primary) {
                    g.setColor(!isEnabled() ? new Color(141, 117, 84)
                            : getModel().isPressed() ? new Color(207, 164, 98)
                            : getModel().isRollover() ? new Color(246, 210, 151) : ACTION);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                if (isEnabled() && (getModel().isRollover() || hasFocus())) {
                    g.setColor(new Color(255, 223, 161, 35));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(PixelTheme.LIGHT);
                    g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                }
                g.setFont(getFont());
                int x = (getWidth() - g.getFontMetrics().stringWidth(getText())) / 2;
                int y = (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
                g.setColor(primary ? ACTION_INK : isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);
                g.drawString(getText(), x, y);
                g.dispose();
            }
        };
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        HandCursor.install(button);
        button.setFont(PixelTheme.font(25));
        button.putClientProperty("sceneFont", 25);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void place(JComponent component, int x, int y, int width, int height) {
        component.putClientProperty("sceneBounds", new Rectangle(x, y, width, height));
        add(component);
    }

    private int sceneHeight() { return selected == null ? 1024 : 864; }
    private double scale() { return Math.min(getWidth() / 1536.0, getHeight() / (double) sceneHeight()); }
    private int offsetX() { return (int) ((getWidth() - 1536 * scale()) / 2); }
    private int offsetY() { return (int) ((getHeight() - sceneHeight() * scale()) / 2); }

    @Override public void doLayout() {
        for (java.awt.Component child : getComponents()) {
            JComponent component = (JComponent) child;
            if (component == popup) {
                component.setBounds(0, 0, getWidth(), getHeight());
                continue;
            }
            Rectangle bounds = (Rectangle) component.getClientProperty("sceneBounds");
            component.setBounds(offsetX() + (int) (bounds.x * scale()), offsetY() + (int) (bounds.y * scale()),
                    (int) (bounds.width * scale()), (int) (bounds.height * scale()));
            resizeFont(component);
            if (component instanceof JTextArea story) {
                float size = story.getFont().getSize2D();
                while (story.getPreferredSize().height > story.getHeight() && size > 15) {
                    size -= 1;
                    story.setFont(PixelTheme.font(size));
                }
            }
        }
    }

    private void resizeFont(JComponent component) {
        Object size = component.getClientProperty("sceneFont");
        if (size instanceof Integer value) { component.setFont(PixelTheme.font((float) Math.max(15, value * scale()))
                .deriveFont(Boolean.TRUE.equals(component.getClientProperty("bold")) ? java.awt.Font.BOLD : java.awt.Font.PLAIN)); }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        PixelTheme.pixels(g);
        g.drawImage(selected == null ? GALLERY : DETAIL, offsetX(), offsetY(),
                (int) (1536 * scale()), (int) (sceneHeight() * scale()), null);
        if (selected != null) {
            g.translate(offsetX(), offsetY());
            g.scale(scale(), scale());
            g.setColor(new Color(226, 195, 146, 90));
            g.fillRect(636, 244, 828, 1);
            g.fillRect(636, 390, 828, 1);
        }
        g.dispose();
    }

    private void refresh() { revalidate(); repaint(); }
    private static String text(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private static BufferedImage read(String path) {
        URL resource = MapSelectionPage.class.getResource(path);
        if (resource == null) { return null; }
        try { return ImageIO.read(resource); } catch (IOException exception) { return null; }
    }

    /** 액자 내부에만 그리며, 원래 배경에 그려진 목재와 조명을 유지한다. */
    private static final class Poster extends JButton {
        private final MapCard card;
        private final BufferedImage image;
        private final boolean caption;

        Poster(MapCard card, boolean caption) {
            this.card    = card;
            this.caption = caption;
            String path  = card == null ? null : card.getPosterPath();
            this.image   = path == null || path.isBlank() ? null : read(path.startsWith("/") ? path : "/" + path);
            setText(card == null ? "새로운 방 준비 중" : text(card.getTitle(), "이름 없는 방"));
            setToolTipText(getText());
            getAccessibleContext().setAccessibleName(getText());
            setEnabled(card != null && caption);
            HandCursor.install(this);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            PixelTheme.pixels(g);
            if (image != null) {
                double scale = caption
                        ? Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight())
                        : Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
                int width    = (int) (image.getWidth() * scale);
                int height   = (int) (image.getHeight() * scale);
                g.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
            }
            g.setFont(PixelTheme.font(Math.max(15, getWidth() / 10f)));
            if (card == null || image == null) {
                center(g, "준비 중", getHeight() / 2);
            }
            if (isEnabled() && (getModel().isRollover() || hasFocus())) {
                g.setColor(PixelTheme.LIGHT);
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                g.drawRect(3, 3, getWidth() - 7, getHeight() - 7);
            }
            g.dispose();
        }

        private void center(Graphics2D g, String value, int y) {
            g.setColor(PixelTheme.LIGHT);
            g.drawString(value, (getWidth() - g.getFontMetrics().stringWidth(value)) / 2, y);
        }
    }
}
