package coderoom.app;

import java.awt.CardLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Taskbar;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 담당: A
 * 역할: 앱에서 사용하는 하나의 최상위 창.
 * 상위 타입: javax.swing.JFrame
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */
public class MainFrame extends JFrame implements AppController.Host {
    // [필드·연결]
    // java.awt.CardLayout과 화면 컨테이너를 보유한다.

    // [구현 예정]
    // showPage(name): 로그인·대시보드·맵 선택·게임 화면을 전환한다.
    // 창 종료 시 현재 GamePage의 타이머와 작업을 정리하도록 콜백을 호출한다.
    // 게임 화면에서는 방이 창의 전체 콘텐츠 영역을 채우도록 배치한다.

    private static final BufferedImage ICON = loadIcon();

    // 같은 이름의 화면을 교체할 때 이전 컴포넌트도 제거하여 계정 데이터를 남기지 않는다.
    private final CardLayout layout             = new CardLayout();
    private final JPanel container              = new JPanel(layout);
    private final Map<String, JComponent> pages = new HashMap<>();
    private BooleanSupplier closeRequest        = () -> true;

    /** 창을 하나만 생성하고 종료 가능 여부는 컨트롤러에 위임한다. EDT에서 호출한다. */
    public MainFrame() {
        super("CODE ROOM");
        requireEdt();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(ICON);
        // 지원되는 데스크톱 환경에서는 Dock·작업 표시줄에도 같은 아이콘을 사용한다.
        if (!GraphicsEnvironment.isHeadless() && Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(ICON);
            }
        }
        setContentPane(container);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                // 저장 중이거나 미저장 기록의 이탈을 취소하면 창을 그대로 유지한다.
                if (closeRequest.getAsBoolean()) {
                    dispose();
                }
            }
        });
    }

    /** 종료 콜백은 게임 자원 정리 후 true, 종료 취소 시 false를 반환한다. */
    public void setCloseRequest(BooleanSupplier closeRequest) {
        requireEdt();
        this.closeRequest = Objects.requireNonNull(closeRequest);
    }

    /** 화면을 등록한다. 같은 이름으로 등록하면 기존 화면은 컨테이너에서 제거한다. */
    public void setPage(String name, JComponent page) {
        requireEdt();
        Objects.requireNonNull(name);
        Objects.requireNonNull(page);
        JComponent previous = pages.get(name);
        if (previous == page) {
            return;
        }
        // 하나의 Swing 컴포넌트를 여러 이름으로 등록하면 CardLayout 상태가 깨진다.
        if (pages.containsValue(page)) {
            throw new IllegalArgumentException("이미 등록된 화면입니다.");
        }
        removePage(name);
        pages.put(name, page);
        container.add(page, name);
        container.revalidate();
        container.repaint();
    }

    /** 로그아웃·게임 교체 시 더 이상 사용하지 않는 화면 참조를 제거한다. */
    public void removePage(String name) {
        requireEdt();
        JComponent previous = pages.remove(name);
        if (previous != null) {
            container.remove(previous);
            container.revalidate();
            container.repaint();
        }
    }

    /** 등록한 화면만 표시하여 잘못된 이름의 전환을 즉시 발견한다. */
    public void showPage(String name) {
        requireEdt();
        if (!pages.containsKey(name)) {
            throw new IllegalArgumentException("등록되지 않은 화면: " + name);
        }
        layout.show(container, name);
    }

    // Swing 화면 변경을 작업 스레드에서 호출하는 실수를 조기에 차단한다.
    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("화면 조작은 EDT에서 실행해야 합니다.");
        }
    }

    /** 앱 아이콘을 클래스패스에서 읽어 실행 위치와 관계없이 적용한다. */
    private static BufferedImage loadIcon() {
        URL resource = MainFrame.class.getResource("/images/app/code-room-icon.png");
        if (resource == null) {
            throw new IllegalStateException("앱 아이콘 이미지가 없습니다.");
        }
        try {
            return ImageIO.read(resource);
        } catch (IOException exception) {
            throw new IllegalStateException("앱 아이콘 이미지를 읽을 수 없습니다.", exception);
        }
    }
}
