package giriatk.view.dialog;

import giriatk.dao.AntrianDao;
import giriatk.dao.LayananDao;
import giriatk.model.Antrian;
import giriatk.model.Layanan;
import giriatk.util.Fmt;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;
import java.util.List;

public class AntrianDialog extends FormDialog {

    private final AntrianDao dao = new AntrianDao();
    private final Antrian existing;

    private final JComboBox<Layanan> cmbLayanan = new JComboBox<>();
    private final JTextField txtPelanggan = UiKit.textField("Contoh: Budi");
    private final JTextField txtDeskripsi = UiKit.textField("Contoh: print 10 lembar warna");
    private final JTextField txtJumlah = UiKit.textField("1");

    public AntrianDialog(Window owner, Antrian existing) {
        super(owner, existing == null ? "Tambah Antrian" : "Ubah Antrian",
                "Catat pesanan cetak atau fotokopi pelanggan.");
        this.existing = existing;

        cmbLayanan.setFont(Theme.text(Font.PLAIN, 13));
        cmbLayanan.setPreferredSize(new Dimension(0, 40));
        try {
            List<Layanan> layanan = new LayananDao().findAllAktif();
            for (Layanan l : layanan) {
                cmbLayanan.addItem(l);
            }
        } catch (SQLException ex) {
            error("Gagal memuat layanan: " + ex.getMessage());
        }

        if (existing != null) {
            txtPelanggan.setText(existing.getNamaPelanggan());
            txtDeskripsi.setText(existing.getDeskripsi());
            txtJumlah.setText(String.valueOf(existing.getJumlah()));
            pilihLayanan(existing.getLayananId());
            cmbLayanan.setEnabled(false);
            txtJumlah.setEnabled(false);
        }

        addField("Layanan", fieldWidth(cmbLayanan, 340));
        addField("Nama pelanggan", fieldWidth(txtPelanggan, 340));
        addField("Deskripsi", fieldWidth(txtDeskripsi, 340));
        addField("Jumlah", fieldWidth(txtJumlah, 340));

        pack();
        setLocationRelativeTo(owner);
    }

    private void pilihLayanan(Integer layananId) {
        if (layananId == null) {
            return;
        }
        for (int i = 0; i < cmbLayanan.getItemCount(); i++) {
            Layanan l = cmbLayanan.getItemAt(i);
            if (l.getId() == layananId) {
                cmbLayanan.setSelectedItem(l);
                return;
            }
        }
    }

    @Override
    protected void simpan() {
        Layanan layanan = (Layanan) cmbLayanan.getSelectedItem();
        if (layanan == null) {
            error("Pilih layanan terlebih dahulu.");
            return;
        }
        Integer jumlah = Fmt.parseBulat(txtJumlah.getText());
        if (jumlah == null || jumlah < 1) {
            error("Jumlah wajib diisi minimal 1.");
            return;
        }
        try {
            Antrian antrian = existing == null ? new Antrian() : existing;
            antrian.setLayananId(layanan.getId());
            antrian.setNamaPelanggan(txtPelanggan.getText().trim().isEmpty() ? null : txtPelanggan.getText().trim());
            antrian.setDeskripsi(txtDeskripsi.getText().trim().isEmpty() ? null : txtDeskripsi.getText().trim());
            antrian.setJumlah(jumlah);
            if (existing == null) {
                antrian.setUserId(Session.user() == null ? null : Session.user().getId());
                dao.insert(antrian);
            } else {
                dao.update(antrian);
            }
            sukses();
        } catch (SQLException ex) {
            error("Gagal menyimpan antrian: " + ex.getMessage());
        }
    }
}
