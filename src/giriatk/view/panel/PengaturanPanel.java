package giriatk.view.panel;

import giriatk.config.AppConfig;
import giriatk.dao.PengaturanDao;
import giriatk.service.BackupService;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

    private final JTextField txtNama = UiKit.textField("Nama toko");
    private final JTextField txtTelepon = UiKit.textField("Nomor telepon");
    private final JTextField txtAlamat = UiKit.textField("Alamat toko");
    private final JTextField txtFooter = UiKit.textField("Catatan kaki struk");

    public PengaturanPanel() {
        super(new BorderLayout());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(buildProfilCard());
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
            txtNama.setText(data.getOrDefault("nama_toko", ""));
            txtTelepon.setText(data.getOrDefault("telepon", ""));
            txtAlamat.setText(data.getOrDefault("alamat", ""));
            txtFooter.setText(data.getOrDefault("footer_struk", ""));
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

    private JPanel field(String label, JTextField field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.text(Font.BOLD, 11));
        lbl.setForeground(Theme.INK_SOFT);
        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
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
