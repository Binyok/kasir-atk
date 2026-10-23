package giriatk.view.panel;

import giriatk.dao.UserDao;
import giriatk.model.User;
import giriatk.service.UserService;
import giriatk.util.Dialogs;
import giriatk.util.Icons;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.AksiIkon;
import giriatk.view.dialog.ResetPasswordDialog;
import giriatk.view.dialog.UserDialog;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class UserPanel extends BasePanel {

    private final UserDao dao = new UserDao();
    private final UserService service = new UserService();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Nama Lengkap", "Username", "Hak Akses / Peran", "Status Akun", "", "", ""}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final UiKit.AppTable table = new UiKit.AppTable(model);
    private final List<User> data = new ArrayList<>();

    public UserPanel() {
        super(new BorderLayout());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(button("Tambah Pengguna", UiKit.Variant.PRIMARY, this::tambah));
        UiKit.IconButton refresh = new UiKit.IconButton(Icons.Name.REFRESH, Theme.MUTED, 16, true);
        refresh.setToolTipText("Muat ulang data");
        refresh.addActionListener(e -> reload());
        actions.add(refresh);

        table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
        table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.BadgeRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.BadgeRenderer());
        int[] lebar = {200, 150, 120, 100};
        for (int i = 0; i < lebar.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }
        AksiIkon.pasang(table, 4, Icons.Name.EDIT, Theme.MUTED);
        AksiIkon.pasang(table, 5, Icons.Name.KEY, Theme.ACCENT);
        AksiIkon.pasang(table, 6, Icons.Name.TRASH, Theme.DANGER);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                if (col == 4 && e.getClickCount() == 1) {
                    ubah();
                } else if (col == 5 && e.getClickCount() == 1) {
                    resetPassword();
                } else if (col == 6 && e.getClickCount() == 1) {
                    hapus();
                } else if (e.getClickCount() == 2 && col != 4 && col != 5 && col != 6) {
                    ubah();
                }
            }
        });

        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.add(actions, BorderLayout.NORTH);

        table.setEmptyState("Belum ada pengguna terdaftar", "Tambahkan akun pengguna untuk staf kasir atau owner.");
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);

        add(pageHeader("Manajemen Pengguna & Otorisasi", "Kelola kredensial akun kasir dan owner, pengaturan hak akses, serta reset password."), BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        reload();
    }

    private UiKit.SoftButton button(String text, UiKit.Variant variant, Runnable action) {
        UiKit.SoftButton b = new UiKit.SoftButton(text, variant);
        b.addActionListener(e -> action.run());
        return b;
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void reload() {
        try {
            data.clear();
            data.addAll(dao.findAll());
            model.setRowCount(0);
            for (User u : data) {
                model.addRow(new Object[]{
                    u.getNamaLengkap(),
                    u.getUsername(),
                    u.isOwner() ? "OWNER" : "KASIR",
                    u.isAktif() ? "AKTIF" : "NONAKTIF",
                    "",
                    "",
                    ""
                });
            }
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal memuat pengguna: " + ex.getMessage());
        }
    }

    private User terpilih() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Dialogs.peringatan(this, "Pilih satu pengguna terlebih dahulu.");
            return null;
        }
        return data.get(table.convertRowIndexToModel(row));
    }

    private void tambah() {
        UserDialog dialog = new UserDialog(owner(), null);
        dialog.setVisible(true);
        if (dialog.isTersimpan()) {
            reload();
        }
    }

    private void ubah() {
        User user = terpilih();
        if (user == null) {
            return;
        }
        UserDialog dialog = new UserDialog(owner(), user);
        dialog.setVisible(true);
        if (dialog.isTersimpan()) {
            reload();
        }
    }

    private void resetPassword() {
        User user = terpilih();
        if (user == null) {
            return;
        }
        ResetPasswordDialog dialog = new ResetPasswordDialog(owner(), user.getId(), user.getNamaLengkap());
        dialog.setVisible(true);
        if (dialog.isTersimpan()) {
            Dialogs.info(this, "Password akun \"" + user.getNamaLengkap() + "\" berhasil direset.");
        }
    }

    private void hapus() {
        User user = terpilih();
        if (user == null) {
            return;
        }
        User current = Session.user();
        if (current != null && current.getId() == user.getId()) {
            Dialogs.peringatan(this, "Akun yang sedang aktif login tidak dapat dihapus.");
            return;
        }
        if (!Dialogs.konfirmasi(this, "Hapus akun pengguna \"" + user.getNamaLengkap() + "\"?")) {
            return;
        }
        try {
            service.hapus(user);
            reload();
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
