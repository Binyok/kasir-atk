package giriatk;

import giriatk.config.Db;
import giriatk.util.Theme;
import giriatk.view.LoginFrame;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.sql.SQLException;

public class Giriatk {

    public static void main(String[] args) {
        configureLookAndFeel();
        if (!prepareDatabase()) {
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabHeight", 40);
            UIManager.put("defaultFont", Theme.text(Font.PLAIN, 13));
        } catch (Exception ex) {
            System.err.println("Gagal memuat tema: " + ex.getMessage());
        }
    }

    private static boolean prepareDatabase() {
        try {
            Db.bootstrap();
            new giriatk.service.DatabaseSeeder().seed();
            return true;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null,
                    "Tidak bisa terhubung ke database MySQL.\n\n"
                    + "Periksa file config/db.properties.\n\n"
                    + "Detail: " + ex.getMessage(),
                    "Koneksi Database Gagal", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
