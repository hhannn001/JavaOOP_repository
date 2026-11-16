package coderoom.pages.mapselect;

import coderoom.entities.map.JdbcMapRepository;
import coderoom.entities.record.JdbcClearRecordRepository;
import coderoom.features.mapcatalog.MapCatalogService;
import coderoom.shared.db.Database;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 별도 예시 문구 대신 실제 기본 테마 설정으로 미리보기한다. */
public class MapSelectionPreview {
    public static void main(String[] args) {
        Database database = new Database(System.getProperty("coderoom.db", "coderoom.db"));
        database.initialize();
        SwingUtilities.invokeLater(() -> {
            MapSelectionPage page = new MapSelectionPage();
            page.load(new MapCatalogService(new JdbcMapRepository(database), new JdbcClearRecordRepository(database)), 4);
            JFrame frame = new JFrame("테마고르기");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1280, 800);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(page);
            frame.setVisible(true);
        });
    }
}
