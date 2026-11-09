package giriatk.view.panel;

import giriatk.config.AppConfig;
import giriatk.dao.PengaturanDao;
import giriatk.service.BackupService;
import giriatk.service.ThermalPrintService;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class PengaturanPanel extends BasePanel {

    private final PengaturanDao dao = new PengaturanDao();
    private final BackupService backupService = new BackupService();
    private final ThermalPrintService printService = new ThermalPrintService();

    private final JTextField txtNama = UiKit.textField("Nama toko");
    private final JTextField txtTelepon = UiKit.textField("Nomor telepon");
    private final JTextField txtAlamat = UiKit.textField("Alamat toko");
    private final JTextField txtFooter = UiKit.textField("Catatan kaki struk");

    private final JComboBox<String> cmbPrinter = new JComboBox<>();
    private final JComboBox<String> cmbKertas = new JComboBox<>(new String[]{"58mm", "80mm"});
    private final JCheckBox chkAutoPrint = UiKit.checkBox("Cetak struk otomatis saat pembayaran selesai", true);

    private final JCheckBox chkMidtransEnable = UiKit.checkBox("Aktifkan Pembayaran Gateway Midtrans (QRIS / GoPay / Snap)", true);
    private final JTextField txtMidtransServerKey = UiKit.textField("Server Key Midtrans");
    private final JTextField txtMidtransClientKey = UiKit.textField("Client Key Midtrans");
    private final JComboBox<String> cmbMidtransEnv = new JComboBox<>(new String[]{"Sandbox (Uji Coba)", "Production (Live)"});

    public PengaturanPanel() {
        super(new BorderLayout());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildProfilCard());
        body.add(Box.createVerticalStrut(16));
        body.add(buildPrinterCard());
        body.add(Box.createVerticalStrut(16));
        body.add(buildMidtransCard());
        body.add(Box.createVerticalStrut(16));
        body.add(buildBackupCard());
        body.add(Box.createVerticalStrut(16));
        body.add(buildInfoCard());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        add(pageHeader("Pengaturan Sistem & Database", "Profil toko, pencadangan basis data berkala, dan informasi server koneksi."), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        try {
            Map<String, String> data = dao.semua();
            String nama = data.getOrDefault("nama_toko", "GiriATK & Print");
            String telp = data.getOrDefault("telepon", "0812-3456-7890");
            if ("0812-0000-0000".equals(telp)) telp = "0812-3456-7890";
            String alamat = data.getOrDefault("alamat", "Jl. Raya Giri No. 88, Banyuwangi");
            if (alamat.isBlank() || alamat.equalsIgnoreCase("Jl. Contoh No. 1")) alamat = "Jl. Raya Giri No. 88, Banyuwangi";
            txtNama.setText(nama);
            txtTelepon.setText(telp);
            txtAlamat.setText(alamat);
            txtFooter.setText(data.getOrDefault("footer_struk", "Terima kasih sudah berbelanja"));

            cmbPrinter.removeAllItems();
            cmbPrinter.addItem("— Default System Printer —");
            for (String p : printService.getDaftarPrinter()) {
                cmbPrinter.addItem(p);
            }
            String savedPrinter = data.getOrDefault("printer_nama", "");
            if (!savedPrinter.isBlank()) {
                cmbPrinter.setSelectedItem(savedPrinter);
            } else {
                cmbPrinter.setSelectedIndex(0);
            }

            cmbKertas.setSelectedItem(data.getOrDefault("printer_lebar", "58mm"));
            chkAutoPrint.setSelected("1".equals(data.getOrDefault("printer_auto", "1")));

            chkMidtransEnable.setSelected("1".equals(data.getOrDefault("midtrans_enabled", "1")));
            txtMidtransServerKey.setText(data.getOrDefault("midtrans_server_key", "SB-Mid-server-YOUR_SERVER_KEY"));
            txtMidtransClientKey.setText(data.getOrDefault("midtrans_client_key", "SB-Mid-client-YOUR_CLIENT_KEY"));
            cmbMidtransEnv.setSelectedIndex("1".equals(data.getOrDefault("midtrans_is_production", "0")) ? 1 : 0);
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal memuat pengaturan: " + ex.getMessage());
        }
    }

    private JPanel buildProfilCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Profil Toko & Header Struk");
        title.setFont(Theme.display(Font.BOLD, 15));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 1, 0, 12));
        form.setOpaque(false);
        form.add(field("Nama Toko / Usaha", txtNama));
        form.add(field("Nomor Kontak / Telepon", txtTelepon));
        form.add(field("Alamat Lengkap Toko", txtAlamat));
        form.add(field("Catatan Kaki (Footer Struk)", txtFooter));
        card.add(form, BorderLayout.CENTER);

        UiKit.SoftButton simpan = new UiKit.SoftButton("SIMPAN PROFIL TOKO", UiKit.Variant.PRIMARY);
        simpan.addActionListener(e -> simpanProfil());
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(simpan);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel field(String label, JComponent field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.text(Font.BOLD, 11));
        lbl.setForeground(Theme.INK_SOFT);
        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildPrinterCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Perangkat Keras: Printer Thermal Struk Kasir");
        title.setFont(Theme.display(Font.BOLD, 15));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 1, 0, 12));
        form.setOpaque(false);

        JPanel pnlPrinter = new JPanel(new BorderLayout(0, 4));
        pnlPrinter.setOpaque(false);
        JLabel lblPrn = new JLabel("Pilih Printer Thermal");
        lblPrn.setFont(Theme.text(Font.BOLD, 11));
        lblPrn.setForeground(Theme.INK_SOFT);
        pnlPrinter.add(lblPrn, BorderLayout.NORTH);
        cmbPrinter.setFont(Theme.text(Font.PLAIN, 13));
        pnlPrinter.add(cmbPrinter, BorderLayout.CENTER);
        form.add(pnlPrinter);

        JPanel pnlKertas = new JPanel(new BorderLayout(0, 4));
        pnlKertas.setOpaque(false);
        JLabel lblKts = new JLabel("Ukuran Kertas Thermal");
        lblKts.setFont(Theme.text(Font.BOLD, 11));
        lblKts.setForeground(Theme.INK_SOFT);
        pnlKertas.add(lblKts, BorderLayout.NORTH);
        cmbKertas.setFont(Theme.text(Font.PLAIN, 13));
        pnlKertas.add(cmbKertas, BorderLayout.CENTER);
        form.add(pnlKertas);

        form.add(chkAutoPrint);
        card.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        UiKit.SoftButton btnTest = new UiKit.SoftButton("Uji Coba Cetak (Test Print)", UiKit.Variant.GHOST);
        btnTest.addActionListener(e -> {
            try {
                printService.cetakUjiCoba();
                Dialogs.info(this, "Perintah uji coba cetak berhasil dikirim ke printer.");
            } catch (Exception ex) {
                Dialogs.error(this, "Gagal mencetak struk uji coba: " + ex.getMessage());
            }
        });
        actions.add(btnTest);

        UiKit.SoftButton btnSimpan = new UiKit.SoftButton("SIMPAN PENGATURAN PRINTER", UiKit.Variant.PRIMARY);
        btnSimpan.addActionListener(e -> simpanPrinter());
        actions.add(btnSimpan);

        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void simpanPrinter() {
        try {
            String selected = (String) cmbPrinter.getSelectedItem();
            if (selected == null || selected.startsWith("—")) {
                selected = "";
            }
            dao.simpan("printer_nama", selected);
            dao.simpan("printer_lebar", (String) cmbKertas.getSelectedItem());
            dao.simpan("printer_auto", chkAutoPrint.isSelected() ? "1" : "0");
            Dialogs.info(this, "Pengaturan printer thermal berhasil disimpan.");
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menyimpan pengaturan printer: " + ex.getMessage());
        }
    }

    private JPanel buildMidtransCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Midtrans Payment Gateway (QRIS / GoPay / Snap)");
        title.setFont(Theme.display(Font.BOLD, 15));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);
        top.add(chkMidtransEnable, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 1, 0, 12));
        form.setOpaque(false);
        form.add(field("Server Key", txtMidtransServerKey));
        form.add(field("Client Key", txtMidtransClientKey));
        form.add(field("Environment", cmbMidtransEnv));
        card.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        UiKit.SoftButton testBtn = new UiKit.SoftButton("Tes Koneksi", UiKit.Variant.GHOST);
        testBtn.addActionListener(e -> {
            simpanMidtrans();
            try {
                new giriatk.service.MidtransService().testConnection();
                Dialogs.info(this, "Koneksi ke Midtrans berhasil!\nServer Key valid.");
            } catch (Exception ex) {
                Dialogs.error(this, "Koneksi Midtrans gagal: " + ex.getMessage());
            }
        });
        actions.add(testBtn);

        UiKit.SoftButton simpan = new UiKit.SoftButton("SIMPAN KONFIGURASI", UiKit.Variant.PRIMARY);
        simpan.addActionListener(e -> simpanMidtrans());
        actions.add(simpan);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void simpanMidtrans() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("midtrans_enabled", chkMidtransEnable.isSelected() ? "1" : "0");
        data.put("midtrans_server_key", txtMidtransServerKey.getText().trim());
        data.put("midtrans_client_key", txtMidtransClientKey.getText().trim());
        data.put("midtrans_is_production", cmbMidtransEnv.getSelectedIndex() == 1 ? "1" : "0");
        try {
            dao.simpanSemua(data);
            Dialogs.info(this, "Konfigurasi Midtrans berhasil disimpan.");
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menyimpan konfigurasi Midtrans: " + ex.getMessage());
        }
    }

    private JPanel buildBackupCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Pencadangan Database (Backup SQL)");
        title.setFont(Theme.display(Font.BOLD, 15));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);

        JLabel info = new JLabel("<html>Ekspor seluruh skema dan record tabel barang, jasa cetak, transaksi, antrian, dan akun ke dalam satu berkas berkode <b>.sql</b> mandiri.<br>"
                + "File ini dapat dipulihkan kapan saja melalui phpMyAdmin atau MySQL CLI.</html>");
        info.setFont(Theme.text(Font.PLAIN, 12));
        info.setForeground(Theme.MUTED);
        card.add(info, BorderLayout.CENTER);

        UiKit.SoftButton backup = new UiKit.SoftButton("BACKUP DATABASE SEKARANG", UiKit.Variant.INK);
        backup.addActionListener(e -> jalankanBackup());
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(backup);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildInfoCard() {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Telemetri Database Host");
        title.setFont(Theme.display(Font.BOLD, 15));
        title.setForeground(Theme.INK);
        top.add(title, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 2, 12, 8));
        grid.setOpaque(false);
        grid.add(infoLabel("Host Server & Port"));
        grid.add(valueLabel(AppConfig.get("db.host") + ":" + AppConfig.get("db.port")));
        grid.add(infoLabel("Nama Skema Database"));
        grid.add(valueLabel(AppConfig.get("db.name")));
        grid.add(infoLabel("User Database"));
        grid.add(valueLabel(AppConfig.get("db.user")));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JLabel infoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.mono(Font.BOLD, 11));
        label.setForeground(Theme.FAINT);
        return label;
    }

    private JLabel valueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.mono(Font.BOLD, 12));
        label.setForeground(Theme.ACCENT);
        return label;
    }

    private void simpanProfil() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("nama_toko", txtNama.getText().trim());
        data.put("telepon", txtTelepon.getText().trim());
        data.put("alamat", txtAlamat.getText().trim());
        data.put("footer_struk", txtFooter.getText().trim());
        try {
            dao.simpanSemua(data);
            Dialogs.info(this, "Profil toko berhasil diperbarui.");
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal menyimpan profil: " + ex.getMessage());
        }
    }

    private void jalankanBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Lokasi Penyimpanan Backup Database");
        chooser.setSelectedFile(new java.io.File("backup-copygiri-" + Fmt.capWaktu() + ".sql"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            backupService.backup(target);
            Dialogs.info(this, "Pencadangan berhasil disimpan ke:\n" + target.toAbsolutePath());
        } catch (Exception ex) {
            Dialogs.error(this, "Gagal membuat berkas backup: " + ex.getMessage());
        }
    }
}
