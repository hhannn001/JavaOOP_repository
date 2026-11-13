
package coderoom.pages.mapselect;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.net.URL;
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

        // 포스터를 가로로 배치
        JPanel posterPanel = new JPanel(new GridLayout(1, 3, 20, 10));
        posterPanel.setBackground(Color.WHITE);
        posterPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 30, 30, 30)
        );

        for (MapCard card : cards) {

            JPanel cardPanel = new JPanel(new BorderLayout(5, 10));
            cardPanel.setBackground(Color.WHITE);
            cardPanel.setBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1)
            );

            // 포스터 이미지
            JLabel posterLabel = new JLabel(
                "이미지 준비 중",
                SwingConstants.CENTER
            );

            posterLabel.setPreferredSize(new Dimension(200, 250));

            String posterPath = card.getPosterPath();

            if (posterPath != null) {

                String resourcePath = posterPath.startsWith("/")
                    ? posterPath
                    : "/" + posterPath;

                URL imageURL = getClass().getResource(resourcePath);

                if (imageURL != null) {

                    ImageIcon originalIcon = new ImageIcon(imageURL);

                    Image image = originalIcon.getImage()
                        .getScaledInstance(
                            200, 250, Image.SCALE_SMOOTH
                        );

                    posterLabel.setText("");
                    posterLabel.setIcon(new ImageIcon(image));
                }
            }

            cardPanel.add(posterLabel, BorderLayout.NORTH);

            // 맵 정보
            String status = card.isPlayable()
                ? "입장 가능"
                : "준비 중";

            String record = card.getBestSeconds() == null
                ? "기록 없음"
                : card.getBestSeconds() + "초";

            JLabel infoLabel = new JLabel(
                "<html><center>"
                + "<b>" + card.getTitle() + "</b><br>"
                + card.getDescription() + "<br>"
                + "주제: " + card.getTopic() + "<br>"
                + "난이도: " + card.getDifficulty() + "<br>"
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
                    card.getTitle() + " 맵을 선택했습니다."
                );
            });

            cardPanel.add(enterButton, BorderLayout.SOUTH);

            posterPanel.add(cardPanel);
        }

        add(posterPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }
}
