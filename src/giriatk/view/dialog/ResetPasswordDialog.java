package giriatk.view.dialog;

import giriatk.service.UserService;
import giriatk.util.UiKit;

import javax.swing.JPasswordField;
import java.awt.Window;
import java.sql.SQLException;

public class ResetPasswordDialog extends FormDialog {

    private final UserService service = new UserService();
    private final int userId;

    private final JPasswordField txtBaru = UiKit.passwordField("Minimal 4 karakter");
    private final JPasswordField txtUlangi = UiKit.passwordField("Ulangi password baru");

    public ResetPasswordDialog(Window owner, int userId, String namaPengguna) {
        super(owner, "Reset Password", "Atur ulang password untuk " + namaPengguna + ".");
        this.userId = userId;
        addField("Password baru", fieldWidth(txtBaru, 320));
        addField("Ulangi password", fieldWidth(txtUlangi, 320));
        pack();
        setLocationRelativeTo(owner);
    }

    @Override
    protected void simpan() {
        String baru = new String(txtBaru.getPassword());
        String ulangi = new String(txtUlangi.getPassword());
        if (baru.length() < 4) {
            error("Password minimal 4 karakter.");
            return;
        }
        if (!baru.equals(ulangi)) {
            error("Ulangi password tidak sama.");
            return;
        }
        try {
            service.resetPassword(userId, baru);
            sukses();
        } catch (SQLException ex) {
            error("Gagal reset password: " + ex.getMessage());
        }
    }
}
