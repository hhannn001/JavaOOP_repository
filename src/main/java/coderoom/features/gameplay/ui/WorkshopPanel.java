package coderoom.features.gameplay.ui;

import coderoom.entities.game.WorkshopRoom;
import coderoom.features.gameplay.PuzzleVariant;
import coderoom.shared.ui.PixelTheme;
import coderoom.shared.ui.SpecimenMark;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Mounts each puzzle's input directly on the pictured equipment. */
final class WorkshopPanel extends JPanel {
    private final PropBoard board;
    private final PuzzleVariant variant;
    private final JLabel feedback = new JLabel(" ", JLabel.CENTER);
    private PropBoard.Control finishControl;
    private Runnable activate = () -> { };
    private java.util.function.Consumer<String> submitFailedRoute = route -> { };
    private Runnable cancel = () -> { };
    private Runnable interactionChanged = () -> { };
    private Supplier<String> response;
    private Runnable restore = () -> { };

    WorkshopPanel(WorkshopRoom room, int index, PuzzleVariant variant, int platformDirection) {
        super(new BorderLayout(0, 6));
        this.variant = variant;
        setName("workshopPuzzle");
        setOpaque(false);
        feedback.setName("workshopFeedback");
        feedback.setVisible(false);
        feedback.addPropertyChangeListener("text", event ->
                getAccessibleContext().setAccessibleDescription(feedback.getText()));
        board = new PropBoard(room, index);
        add(board, BorderLayout.CENTER);
        add(feedback, BorderLayout.SOUTH);
        if ((room == WorkshopRoom.ENGINE && index != 2 && index != 3)
                || (room == WorkshopRoom.GREENHOUSE && (index == 0 || index == 2))) {
            finishControl = control("workshopStart", "장치 작동", 891, 566, 87, 84, () -> activate.run());
            finishControl.sprite(PropBoard.read("/images/game-ui/study/latch.png"));
        }
        if (room == WorkshopRoom.ENGINE) { buildEngine(index); }
        else { buildGreenhouse(index, platformDirection); }
    }

    int artworkTop() {
        doLayout();
        return board.getY() + board.imageBounds().y;
    }

    private void buildEngine(int index) {
        switch (index) {
            case 0 -> power();
            case 1 -> sleeves();
            case 2 -> distribution();
            case 3 -> pressure();
            case 4 -> interlock();
            default -> throw new IllegalArgumentException("Unknown machine room device " + index);
        }
    }

    private void buildGreenhouse(int index, int platformDirection) {
        switch (index) {
            case 0 -> seeds();
            case 1 -> gutters();
            case 2 -> platforms();
            case 3 -> balance();
            case 4 -> railway(platformDirection == 1);
            default -> throw new IllegalArgumentException("Unknown greenhouse device " + index);
        }
    }

    private void power() {
        boolean[] connected = new boolean[5];
        int[] centers       = {169, 337, 503, 670, 837};
        int[] socketCenters = {173, 338, 504, 670, 836};
        PropBoard.Control[] toggles = new PropBoard.Control[5];
        BufferedImage up   = PropBoard.sprite("toggle-up");
        BufferedImage down = PropBoard.sprite("toggle-down");
        for (int i = 0; i < toggles.length; i++) {
            int module   = i;
            String label = (char) ('A' + i) + " · " + PuzzleVariant.moduleVoltage(i) + "V · " + (PuzzleVariant.operational(i) ? "정상" : "고장");
            toggles[i] = control("module" + i, label, centers[i] - 46, 478, 92, 97, () -> {
                connected[module] = !connected[module];
                toggles[module].sprite(connected[module] ? up : down);
                board.repaint();
            });
            toggles[i].sprite(down);
        }
        board.effects(g -> {
            for (int i = 0; i < connected.length; i++) {
                int x = centers[i];
                String mark = PuzzleVariant.operational(i) ? "✓" : "×";
                String plate = (char) ('A' + i) + " " + PuzzleVariant.moduleVoltage(i) + "V " + mark;
                PropBoard.nameplate(g, plate, x, 443, 19);
                if (connected[i]) { powerGlow(g, socketCenters[i], 225); }
            }
        });
        response = () -> {
            StringBuilder state = new StringBuilder();
            for (int i = 0; i < connected.length; i++) {
                if (connected[i]) { state.append((char) ('A' + i)); }
            }
            return state.toString();
        };
        restore = () -> {
            Arrays.fill(connected, false);
            for (PropBoard.Control toggle : toggles) { toggle.sprite(down); }
        };
    }

    private static void powerGlow(Graphics2D graphics, int x, int y) {
        Graphics2D light = (Graphics2D) graphics.create();
        try {
            light.translate(x, y);
            light.scale(1, 1.35);
            light.setPaint(new RadialGradientPaint(0, 0, 43,
                    new float[] {0, .3f, .68f, 1},
                    new Color[] {new Color(255, 182, 62, 148), new Color(255, 160, 44, 103),
                            new Color(245, 123, 29, 40), new Color(245, 123, 29, 0)}));
            light.fill(new Ellipse2D.Double(-43, -43, 86, 86));
            light.scale(.24, 1);
            light.setPaint(new RadialGradientPaint(0, 0, 27,
                    new float[] {0, .22f, .55f, 1},
                    new Color[] {new Color(255, 246, 207, 247), new Color(255, 217, 139, 213),
                            new Color(255, 172, 66, 110), new Color(255, 172, 66, 0)}));
            light.fill(new Ellipse2D.Double(-27, -27, 54, 54));
        } finally {
            light.dispose();
        }
    }

    private void sleeves() {
        int[][] readings = variant.sleeveReadings();
        boolean[][] measured = new boolean[2][4];
        int[] valve = {0};
        int[] probe = {0};
        String[] names = {"A · |", "B · |||", "C · ||", "D · |"};
        PropBoard.Control[] joints = new PropBoard.Control[4];
        for (int i = 0; i < joints.length; i++) {
            int joint = i;
            joints[i] = control("joint" + i, names[i], 143 + i * 218, 237, 64, 64, () -> {
                probe[0] = joint;
                for (int j = 0; j < joints.length; j++) { joints[j].lit(j == joint); }
                measured[valve[0]][joint] = true;
                showProbe(readings, measured, valve[0], joint);
                board.repaint();
            });
        }
        BufferedImage probeKnob = PropBoard.sprite("knob");
        for (PropBoard.Control joint : joints) { joint.sprite(probeKnob); }
        joints[0].lit(true);
        BufferedImage collar   = PropBoard.sprite("sleeve-collar");
        DragPuzzlePanel pieces = new DragPuzzlePanel("ABCD", 1, collar,
                new String[] {"|", "||", "|||", "▮"});
        pieces.setName("sleeveBoard");
        pieces.getAccessibleContext().setAccessibleName("슬리브 왼쪽부터 한 줄, 두 줄, 세 줄, 넓은 띠");
        board.place(pieces, 88, 369, 824, 265);
        BufferedImage meter = PropBoard.sprite("dial");
        control("probeValve", "검침 밸브 열기 / 닫기", 460, 72, 81, 82, () -> {
            valve[0] = 1 - valve[0];
            measured[valve[0]][probe[0]] = true;
            showProbe(readings, measured, valve[0], probe[0]);
            board.repaint();
        }).sprite(PropBoard.sprite("handwheel"));
        board.effects(g -> {
            PropBoard.sprite(g, meter, 794, 33, 155, 160);
            pressureNeedle(g, 871, 112, 36, readings[valve[0]][probe[0]] / 100.0);
            if (valve[0] == 1) { powerGlow(g, 500, 167); }
            for (int i = 0; i < names.length; i++) {
                int x = 175 + i * 218;
                PropBoard.engraving(g, names[i], x, 343, 23);
                PropBoard.engraving(g, probeReading(readings, measured, 0, i) + " / "
                        + probeReading(readings, measured, 1, i), x, 368, 25);
            }
            PropBoard.engraving(g, "수리 대상 " + (char) ('A' + probe[0]), 500, 398, 22);
        });
        response = () -> {
            int joint = probe[0];
            if (!measured[0][joint] || !measured[1][joint] || pieces.answer().isEmpty()) { return ""; }
            return (char) ('A' + joint) + pieces.answer();
        };
        restore = () -> {
            Arrays.fill(measured[0], false);
            Arrays.fill(measured[1], false);
            valve[0] = 0;
            joints[0].doClick(0);
            pieces.reset();
        };
        joints[0].doClick(0);
    }

    private void showProbe(int[][] readings, boolean[][] measured, int valve, int joint) {
        feedback.setText("이음쇠 " + (char) ('A' + joint) + (valve == 0 ? " · 닫힘 " : " · 열림 ")
                + probeReading(readings, measured, valve, joint));
    }

    private static String probeReading(int[][] readings, boolean[][] measured, int valve, int joint) {
        return measured[valve][joint] ? Integer.toString(readings[valve][joint]) : "–";
    }

    private void distribution() {
        String[] devices = {"환풍기", "냉각기", "펌프", "예비 장치", "작업등"};
        int capacity     = variant.distributionCapacity();
        var sequence     = new StringBuilder();
        boolean[] tested = {false};
        int[] remaining  = {capacity};
        int[] tripped    = {-1};
        BufferedImage up   = PropBoard.sprite("toggle-up");
        BufferedImage down = PropBoard.sprite("toggle-down");
        PropBoard.Control[] breakers = new PropBoard.Control[devices.length];
        for (int i = 0; i < devices.length; i++) {
            char key = (char) ('A' + i);
            String label = key + " · " + devices[i] + " " + PuzzleVariant.deviceWatts(i) + "W" + (PuzzleVariant.operational(i) ? "" : " 고장");
            breakers[i] = control("breaker" + key, label, 205 + i * 125, 482, 90, 104, () -> {
                int selected = sequence.indexOf(String.valueOf(key));
                tested[0]    = false;
                tripped[0]   = -1;
                remaining[0] = capacity;
                feedback.setText(" ");
                if (selected < 0) { sequence.append(key); }
                else { sequence.deleteCharAt(selected); }
                breakers[key - 'A'].sprite(selected < 0 ? up : down);
                board.repaint();
            });
            breakers[i].sprite(down);
        }
        finishControl = control("breakerTrial", "가동 실행", 22, 536, 168, 100, () -> {
            remaining[0] = capacity;
            tripped[0]   = -1;
            tested[0]    = !sequence.isEmpty();
            for (int i = 0; i < sequence.length(); i++) {
                int device = sequence.charAt(i) - 'A';
                if (!PuzzleVariant.operational(device) || PuzzleVariant.deviceWatts(device) > remaining[0]) {
                    tripped[0] = device;
                    String reason = !PuzzleVariant.operational(device) ? "고장 장비" : "남은 전력 부족";
                    feedback.setText(devices[device] + "에서 시험 중지 · " + reason);
                    board.repaint();
                    activate.run();
                    return;
                }
                remaining[0] -= PuzzleVariant.deviceWatts(device);
            }
            feedback.setText(tested[0] ? "가동 시험 완료 · 남은 전력 " + remaining[0] + "W" : "먼저 가동 순서를 입력하세요.");
            board.repaint();
            if (tested[0]) { activate.run(); }
        });
        finishControl.sprite(PropBoard.sprite("red-button"));
        board.effects(g -> {
            if (!sequence.isEmpty()) {
                String reserved = String.join("  ", sequence.toString().split(""));
                PropBoard.readout(g, reserved, 500, 248, 40);
            }
            PropBoard.readout(g, remaining[0] + " / " + capacity + " W", 500, 335, 36);
            for (int i = 0; i < devices.length; i++) {
                int x = 250 + i * 125;
                String plate = (char) ('A' + i) + " · " + PuzzleVariant.deviceWatts(i) + "W"
                        + (PuzzleVariant.operational(i) ? "" : " ×");
                PropBoard.engraving(g, plate, x, 643, 21);
                if (i == tripped[0]) { faultLamp(g, x, 457); }
            }
        });
        response = () -> tested[0] ? sequence.toString() : "";
        restore = () -> {
            sequence.setLength(0);
            tested[0]    = false;
            remaining[0] = capacity;
            tripped[0]   = -1;
            for (PropBoard.Control breaker : breakers) { breaker.sprite(down); }
        };
    }

    private void pressure() {
        var planned = new StringBuilder();
        boolean[] released = {false};
        boolean[] rejected = {false};
        PropBoard.Control[] levers = new PropBoard.Control[4];
        BufferedImage raised  = PropBoard.sprite("lever-up");
        BufferedImage lowered = PropBoard.sprite("lever-down");
        for (int i = 0; i < levers.length; i++) {
            char key     = (char) ('A' + i);
            String label = key + "  −" + variant.pressureDrop(key);
            levers[i] = control("lever" + key, label, 143 + i * 188, 329, 130, 210, () -> {
                int previous = planned.indexOf(String.valueOf(key));
                if (previous >= 0) { planned.deleteCharAt(previous); }
                else { planned.append(key); }
                released[0] = false;
                rejected[0] = false;
                levers[key - 'A'].sprite(previous < 0 ? lowered : raised);
                feedback.setText("예약 순서  " + planned);
                board.repaint();
            });
            levers[i].sprite(raised);
            JLabel gaugeLabel = new JLabel(label, JLabel.CENTER);
            gaugeLabel.setName("leverReading" + key);
            gaugeLabel.setForeground(new Color(255, 225, 172));
            gaugeLabel.setFont(PixelTheme.font(15).deriveFont(java.awt.Font.BOLD));
            board.place(gaugeLabel, 125 + i * 188, 533, 160, 48);
        }
        finishControl = control("pressureTrial", "배출 실행", 449, 581, 102, 81, () -> {
            if (planned.length() != levers.length) {
                rejected[0] = true;
                feedback.setText("레버 네 개를 모두 예약하세요.");
            } else if (!variant.pressureOrder().contentEquals(planned)) {
                rejected[0] = true;
                feedback.setText("안전 조건 불일치 · 예약 순서를 확인하세요.");
                activate.run();
            } else {
                released[0] = true;
                feedback.setText("압력 정상");
                activate.run();
            }
            board.repaint();
        });
        finishControl.sprite(PropBoard.sprite("red-button"));
        board.effects(g -> {
            pressureNeedle(g, 503, 164, 86, released[0] ? 0 : 1);
            if (rejected[0]) { faultLamp(g, 571, 617); }
            for (int i = 0; i < planned.length(); i++) {
                PropBoard.engraving(g, String.valueOf(planned.charAt(i)), 320 + i * 120, 369, 30);
            }
        });
        response = () -> planned.length() == levers.length ? planned.toString() : "";
        restore = () -> {
            planned.setLength(0);
            released[0] = false;
            rejected[0] = false;
            for (PropBoard.Control lever : levers) { lever.sprite(raised); }
        };
    }

    private void interlock() {
        String[] names = {"전력", "밀폐", "부하", "압력"};
        String[] signals = {"power", "sealed", "loadOk", "pressOk"};
        Color[] cable = {new Color(232, 174, 67), new Color(111, 190, 210),
                         new Color(229, 124, 88), new Color(168, 196, 112)};
        int[] ports = {-1, -1, -1, -1};
        int[] source = {-1};
        int[] cut        = {-1};
        int[] prediction = {-1};
        boolean[] verified = new boolean[2];
        boolean[] lamps = new boolean[2];
        int[] sockets   = {221, 351, 654, 784};
        int[] terminals = {217, 406, 594, 785};
        Point[] looseEnd = {null};
        BufferedImage socket       = PropBoard.sprite("socket");
        BufferedImage plug         = PropBoard.sprite("plug");
        BufferedImage insertedPlug = PropBoard.sprite("plug-inserted");
        PropBoard.Control[] inputs = new PropBoard.Control[4];
        PropBoard.Control[] outputs = new PropBoard.Control[4];
        Runnable refreshWiring = () -> {
            for (int i = 0; i < ports.length; i++) {
                int wire = ports[i];
                inputs[i].describe(wire < 0 ? "빈 소켓 " + (i + 1)
                        : "연결된 신호 " + names[wire] + " · 드래그해서 옮기기");
            }
            cut[0]        = -1;
            prediction[0] = -1;
            Arrays.fill(verified, false);
            interactionChanged.run();
            updateInterlock(ports, lamps, cut[0], names);
        };
        java.util.function.IntConsumer select = wire -> {
            if (source[0] != wire) { prediction[0] = -1; }
            source[0] = wire;
            board.repaint();
            for (int i = 0; i < outputs.length; i++) { outputs[i].lit(i == wire); }
        };
        java.util.function.IntConsumer attach = slot -> {
            if (source[0] < 0) { return; }
            int displaced = ports[slot];
            for (int i = 0; i < ports.length; i++) {
                if (i != slot && ports[i] == source[0]) {
                    ports[i] = displaced;
                    break;
                }
            }
            ports[slot] = source[0];
            refreshWiring.run();
        };
        for (int i = 0; i < ports.length; i++) {
            int slot = i;
            inputs[i] = control("port" + i, "빈 소켓 " + (i + 1), sockets[i] - 33, 250, 66, 66,
                    () -> {
                        if (source[0] < 0 && ports[slot] >= 0) { select.accept(ports[slot]); }
                        else { attach.accept(slot); }
                    });
            inputs[i].hotspot();
            outputs[i] = control("signal" + i, names[i] + " (" + signals[i] + ")", terminals[i] - 46, 460, 92, 92,
                    () -> select.accept(slot));
            outputs[i].sprite(plug);
            MouseAdapter drag = new MouseAdapter() {
                @Override public void mouseDragged(MouseEvent event) {
                    if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) { return; }
                    var control = (PropBoard.Control) event.getComponent();
                    int wire    = control == inputs[slot] ? ports[slot] : slot;
                    if (wire < 0) { return; }
                    control.getModel().setArmed(false);
                    control.getModel().setPressed(false);
                    select.accept(wire);
                    looseEnd[0] = board.designPoint(SwingUtilities.convertPoint(control, event.getPoint(), board));
                    board.repaint();
                }

                @Override public void mouseReleased(MouseEvent event) {
                    if (looseEnd[0] == null) { return; }
                    Point drop = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), board);
                    looseEnd[0] = null;
                    for (int port = 0; port < inputs.length; port++) {
                        if (inputs[port].getBounds().contains(drop)) {
                            attach.accept(port);
                            return;
                        }
                    }
                    if (event.getComponent() == inputs[slot]) {
                        ports[slot] = -1;
                        refreshWiring.run();
                    }
                    board.repaint();
                }
            };
            outputs[i].addMouseListener(drag);
            outputs[i].addMouseMotionListener(drag);
            inputs[i].addMouseListener(drag);
            inputs[i].addMouseMotionListener(drag);
        }
        for (int lamp = 0; lamp < lamps.length; lamp++) {
            int channel = lamp;
            int x       = lamp == 0 ? 344 : 659;
            String name = lamp == 0 ? "가동 표시등" : "안전 표시등";
            control("predict" + lamp, name + "이 계속 켜져 있다고 예측", x - 53, 66, 106, 102, () -> {
                prediction[0] = channel;
                feedback.setText("예측 · " + name + " 켜짐");
                board.repaint();
            }).hotspot();
        }
        control("faultTest", "차단 시험", 410, 59, 180, 99, () -> {
            int failure = source[0];
            if (failure < 0 || Arrays.stream(ports).anyMatch(port -> port < 0)) {
                feedback.setText("네 신호를 연결한 뒤 시험할 플러그를 선택하세요.");
                return;
            }
            cut[0] = cut[0] == failure ? -1 : failure;
            interactionChanged.run();
            updateInterlock(ports, lamps, cut[0], names);
            if (cut[0] >= 0 && prediction[0] >= 0
                    && circuitConnected(ports, 0, -1)
                    && circuitConnected(ports, 1, -1)) {
                int circuit = variant.interlockPairs().indexOf('A' + failure) / 2;
                if (lamps[prediction[0]]) { verified[circuit] = true; }
                feedback.setText(feedback.getText() + (lamps[prediction[0]] ? " · 예측 일치" : " · 예측 다시 확인"));
            }
            prediction[0] = -1;
            board.repaint();
        }).sprite(PropBoard.sprite("red-button"));
        board.effects(g -> {
            deviceLight(g, 344, 119, lamps[0]);
            deviceLight(g, 659, 119, lamps[1]);
            if (verified[0]) { powerGlow(g, 394, 156); }
            if (verified[1]) { powerGlow(g, 709, 156); }
            PropBoard.engraving(g, "running", 344, 184, 25);
            PropBoard.engraving(g, "safe", 659, 184, 25);
            if (prediction[0] >= 0) {
                int x = prediction[0] == 0 ? 344 : 659;
                g.setColor(new Color(249, 206, 134));
                g.drawOval(x - 49, 70, 98, 98);
            }
            for (int i = 0; i < ports.length; i++) {
                PropBoard.sprite(g, socket, sockets[i] - 47, 236, 94, 94);
            }
            for (int i = 0; i < ports.length; i++) {
                int wire = ports[i];
                if (wire >= 0 && (looseEnd[0] == null || wire != source[0])) {
                    drawWire(g, cut[0] == wire ? cable[wire].darker().darker() : cable[wire],
                            terminals[wire], sockets[i], 305);
                    PropBoard.sprite(g, insertedPlug, sockets[i] - 21, 267, 42, 40);
                }
            }
            Point pointer = looseEnd[0];
            if (pointer != null) {
                drawWire(g, cable[source[0]], terminals[source[0]], pointer.x, pointer.y + 23);
                PropBoard.sprite(g, plug, pointer.x - 21, pointer.y - 24, 42, 48);
            }
            for (int i = 0; i < terminals.length; i++) {
                g.setColor(cable[i]);
                g.fillOval(terminals[i] - 6, 551, 12, 12);
                signalMark(g, i, terminals[i], 576);
                PropBoard.engraving(g, signals[i], terminals[i], 611, 25);
                if (cut[0] == i) { faultLamp(g, terminals[i], 454); }
            }
        });
        response = () -> {
            if (Arrays.stream(ports).anyMatch(value -> value < 0)) { return ""; }
            String wiring = sortedPair(ports[0], ports[1]) + sortedPair(ports[2], ports[3]);
            if (!circuitConnected(ports, 0, -1)
                    || !circuitConnected(ports, 1, -1)) { return wiring; }
            if (!verified[0] || !verified[1] || cut[0] >= 0) {
                feedback.setText("두 회로를 시험하고 신호를 복구하세요.");
                return "";
            }
            return wiring;
        };
        restore = () -> {
            Arrays.fill(ports, -1);
            Arrays.fill(lamps, false);
            source[0]     = -1;
            cut[0]        = -1;
            prediction[0] = -1;
            Arrays.fill(verified, false);
            looseEnd[0] = null;
            for (int i = 0; i < inputs.length; i++) { inputs[i].describe("빈 소켓 " + (i + 1)); }
            for (PropBoard.Control output : outputs) { output.lit(false); }
        };
    }

    private static void drawWire(Graphics2D g, Color color, int origin, int x, int y) {
        var line = new CubicCurve2D.Double(origin, 505, origin, 398, x, (y + 505) / 2.0, x, y);
        g.setColor(new Color(34, 24, 20));
        g.setStroke(new BasicStroke(15, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);
        g.setColor(color);
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);
    }

    private static void deviceLight(Graphics2D g, int x, int y, boolean on) {
        g.setColor(on ? new Color(255, 229, 157, 75) : new Color(25, 18, 11, 182));
        g.fillOval(x - 43, y - 43, 86, 86);
    }

    private static void faultLamp(Graphics2D g, int x, int y) {
        g.setColor(new Color(255, 81, 46, 67));
        g.fillOval(x - 13, y - 13, 26, 26);
        g.setColor(new Color(254, 123, 66));
        g.fillOval(x - 6, y - 6, 12, 12);
    }

    private void updateInterlock(int[] ports, boolean[] lamps, int failed, String[] names) {
        lamps[0] = circuitConnected(ports, 0, failed);
        lamps[1] = circuitConnected(ports, 1, failed);
        if (Arrays.stream(ports).anyMatch(port -> port < 0)) {
            feedback.setText(" ");
        } else {
            String prefix = failed < 0 ? "" : names[failed] + " 신호 차단 · ";
            feedback.setText(prefix + "가동 " + (lamps[0] ? "켜짐" : "꺼짐")
                    + " · 안전 " + (lamps[1] ? "켜짐" : "꺼짐"));
        }
        board.repaint();
    }

    private boolean circuitConnected(int[] ports, int circuit, int failed) {
        int index   = circuit * 2;
        String pair = variant.interlockPairs();
        int first   = pair.charAt(index) - 'A';
        int second  = pair.charAt(index + 1) - 'A';
        int left    = ports[index];
        int right   = ports[index + 1];
        return failed != first && failed != second
                && ((left == first && right == second) || (left == second && right == first));
    }

    private void seeds() {
        SpecimenMark[][] labels = {
            {SpecimenMark.ROUND, SpecimenMark.DAMAGED, SpecimenMark.TRIANGLE},
            {SpecimenMark.SERRATED, SpecimenMark.INTACT, SpecimenMark.SQUARE},
            {SpecimenMark.ROUND, SpecimenMark.INTACT, SpecimenMark.TRIANGLE},
            {SpecimenMark.SERRATED, SpecimenMark.INTACT, SpecimenMark.WAVE},
            {SpecimenMark.ROUND, SpecimenMark.INTACT, SpecimenMark.WAVE}
        };
        var marks = new java.util.EnumMap<SpecimenMark, BufferedImage>(SpecimenMark.class);
        for (SpecimenMark mark : SpecimenMark.values()) { marks.put(mark, PropBoard.read(mark.imagePath())); }
        boolean[] chosen = new boolean[labels.length];
        BufferedImage envelope = PropBoard.sprite("seed-closed");
        BufferedImage opened   = PropBoard.sprite("seed-open");
        PropBoard.Control[] packets = new PropBoard.Control[labels.length];
        for (int i = 0; i < packets.length; i++) {
            int seed = i;
            String description = String.join(", ", Arrays.stream(labels[variant.seedIndex(i)]).map(SpecimenMark::description).toList());
            packets[i] = control("seed" + i, description, 39 + i * 181, 266, 167, 266, () -> {
                chosen[seed] = !chosen[seed];
                packets[seed].sprite(chosen[seed] ? opened : envelope);
            });
            packets[i].sprite(envelope);
        }
        board.effects(g -> {
            for (int i = 0; i < packets.length; i++) {
                int x = 123 + i * 181;
                for (int mark = 0; mark < labels[i].length; mark++) {
                    g.drawImage(marks.get(labels[variant.seedIndex(i)][mark]), x - 60 + mark * 43, 533, 32, 32, null);
                }
            }
        });
        response = () -> {
            var selected = new StringBuilder();
            for (int i = 0; i < chosen.length; i++) {
                if (chosen[i]) { selected.append((char) ('A' + i)); }
            }
            return selected.toString();
        };
        restore = () -> {
            Arrays.fill(chosen, false);
            for (PropBoard.Control packet : packets) { packet.sprite(envelope); }
        };
    }

    private void gutters() {
        var gutters = new GutterControls(board, variant, () -> activate.run());
        response = gutters::answer;
        restore  = gutters::reset;
        cancel   = gutters::stop;
    }

    private void platforms() {
        String instructions = "가로 발판을 왼쪽으로, 세로 발판을 아래로, 가로 발판을 오른쪽으로 옮기세요.";
        feedback.setForeground(PixelTheme.LIGHT);
        feedback.setFont(PixelTheme.font(13));
        feedback.setVisible(true);
        finishControl.describe("발판과 레일 연결 확인");
        RailPlatformPanel rail = new RailPlatformPanel(
                PropBoard.sprite("cross-platform"), PropBoard.sprite("long-platform"));
        Runnable updateStatus = () -> {
            String answer = rail.answer();
            if (answer.isEmpty()) {
                feedback.setText(instructions);
            } else if (answer.length() > 2) {
                feedback.setText("가로·세로 레일 끝을 고정 레일에 맞춰 주세요.");
            } else {
                feedback.setText("레일이 맞았습니다. 황동 걸쇠를 누르세요.");
            }
        };
        PropBoard.Control horizontal = new PropBoard.Control("adjustHorizontalRail", "가로 레일 조절");
        PropBoard.Control vertical   = new PropBoard.Control("adjustVerticalRail", "세로 레일 조절");
        horizontal.addActionListener(event -> {
            interactionChanged.run();
            rail.adjustRail(0, horizontal);
            updateStatus.run();
        });
        vertical.addActionListener(event -> {
            interactionChanged.run();
            rail.adjustRail(1, vertical);
            updateStatus.run();
        });
        board.place(horizontal, 744, 393, 185, 43);
        board.place(vertical, 422, 487, 181, 43);
        board.place(rail, 0, 0, PropBoard.WIDTH, PropBoard.HEIGHT);
        response = () -> {
            updateStatus.run();
            return rail.answer();
        };
        restore = () -> {
            feedback.setText(instructions);
            rail.reset();
            rail.describeRail(0, horizontal);
            rail.describeRail(1, vertical);
        };
        restore.run();
    }

    private void balance() {
        var balance = new BalanceControls(board, variant.balanceLoad(), () -> activate.run());
        response = balance::answer;
        restore  = balance::reset;
        cancel   = balance::stop;
    }

    private void railway(boolean crossingConnected) {
        int[] position      = {0};
        boolean[] tested    = {false};
        boolean[] returning = {false};
        String[] testRoute  = {""};
        String initialDescription = "빈 운반차 · "
                + (variant.railFlooded() ? "낮은 선로 침수" : "낮은 선로 배수됨")
                + (variant.railBlocked() ? " · 본선에 정지턱" : " · 본선 정지턱 없음");
        Point dock     = new Point(90, 286);
        Point bridge   = new Point(151, 286);
        Point fork     = new Point(225, 286);
        Point crossing = new Point(478, 183);
        Point lower    = new Point(526, 394);
        Point upperEnd = new Point(683, 216);
        Point lowerEnd = new Point(682, 361);
        Point junction = new Point(705, 284);
        Point exit     = new Point(805, 283);
        Point mainExit = new Point(817, 184);
        Point flood    = new Point(352, 346);
        Point blocked  = new Point(768, 184);
        BufferedImage emptyCart  = PropBoard.sprite("trolley-empty");
        BufferedImage loadedCart = PropBoard.sprite("trolley");
        BufferedImage bufferStop = PropBoard.sprite("rail-buffer");
        var trolley = new PropBoard.Control("pushTrolley", "빈 운반차 밀기");
        trolley.sprite(emptyCart);
        trolley.setEnabled(crossingConnected);
        board.place(trolley, dock.x, dock.y, 125, 92);
        var motion = new CartMotion(board, trolley, dock);
        PropBoard.Control latch = control("workshopStart", "운반이 끝나면 황동 걸쇠 누르기",
                879, 507, 77, 72, () -> activate.run());
        finishControl = latch;
        latch.sprite(PropBoard.read("/images/game-ui/study/latch.png"));
        latch.setEnabled(false);

        PropBoard.Selector first = new PropBoard.Selector("firstTurn", value -> {
            if (position[0] != 0) {
                position[0] = 0;
                motion.place(dock);
                latch.setEnabled(false);
                latch.lit(false);
            }
            board.repaint();
        }, "낮은 레일", "높은 레일");
        PropBoard.Selector second = new PropBoard.Selector("secondTurn", value -> {
            if (position[0] == 2) {
                position[0] = 1;
                motion.place(first.value() == 1 ? crossing : lower);
                latch.setEnabled(false);
                latch.lit(false);
            }
            board.repaint();
        }, "출구 쪽 분기", "본선");
        BufferedImage switchLeft  = PropBoard.sprite("rail-left");
        BufferedImage switchRight = PropBoard.sprite("rail-right");
        board.place(first.control(), 229, 333, 116, 89);
        board.place(second.control(), 721, 316, 116, 89);
        first.sprites(switchLeft, switchRight);
        second.sprites(switchLeft, switchRight);
        first.control().hideTooltip();
        second.control().hideTooltip();
        trolley.hideTooltip();
        latch.hideTooltip();
        java.util.function.Consumer<Boolean> controls = enabled -> {
            first.control().setEnabled(enabled);
            second.control().setEnabled(enabled);
            trolley.setEnabled(enabled && crossingConnected);
        };
        trolley.addActionListener(event -> {
            if (!crossingConnected || motion.isMoving() || position[0] == 2) { return; }
            controls.accept(false);
            Runnable resume = () -> controls.accept(true);
            String route = testRoute[0] + letters(first, second);
            Runnable reject = () -> {
                resume.run();
                submitFailedRoute.accept(route);
            };
            boolean flooded     = !tested[0] && variant.railFlooded();
            boolean bufferAhead = !tested[0] && variant.railBlocked();
            boolean high        = first.value() == 1;
            boolean side        = second.value() == 0;
            Point bend          = high ? new Point(312, 249) : flood;
            Point approach      = high ? crossing : lower;
            Point endOfTrack    = high ? upperEnd : lowerEnd;
            if (position[0] == 0) {
                if (high != flooded) {
                    motion.travel(reject, bridge, fork, high ? new Point(263, 264) : flood, fork, bridge, dock);
                } else {
                    motion.travel(() -> { position[0] = 1; resume.run(); }, bridge, fork, bend, approach);
                }
                return;
            }

            if (side != bufferAhead) {
                if (!side) { motion.travel(reject, endOfTrack, blocked, endOfTrack, approach); }
                else { motion.travel(reject, endOfTrack, junction, exit, junction, endOfTrack, approach); }
            } else if (!tested[0]) {
                motion.travel(() -> {
                    testRoute[0] = letters(first, second);
                    returning[0] = true;
                    Point returnTurn = side || !high ? junction : new Point(776, 184);
                    motion.travel(() -> {
                        position[0]  = 0;
                        tested[0]    = true;
                        returning[0] = false;
                        trolley.sprite(loadedCart);
                        trolley.describe("씨앗을 실은 운반차 · 선로 배수됨 · 본선 정지턱 없음");
                        resume.run();
                    }, returnTurn, endOfTrack, approach, bend, fork, bridge, dock);
                }, endOfTrack, side || !high ? junction : new Point(776, 184), side ? exit : mainExit);
            } else {
                motion.travel(() -> {
                    position[0] = 2;
                    latch.setEnabled(true);
                    latch.lit(true);
                    resume.run();
                }, lowerEnd, junction, new Point(776, 215), mainExit);
            }
        });
        trolley.describe(initialDescription);
        BufferedImage bridgeDeck    = PropBoard.sprite("cross-platform");
        BufferedImage bridgeSupport = PropBoard.sprite("long-platform");
        board.effects(g -> {
            g.setColor(new Color(47, 32, 25));
            g.fillRect(182, 298, 66, 68);
            if (crossingConnected) {
                PropBoard.sprite(g, bridgeSupport, 199, 350, 32, 54);
                PropBoard.sprite(g, bridgeDeck, 182, 298, 66, 68);
                RailPlatformPanel.track(g, 182, 68, 309, 354, 66);
            }
            double cleared = tested[0] ? 1 : returning[0] ? motion.progress() : 0;
            if (cleared < 1) {
                if (variant.railFlooded()) { floodedTrack(g, 1 - cleared); }
            }
            if (cleared < 1 && variant.railBlocked()) {
                Graphics2D recess = (Graphics2D) g.create();
                try {
                    recess.clipRect(876, 188, 45, 82);
                    PropBoard.sprite(recess, bufferStop, 876, 188 - (int) (82 * cleared), 45, 82);
                } finally {
                    recess.dispose();
                }
            }
            turnout(g, 294, 333, 391, first.value() == 0 ? 384 : 284);
            turnout(g, 766, 331, second.value() == 0 ? 869 : 838, second.value() == 0 ? 331 : 225);
        });
        response = () -> position[0] == 2 ? testRoute[0] + letters(first, second) : "";
        restore = () -> {
            position[0]  = 0;
            tested[0]    = false;
            returning[0] = false;
            testRoute[0] = "";
            first.set(0);
            second.set(0);
            motion.place(dock);
            trolley.sprite(emptyCart);
            trolley.describe(initialDescription);
            latch.setEnabled(false);
            latch.lit(false);
            controls.accept(true);
        };
        cancel = () -> {
            if (motion.isMoving()) {
                motion.place(position[0] == 0 ? dock : first.value() == 1 ? crossing : lower);
                returning[0] = false;
                controls.accept(true);
            }
        };
    }

    private static void floodedTrack(Graphics2D graphics, double amount) {
        Graphics2D pool = (Graphics2D) graphics.create();
        try {
            var water = new java.awt.geom.Path2D.Double();
            water.moveTo(361, 403);
            water.curveTo(397, 386, 473, 413, 519, 411);
            water.curveTo(566, 412, 632, 407, 669, 422);
            water.curveTo(685, 442, 655, 463, 602, 468);
            water.curveTo(526, 465, 467, 470, 402, 456);
            water.curveTo(373, 452, 348, 430, 361, 403);
            pool.clip(water);
            pool.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) amount));
            pool.setPaint(new java.awt.GradientPaint(420, 393, new Color(41, 91, 108, 50),
                    440, 467, new Color(12, 49, 62, 183)));
            pool.fill(water);
            pool.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            pool.setColor(new Color(161, 214, 223, 115));
            pool.drawArc(382, 418, 128, 21, 184, 153);
            pool.drawArc(486, 438, 144, 19, 194, 148);
            pool.drawArc(580, 417, 82, 26, 13, 170);
        } finally {
            pool.dispose();
        }
    }

    private static void turnout(Graphics2D graphics, int x, int y, int endX, int endY) {
        Graphics2D rails = (Graphics2D) graphics.create();
        try {
            for (int offset : new int[] {-12, 12}) {
                rails.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                rails.setColor(new Color(55, 33, 23));
                rails.drawLine(x, y + offset, endX, endY + offset);
                rails.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                rails.setColor(new Color(171, 113, 58));
                rails.drawLine(x, y + offset, endX, endY + offset);
                rails.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                rails.setColor(new Color(255, 216, 145, 218));
                rails.drawLine(x, y + offset - 2, endX, endY + offset - 2);
            }
        } finally {
            rails.dispose();
        }
    }

    private static final class CartMotion {
        private final PropBoard board;
        private final PropBoard.Control cart;
        private final Timer timer;
        private Point current;
        private Point origin;
        private Point[] legs;
        private Runnable arrived;
        private static final int TICKS_PER_LEG = 12;
        private int tick;

        CartMotion(PropBoard board, PropBoard.Control cart, Point initial) {
            this.board = board;
            this.cart  = cart;
            current    = initial;
            timer      = new Timer(16, event -> advance());
        }

        void place(Point point) {
            timer.stop();
            current = point;
            board.move(cart, point.x, point.y);
        }

        boolean isMoving() { return timer.isRunning(); }

        double progress() { return Math.min(1, tick / (double) (TICKS_PER_LEG * legs.length)); }

        void travel(Runnable afterArrival, Point... route) {
            origin  = current;
            legs    = route;
            arrived = afterArrival;
            tick    = 0;
            timer.restart();
        }

        private void advance() {
            int leg = tick / TICKS_PER_LEG;
            if (leg == legs.length) {
                place(legs[leg - 1]);
                arrived.run();
                return;
            }
            Point from = leg == 0 ? origin : legs[leg - 1];
            Point to   = legs[leg];
            double part = ++tick % TICKS_PER_LEG / (double) TICKS_PER_LEG;
            if (part == 0) { part = 1; }
            current = new Point((int) (from.x + (to.x - from.x) * part),
                    (int) (from.y + (to.y - from.y) * part));
            board.move(cart, current.x, current.y);
        }
    }

    private static void signalMark(Graphics2D g, int signal, int x, int y) {
        g.setColor(PixelTheme.LIGHT);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (signal) {
            case 0 -> g.drawPolyline(new int[] {x + 4, x - 7, x + 3, x - 5},
                    new int[] {y - 12, y, y, y + 12}, 4);
            case 1 -> g.fillPolygon(new int[] {x, x + 10, x, x - 10},
                    new int[] {y - 13, y + 4, y + 12, y + 4}, 4);
            case 2 -> {
                g.fillRect(x - 13, y + 1, 5, 9);
                g.fillRect(x - 3, y - 5, 5, 15);
                g.fillRect(x + 7, y - 11, 5, 21);
            }
            case 3 -> {
                g.drawArc(x - 12, y - 10, 24, 21, 0, 180);
                g.drawLine(x, y + 2, x + 7, y - 5);
            }
            default -> throw new IllegalArgumentException("Unknown signal " + signal);
        }
    }

    private PropBoard.Control control(String name, String label, int x, int y, int w, int h, Runnable pressed) {
        var control = new PropBoard.Control(name, label);
        control.hideTooltip();
        control.addActionListener(event -> {
            interactionChanged.run();
            pressed.run();
        });
        board.place(control, x, y, w, h);
        return control;
    }

    private static void pressureNeedle(Graphics2D g, int x, int y, int length, double value) {
        double angle = Math.toRadians(150 + 240 * value);
        g.setColor(new Color(77, 38, 25));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x, y, x + (int) (length * Math.cos(angle)), y + (int) (length * Math.sin(angle)));
        g.fillOval(x - 6, y - 6, 12, 12);
    }

    private static String letters(PropBoard.Selector... selectors) {
        var result = new StringBuilder();
        for (PropBoard.Selector selector : selectors) { result.append(selector.letter()); }
        return result.toString();
    }

    private static String sortedPair(int left, int right) {
        return new String(new char[] {(char) ('A' + Math.min(left, right)), (char) ('A' + Math.max(left, right))});
    }

    void onStart(Runnable action) {
        activate = action;
    }

    void onFailedRoute(java.util.function.Consumer<String> submit) { submitFailedRoute = submit; }

    void onInteractionChanged(Runnable action) { interactionChanged = action; }

    String answer() { return response.get(); }

    void resist() {
        if (finishControl != null) { finishControl.resist(); }
        getAccessibleContext().setAccessibleDescription("아직 걸쇠가 움직이지 않습니다.");
    }

    void cancel() { cancel.run(); }

    void reset() {
        feedback.setText(" ");
        restore.run();
        board.repaint();
    }
}
