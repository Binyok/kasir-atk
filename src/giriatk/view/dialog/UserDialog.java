package giriatk.view.dialog;

import giriatk.model.Role;
import giriatk.model.User;
import giriatk.service.UserService;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;

public class UserDialog extends FormDialog {

    private final UserService service = new UserService();
    private final User existing;

    private final JTextField txtNama = UiKit.textField("Nama lengkap");
    private final JTextField txtUsername = UiKit.textField("Username untuk login");
    private final JComboBox<Role> cmbRole = new JComboBox<>(Role.values());
    private final JPasswordField txtPassword = UiKit.passwordField("Minimal 4 karakter");
    private final JCheckBox chkAktif = UiKit.checkBox("Akun aktif dan bisa login", true);

    public UserDialog(Window owner, User existing) {
        super(owner, existing == null ? "Tambah Pengguna" : "Ubah Pengguna",
                "Tentukan peran dan akses akun pengguna.");
        this.existing = existing;

        cmbRole.setFont(Theme.text(Font.PLAIN, 13));
        cmbRole.setPreferredSize(new Dimension(0, 40));
        cmbRole.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Role role) {
                    setText(role == Role.OWNER ? "Owner - akses penuh" : "Kasir - transaksi & stok");
                }
                return this;
            }
        });

        if (existing != null) {
            txtNama.setText(existing.getNamaLengkap());
            txtUsername.setText(existing.getUsername());
            cmbRole.setSelectedItem(existing.getRole());
            chkAktif.setSelected(existing.isAktif());
            txtPassword.putClientProperty("JTextField.placeholderText", "Kosongkan jika tidak diubah");
        }

        addField("Nama lengkap", fieldWidth(txtNama, 340));
        addField("Username", fieldWidth(txtUsername, 340));
        addField("Peran", fieldWidth(cmbRole, 340));
        addField(existing == null ? "Password" : "Password baru", fieldWidth(txtPassword, 340));
        addFullWidth(chkAktif);

        pack();
        setLocationRelativeTo(owner);
    }

    @Override
    protected void simpan() {
        User user = existing == null ? new User() : existing;
        user.setNamaLengkap(txtNama.getText().trim());
        user.setUsername(txtUsername.getText().trim());
        user.setRole((Role) cmbRole.getSelectedItem());
        user.setAktif(chkAktif.isSelected());

        String password = new String(txtPassword.getPassword());
        try {
            service.simpan(user, password);
            sukses();
        } catch (SQLException ex) {
            error(ex.getMessage());
        }
    }
}
