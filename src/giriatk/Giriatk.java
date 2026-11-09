package giriatk;

import giriatk.config.Db;
import giriatk.util.Theme;
import giriatk.view.LoginFrame;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
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

    public static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("Button.arc", 0);
            UIManager.put("Component.arc", 0);
            UIManager.put("TextComponent.arc", 0);
            UIManager.put("ProgressBar.arc", 0);
            UIManager.put("ScrollBar.thumbArc", 0);
            UIManager.put("ScrollBar.trackArc", 0);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabHeight", 40);
            UIManager.put("Component.accentColor", Theme.ACCENT);
            UIManager.put("Component.focusColor", Theme.ACCENT);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Component.borderColor", Theme.BORDER_STRONG);
            UIManager.put("Component.disabledBorderColor", Theme.BORDER);
            UIManager.put("Component.focusedBorderColor", Theme.ACCENT);
            UIManager.put("Panel.background", Theme.CANVAS);
            UIManager.put("TextField.background", Theme.SURFACE);
            UIManager.put("PasswordField.background", Theme.SURFACE);
            UIManager.put("TextArea.background", Theme.SURFACE);
            UIManager.put("ComboBox.background", Theme.SURFACE);
            UIManager.put("ComboBox.buttonBackground", Theme.SURFACE);
            UIManager.put("ComboBox.buttonSeparatorColor", Theme.BORDER_STRONG);
            UIManager.put("Table.background", Theme.SURFACE);
            UIManager.put("Table.selectionBackground", Theme.SURFACE_ELEVATED);
            UIManager.put("Table.selectionForeground", Theme.INK);
            UIManager.put("TableHeader.background", Theme.SURFACE_ELEVATED);
            UIManager.put("CheckBox.icon.arc", 0);
            UIManager.put("CheckBox.icon.background", Theme.SURFACE);
            UIManager.put("CheckBox.icon.borderColor", Theme.BORDER_STRONG);
            UIManager.put("CheckBox.icon.selectedBackground", Theme.ACCENT);
            UIManager.put("CheckBox.icon.selectedBorderColor", Theme.ACCENT);
            UIManager.put("CheckBox.icon.checkmarkColor", Theme.SURFACE);
            UIManager.put("CheckBox.icon.hoverBackground", Theme.SURFACE_ELEVATED);
            UIManager.put("CheckBox.icon.hoverSelectedBackground", Theme.ACCENT_HOVER);
            UIManager.put("RadioButton.icon.background", Theme.SURFACE);
            UIManager.put("RadioButton.icon.selectedBackground", Theme.ACCENT);
            UIManager.put("RadioButton.icon.checkmarkColor", Theme.SURFACE);
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
