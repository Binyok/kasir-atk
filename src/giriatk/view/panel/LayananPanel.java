package giriatk.view.panel;

import giriatk.dao.LayananDao;
import giriatk.model.Layanan;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Icons;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.AksiIkon;
import giriatk.view.dialog.LayananDialog;

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

public class LayananPanel extends BasePanel {

    private final LayananDao dao = new LayananDao();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Nama Layanan Jasa", "Satuan Hitung", "Tarif Satuan", "Keterangan / Spesifikasi", "Status Operasional", "", ""}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final UiKit.AppTable table = new UiKit.AppTable(model);
    private final List<Layanan> data = new ArrayList<>();

    public LayananPanel() {
        super(new BorderLayout());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(button("Tambah Layanan", UiKit.Variant.PRIMARY, this::tambah));
        UiKit.IconButton refresh = new UiKit.IconButton(Icons.Name.REFRESH, Theme.MUTED, 16, true);
        refresh.setToolTipText("Muat ulang");
        refresh.addActionListener(e -> reload());
        actions.add(refresh);

        table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT, true));
        table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
        table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT, true));
        table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(4).setCellRenderer(new UiKit.BadgeRenderer());
        int[] lebar = {180, 80, 110, 260, 100};
        for (int i = 0; i < lebar.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }
        AksiIkon.pasang(table, 5, Icons.Name.EDIT, Theme.MUTED);
        AksiIkon.pasang(table, 6, Icons.Name.TRASH, Theme.DANGER);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                if (col == 5 && e.getClickCount() == 1) {
                    ubah();
                } else if (col == 6 && e.getClickCount() == 1) {
                    hapus();
                } else if (e.getClickCount() == 2 && col != 5 && col != 6) {
                    ubah();
                }
            }
        });

        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.add(actions, BorderLayout.NORTH);

        table.setEmptyState("Belum ada layanan jasa", "Tambahkan layanan seperti Print HVS, Fotokopi, Jilid, atau Laminating.");
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);

        add(pageHeader("Katalog Layanan Jasa Cetak", "Kelola daftar jenis jasa print, fotokopi, jilid, beserta tarif dan satuannya."), BorderLayout.NORTH);
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
            for (Layanan l : data) {
                model.addRow(new Object[]{
                    l.getNama(),
                    l.getSatuan(),
                    Fmt.rupiah(l.getHargaSatuan()),
                    l.getKeterangan() == null ? "-" : l.getKeterangan(),
                    l.isAktif() ? "AKTIF" : "NONAKTIF",
                    "",
                    ""
                });
            }
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal memuat layanan: " + ex.getMessage());
        }
    }

    private Layanan terpilih() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Dialogs.peringatan(this, "Pilih satu layanan terlebih dahulu.");
            return null;
        }
        return data.get(table.convertRowIndexToModel(row));
    }

    private void tambah() {
        LayananDialog dialog = new LayananDialog(owner(), null);
        dialog.setVisible(true);
        if (dialog.isTersimpan()) {
            reload();
        }
    }

    private void ubah() {
        Layanan layanan = terpilih();
        if (layanan == null) {
            return;
        }
        LayananDialog dialog = new LayananDialog(owner(), layanan);
        dialog.setVisible(true);
        if (dialog.isTersimpan()) {
            reload();
        }
    }

    private void hapus() {
        Layanan layanan = terpilih();
        if (layanan == null) {
            return;
        }
        if (!Dialogs.konfirmasi(this, "Hapus layanan \"" + layanan.getNama() + "\"?")) {
            return;
        }
        try {
            dao.delete(layanan.getId());
            reload();
        } catch (Exception ex) {
            Dialogs.error(this, "Layanan tidak bisa dihapus karena masih tercatat pada riwayat transaksi.\n"
                    + "Ubah status menjadi nonaktif saja.\n\nDetail: " + ex.getMessage());
        }
    }
}
