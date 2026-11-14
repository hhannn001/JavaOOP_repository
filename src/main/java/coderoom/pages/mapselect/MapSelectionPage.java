package coderoom.pages.mapselect;

/**
 * 담당: C
 * 역할: 포스터형 맵 선택 화면
 * 상위 타입: javax.swing.JPanel
 * 설계 초안: 필드·생성자·메서드는 아래 주석을 기준으로 구현한다.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import coderoom.features.mapcatalog.MapCard;

public class MapSelectionPage extends JPanel {

	// [연결]
    // coderoom.features.mapcatalog.MapCatalogService와 현재 사용자 ID를 전달받는다.
    // 맵 선택은 LongConsumer 콜백으로 상위에 알린다.

    // [구현 예정]
    // render(cards): 플레이 가능한 포스터 1개와 준비 중 포스터 2개를 표시한다.
    // 클리어 여부·최고 기록을 표시하고 준비 중 맵의 입장을 막는다.
    // 목록 조회는 SwingWorker로 처리하고 게임 복귀 시 기록을 다시 읽는다.

    // [경계]
    // app.AppController나 다른 pages 슬라이스를 직접 import하지 않는다.
	
    public MapSelectionPage() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
    }

    // 맵 목록을 화면에 표시
    public void render(List<MapCard> cards) {

        removeAll();

        // 화면 제목
        JLabel title = new JLabel(
            "CODE ROOM - MAP SELECT",
            SwingConstants.CENTER
        );

        title.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        title.setBorder(BorderFactory.createEmptyBorder(25, 10, 25, 10));

        add(title, BorderLayout.NORTH);

        // 맵 목록이 없거나 비어 있는 경우
        if (cards == null || cards.isEmpty()) {
            showEmptyMessage("표시할 맵이 없습니다.");
            return;
        }

        // null인 맵 데이터 제외
        List<MapCard> validCards = new ArrayList<>();

        for (MapCard card : cards) {
            if (card != null) {
                validCards.add(card);
            }
        }

        if (validCards.isEmpty()) {
            showEmptyMessage("맵 정보를 불러오지 못했습니다.");
            return;
        }

        // 포스터를 가로로 배치
        JPanel posterPanel = new JPanel(
            new GridLayout(1, validCards.size(), 20, 10)
        );

        posterPanel.setBackground(Color.WHITE);
        posterPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 30, 30, 30)
        );

        for (MapCard card : validCards) {

            JPanel cardPanel = new JPanel(new BorderLayout(5, 10));
            cardPanel.setBackground(Color.WHITE);
            cardPanel.setBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1)
            );

            // 포스터 이미지
            JLabel posterLabel = new JLabel(
                "이미지를 불러올 수 없습니다.",
                SwingConstants.CENTER
            );

            posterLabel.setPreferredSize(new Dimension(200, 250));

            String posterPath = card.getPosterPath();

            if (posterPath != null && !posterPath.trim().isEmpty()) {

                String resourcePath = posterPath.startsWith("/")
                    ? posterPath
                    : "/" + posterPath;

                try {
                    URL imageURL = getClass().getResource(resourcePath);

                    if (imageURL != null) {

                        ImageIcon originalIcon = new ImageIcon(imageURL);

                        // 이미지가 정상적으로 읽혔는지 확인
                        if (originalIcon.getIconWidth() > 0) {

                            Image image = originalIcon.getImage()
                                .getScaledInstance(
                                    200, 250, Image.SCALE_SMOOTH
                                );

                            posterLabel.setText("");
                            posterLabel.setIcon(new ImageIcon(image));
                        }
                    }

                } catch (RuntimeException ex) {
                    System.out.println(
                        "포스터 이미지 조회 실패: " + resourcePath
                    );
                }
            }

            cardPanel.add(posterLabel, BorderLayout.NORTH);

            // 맵 정보
            String titleText = safeText(card.getTitle(), "제목 없음");
            String description = safeText(
                card.getDescription(), "설명 없음"
            );
            String topic = safeText(card.getTopic(), "정보 없음");
            String difficulty = safeText(
                card.getDifficulty(), "정보 없음"
            );

            String status = card.isPlayable()
                ? "입장 가능"
                : "준비 중";

            String record = card.getBestSeconds() == null
                ? "기록 없음"
                : card.getBestSeconds() + "초";

            JLabel infoLabel = new JLabel(
                "<html><center>"
                + "<b>" + escapeHtml(titleText) + "</b><br>"
                + escapeHtml(description) + "<br>"
                + "주제: " + escapeHtml(topic) + "<br>"
                + "난이도: " + escapeHtml(difficulty) + "<br>"
                + "상태: " + status + "<br>"
                + "클리어: " + (card.isCleared() ? "완료" : "미완료")
                + "<br>최고 기록: " + record
                + "</center></html>",
                SwingConstants.CENTER
            );

            cardPanel.add(infoLabel, BorderLayout.CENTER);

            // 입장 버튼
            JButton enterButton = new JButton(
                card.isPlayable() ? "입장하기" : "준비 중"
            );

            enterButton.setEnabled(card.isPlayable());

            enterButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(
                    this,
                    titleText + " 맵을 선택했습니다."
                );
            });

            cardPanel.add(enterButton, BorderLayout.SOUTH);

            posterPanel.add(cardPanel);
        }

        add(posterPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    // 맵 목록이 없을 때 안내 메시지
    private void showEmptyMessage(String message) {

        JLabel emptyLabel = new JLabel(
            message,
            SwingConstants.CENTER
        );

        emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 18));

        add(emptyLabel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    // null 또는 빈 문자열 처리
    private String safeText(String value, String defaultValue) {

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    // HTML 태그로 잘못 해석되지 않도록 문자 변환
    private String escapeHtml(String value) {

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
