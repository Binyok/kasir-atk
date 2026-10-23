package giriatk.view.dialog;

import giriatk.dao.KategoriDao;
import giriatk.model.Kategori;
import giriatk.util.UiKit;

import javax.swing.JTextField;
import java.awt.Window;
import java.sql.SQLException;

public class KategoriDialog extends FormDialog {

    private final KategoriDao dao = new KategoriDao();
    private final Kategori existing;

    private final JTextField txtNama = UiKit.textField("Contoh: Alat Tulis");
    private final JTextField txtKeterangan = UiKit.textField("Opsional");

    public KategoriDialog(Window owner, Kategori existing) {
        super(owner, existing == null ? "Tambah Kategori" : "Ubah Kategori",
                "Kelompokkan barang agar mudah dicari dan dilaporkan.");
        this.existing = existing;

        if (existing != null) {
            txtNama.setText(existing.getNama());
            txtKeterangan.setText(existing.getKeterangan());
        }

        addField("Nama kategori", fieldWidth(txtNama, 340));
        addField("Keterangan", fieldWidth(txtKeterangan, 340));

        pack();
        setLocationRelativeTo(owner);
    }

    @Override
    protected void simpan() {
        String nama = txtNama.getText().trim();
        if (nama.isEmpty()) {
            error("Nama kategori wajib diisi.");
            return;
        }
        try {
            int id = existing == null ? 0 : existing.getId();
            if (dao.namaTaken(nama, id)) {
                error("Nama kategori sudah dipakai.");
                return;
            }
            Kategori kategori = existing == null ? new Kategori() : existing;
            kategori.setNama(nama);
            String keterangan = txtKeterangan.getText().trim();
            kategori.setKeterangan(keterangan.isEmpty() ? null : keterangan);
            if (existing == null) {
                dao.insert(kategori);
            } else {
                dao.update(kategori);
            }
            sukses();
        } catch (SQLException ex) {
            error("Gagal menyimpan kategori: " + ex.getMessage());
        }
    }
}
