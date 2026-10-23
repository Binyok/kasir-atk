package giriatk.view.dialog;

import giriatk.dao.LayananDao;
import giriatk.model.Layanan;
import giriatk.util.Fmt;
import giriatk.util.UiKit;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Window;
import java.math.BigDecimal;
import java.sql.SQLException;

public class LayananDialog extends FormDialog {

    private final LayananDao dao = new LayananDao();
    private final Layanan existing;

    private final JTextField txtNama = UiKit.textField("Contoh: Print Hitam Putih");
    private final JComboBox<String> cmbSatuan = new JComboBox<>(new String[]{
        "lembar", "buku", "halaman", "item", "set", "menit"
    });
    private final JTextField txtHarga = UiKit.textField("Contoh: 200");
    private final JTextField txtKeterangan = UiKit.textField("Opsional");
    private final JCheckBox chkAktif = UiKit.checkBox("Layanan aktif dan bisa dipilih saat transaksi", true);

    public LayananDialog(Window owner, Layanan existing) {
        super(owner, existing == null ? "Tambah Layanan" : "Ubah Layanan",
                "Atur jenis jasa cetak beserta harga satuannya.");
        this.existing = existing;

        cmbSatuan.setEditable(true);
        cmbSatuan.setFont(giriatk.util.Theme.text(java.awt.Font.PLAIN, 13));
        cmbSatuan.setPreferredSize(new Dimension(0, 40));

        if (existing != null) {
            txtNama.setText(existing.getNama());
            cmbSatuan.setSelectedItem(existing.getSatuan());
            txtHarga.setText(Fmt.angka(existing.getHargaSatuan()));
            txtKeterangan.setText(existing.getKeterangan());
            chkAktif.setSelected(existing.isAktif());
        }

        addField("Nama layanan", fieldWidth(txtNama, 340));
        addField("Satuan", fieldWidth(cmbSatuan, 340));
        addField("Harga satuan", fieldWidth(txtHarga, 340));
        addField("Keterangan", fieldWidth(txtKeterangan, 340));
        addFullWidth(chkAktif);

        pack();
        setLocationRelativeTo(owner);
    }

    @Override
    protected void simpan() {
        String nama = txtNama.getText().trim();
        if (nama.isEmpty()) {
            error("Nama layanan wajib diisi.");
            return;
        }
        BigDecimal harga = Fmt.parseAngka(txtHarga.getText());
        if (harga == null) {
            error("Harga satuan wajib diisi dengan angka.");
            return;
        }
        Object satuan = cmbSatuan.getEditor().getItem();
        String satuanText = satuan == null ? "" : satuan.toString().trim();
        if (satuanText.isEmpty()) {
            error("Satuan wajib diisi.");
            return;
        }

        try {
            int id = existing == null ? 0 : existing.getId();
            if (dao.namaTaken(nama, id)) {
                error("Nama layanan sudah dipakai.");
                return;
            }
            Layanan layanan = existing == null ? new Layanan() : existing;
            layanan.setNama(nama);
            layanan.setSatuan(satuanText);
            layanan.setHargaSatuan(harga);
            String keterangan = txtKeterangan.getText().trim();
            layanan.setKeterangan(keterangan.isEmpty() ? null : keterangan);
            layanan.setAktif(chkAktif.isSelected());
            if (existing == null) {
                dao.insert(layanan);
            } else {
                dao.update(layanan);
            }
            sukses();
        } catch (SQLException ex) {
            error("Gagal menyimpan layanan: " + ex.getMessage());
        }
    }
}
