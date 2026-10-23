package giriatk.util;

import javax.swing.JOptionPane;
import java.awt.Component;

public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Informasi", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void peringatan(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Peringatan", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Kesalahan", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean konfirmasi(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
