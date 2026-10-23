package giriatk.view.dialog;

import giriatk.dao.BarangDao;
import giriatk.dao.KategoriDao;
import giriatk.model.Barang;
import giriatk.model.Kategori;
import giriatk.util.Fmt;
import giriatk.util.ImageUtil;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class BarangDialog extends FormDialog {

    private static final String TANPA_KATEGORI = "— Tanpa Kategori —";

    private final BarangDao dao = new BarangDao();
    private final KategoriDao kategoriDao = new KategoriDao();
    private final Barang existing;

    private final JTextField txtKode = UiKit.textField("Kode barang");
    private final JTextField txtNama = UiKit.textField("Contoh: Pulpen");
    private final JTextField txtMerk = UiKit.textField("Contoh: Pilot, Standard, Faber-Castell");
    private final JTextField txtJenis = UiKit.textField("Contoh: G-2, AE7, Gel Pen");
    private final JTextField txtWarna = UiKit.textField("Contoh: Hitam, Biru, Merah");
    private final JTextField txtUkuran = UiKit.textField("Contoh: 0.5mm, A4, F4");
    private final JComboBox<Object> cmbKategori = new JComboBox<>();
    private final JComboBox<String> cmbSatuan = new JComboBox<>(new String[]{
        "pcs", "rim", "box", "lusin", "pack", "buah", "buku", "set"
    });
    private final JTextField txtHargaBeli = UiKit.textField("Harga beli");
    private final JTextField txtHargaJual = UiKit.textField("Harga jual");
    private final JTextField txtStok = UiKit.textField("0");
    private final JTextField txtStokMin = UiKit.textField("0");
    private final JCheckBox chkAktif = UiKit.checkBox("Barang aktif dan bisa dijual", true);

    private final JLabel lblPreview = new JLabel();
    private File selectedImageFile;
    private String currentGambar;

    public BarangDialog(Window owner, Barang existing) {
        super(owner, existing == null ? "Tambah Barang" : "Ubah Barang",
                "Lengkapi data barang untuk penjualan dan pemantauan stok.");
        this.existing = existing;

        cmbSatuan.setEditable(true);
        styleCombo(cmbSatuan);
        styleCombo(cmbKategori);

        cmbKategori.addItem(TANPA_KATEGORI);
        try {
            List<Kategori> kategoriList = kategoriDao.findAll();
            for (Kategori k : kategoriList) {
                cmbKategori.addItem(k);
            }
        } catch (SQLException ex) {
            error("Gagal memuat kategori: " + ex.getMessage());
        }

        if (existing == null) {
            try {
                txtKode.setText(dao.kodeOtomatis());
            } catch (SQLException ex) {
                txtKode.setText("BRG0001");
            }
            txtStok.setText("0");
            txtStokMin.setText("0");
        } else {
            txtKode.setText(existing.getKode());
            txtNama.setText(existing.getNama());
            txtMerk.setText(existing.getMerk() != null ? existing.getMerk() : "");
            txtJenis.setText(existing.getJenis() != null ? existing.getJenis() : "");
            txtWarna.setText(existing.getWarna() != null ? existing.getWarna() : "");
            txtUkuran.setText(existing.getUkuran() != null ? existing.getUkuran() : "");
            cmbSatuan.setSelectedItem(existing.getSatuan());
            txtHargaBeli.setText(Fmt.angka(existing.getHargaBeli()));
            txtHargaJual.setText(Fmt.angka(existing.getHargaJual()));
            txtStok.setText(String.valueOf(existing.getStok()));
            txtStokMin.setText(String.valueOf(existing.getStokMinimum()));
            chkAktif.setSelected(existing.isAktif());
            pilihKategori(existing.getKategoriId());
            currentGambar = existing.getGambar();
        }

        addField("Kode barang", fieldWidth(txtKode, 340));
        addField("Nama barang", fieldWidth(txtNama, 340));
        addField("Merk", fieldWidth(txtMerk, 340));
        addField("Jenis / Tipe", fieldWidth(txtJenis, 340));
        addField("Warna", fieldWidth(txtWarna, 340));
        addField("Ukuran", fieldWidth(txtUkuran, 340));
        addField("Kategori", fieldWidth(cmbKategori, 340));
        addField("Satuan", fieldWidth(cmbSatuan, 340));
        addField("Harga beli", fieldWidth(txtHargaBeli, 340));
        addField("Harga jual", fieldWidth(txtHargaJual, 340));
        addField("Stok saat ini", fieldWidth(txtStok, 340));
        addField("Stok minimum", fieldWidth(txtStokMin, 340));
        addField("Gambar produk", buildGambarPanel());
        addFullWidth(chkAktif);

        batasiTinggi();
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildGambarPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(340, 80));

        lblPreview.setPreferredSize(new Dimension(64, 64));
        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblPreview.setVerticalAlignment(SwingConstants.CENTER);
        lblPreview.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        updatePreview();
        panel.add(lblPreview);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new javax.swing.BoxLayout(buttons, javax.swing.BoxLayout.Y_AXIS));

        UiKit.SoftButton btnPilih = new UiKit.SoftButton("Pilih Gambar", UiKit.Variant.GHOST);
        btnPilih.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPilih.addActionListener(e -> pilihGambar());
        buttons.add(btnPilih);
        buttons.add(javax.swing.Box.createVerticalStrut(4));

        UiKit.SoftButton btnHapus = new UiKit.SoftButton("Hapus Gambar", UiKit.Variant.GHOST);
        btnHapus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHapus.addActionListener(e -> {
            selectedImageFile = null;
            currentGambar = null;
            updatePreview();
        });
        buttons.add(btnHapus);

        panel.add(buttons);
        return panel;
    }

    private void pilihGambar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Gambar (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fc.getSelectedFile();
            updatePreview();
        }
    }

    private void updatePreview() {
        if (selectedImageFile != null && selectedImageFile.exists()) {
            lblPreview.setIcon(ImageUtil.getThumbnail(selectedImageFile, 60, 60));
        } else if (currentGambar != null && !currentGambar.isBlank()) {
            lblPreview.setIcon(ImageUtil.getThumbnail(currentGambar, 60, 60));
        } else {
            lblPreview.setIcon(ImageUtil.createPlaceholder(60, 60));
        }
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(Theme.text(Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(0, 40));
    }

    private void pilihKategori(Integer kategoriId) {
        if (kategoriId == null) {
            cmbKategori.setSelectedItem(TANPA_KATEGORI);
            return;
        }
        for (int i = 0; i < cmbKategori.getItemCount(); i++) {
            Object item = cmbKategori.getItemAt(i);
            if (item instanceof Kategori k && k.getId() == kategoriId) {
                cmbKategori.setSelectedItem(item);
                return;
            }
        }
    }

    @Override
    protected void simpan() {
        String kode = txtKode.getText().trim();
        String nama = txtNama.getText().trim();
        if (kode.isEmpty()) {
            error("Kode barang wajib diisi.");
            return;
        }
        if (nama.isEmpty()) {
            error("Nama barang wajib diisi.");
            return;
        }
        BigDecimal hargaBeli = Fmt.parseAngka(txtHargaBeli.getText());
        BigDecimal hargaJual = Fmt.parseAngka(txtHargaJual.getText());
        if (hargaBeli == null) {
            error("Harga beli wajib diisi dengan angka.");
            return;
        }
        if (hargaJual == null) {
            error("Harga jual wajib diisi dengan angka.");
            return;
        }
        Integer stok = Fmt.parseBulat(txtStok.getText());
        Integer stokMin = Fmt.parseBulat(txtStokMin.getText());
        if (stok == null) {
            error("Stok wajib diisi dengan angka.");
            return;
        }
        if (stokMin == null) {
            error("Stok minimum wajib diisi dengan angka.");
            return;
        }
        Object satuanItem = cmbSatuan.getEditor().getItem();
        String satuan = satuanItem == null ? "" : satuanItem.toString().trim();
        if (satuan.isEmpty()) {
            error("Satuan wajib diisi.");
            return;
        }

        try {
            int id = existing == null ? 0 : existing.getId();
            if (dao.kodeTaken(kode, id)) {
                error("Kode barang sudah dipakai.");
                return;
            }
            Barang barang = existing == null ? new Barang() : existing;
            barang.setKode(kode);
            barang.setNama(nama);
            barang.setMerk(emptyToNull(txtMerk.getText()));
            barang.setJenis(emptyToNull(txtJenis.getText()));
            barang.setWarna(emptyToNull(txtWarna.getText()));
            barang.setUkuran(emptyToNull(txtUkuran.getText()));
            Object kategoriItem = cmbKategori.getSelectedItem();
            barang.setKategoriId(kategoriItem instanceof Kategori k ? k.getId() : null);
            barang.setSatuan(satuan);
            barang.setHargaBeli(hargaBeli);
            barang.setHargaJual(hargaJual);
            barang.setStok(stok);
            barang.setStokMinimum(stokMin);
            barang.setAktif(chkAktif.isSelected());

            if (selectedImageFile != null) {
                String fileName = ImageUtil.simpanGambar(selectedImageFile, kode);
                barang.setGambar(fileName);
            } else {
                barang.setGambar(currentGambar);
            }

            if (existing == null) {
                dao.insert(barang);
            } else {
                dao.update(barang);
            }
            sukses();
        } catch (SQLException ex) {
            error("Gagal menyimpan barang: " + ex.getMessage());
        }
    }

    private String emptyToNull(String text) {
        if (text == null) return null;
        text = text.trim();
        return text.isEmpty() ? null : text;
    }
}
