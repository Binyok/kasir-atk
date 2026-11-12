package giriatk.view.dialog;

import giriatk.service.MidtransService;
import giriatk.service.MidtransService.SnapResponse;
import giriatk.service.MidtransService.StatusResponse;
import giriatk.util.Dialogs;
import giriatk.util.Fmt;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.net.URI;

/**
 * Dialog pembayaran Midtrans (Snap).
 *
 * Alur yang benar:
 * 1. Kasir memilih channel (QRIS / VA / e-wallet / semua metode).
 * 2. Aplikasi membuat SATU transaksi Snap ke server Midtrans (butuh internet
 *    dan Server Key Sandbox/Production yang valid di menu Pengaturan).
 * 3. Halaman pembayaran Midtrans dibuka di browser - QR / nomor VA yang asli
 *    diterbitkan Midtrans di halaman itu, BUKAN digambar/dibuat lokal.
 * 4. Aplikasi mengecek status ke Midtrans secara berkala di background thread.
 *    Transaksi hanya dianggap LUNAS bila Midtrans menjawab settlement/capture.
 *
 * Mode simulasi hanya tersedia bila Server Key BELUM dikonfigurasi, dan
 * hasilnya selalu ditandai jelas sebagai SIMULASI (bukan pembayaran asli).
 */
public class MidtransPaymentDialog extends JDialog {

    private final MidtransService midtransService = new MidtransService();
    private final String orderId;
    private final BigDecimal amount;
    private final String customerName;
    private boolean paid = false;
    private boolean simulation = false;
    private String paymentType = "QRIS";
    private Timer pollingTimer;

    private static final record MetodePilihan(String label, String code, String instruksi) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final MetodePilihan[] daftarMetode = new MetodePilihan[]{
        new MetodePilihan("QRIS (semua e-wallet / m-Banking)", "qris", "QR asli akan diterbitkan Midtrans di halaman pembayaran. Scan dengan e-wallet / m-Banking apa pun."),
        new MetodePilihan("BCA Virtual Account", "bca_va", "Nomor VA asli akan diterbitkan Midtrans di halaman pembayaran setelah halaman dibuka."),
        new MetodePilihan("BRI Virtual Account (BRIVA)", "bri_va", "Nomor BRIVA asli akan diterbitkan Midtrans di halaman pembayaran setelah halaman dibuka."),
        new MetodePilihan("BNI Virtual Account", "bni_va", "Nomor VA BNI asli akan diterbitkan Midtrans di halaman pembayaran setelah halaman dibuka."),
        new MetodePilihan("Mandiri Bill Payment", "echannel", "Kode perusahaan & nomor pembayaran asli akan diterbitkan Midtrans di halaman pembayaran."),
        new MetodePilihan("GoPay", "gopay", "Selesaikan pembayaran di aplikasi Gojek / halaman Midtrans yang dibuka."),
        new MetodePilihan("ShopeePay", "shopeepay", "Selesaikan pembayaran di aplikasi Shopee / halaman Midtrans yang dibuka."),
        new MetodePilihan("Semua Metode (Buka Halaman Midtrans Snap)", "AUTO", "Pilih metode pembayaran lengkap di halaman web Midtrans.")
    };

    private final JComboBox<MetodePilihan> cmbMetode = new JComboBox<>(daftarMetode);
    private final JLabel lblInstruksi = new JLabel();
    private final JLabel lblStatus = new JLabel("Belum membuka halaman pembayaran Midtrans.");
    private String lastRedirectUrl = null;
    private String snapMethodCode = null;

    private final UiKit.SoftButton btnWeb = new UiKit.SoftButton("Buka Halaman Pembayaran Midtrans", UiKit.Variant.PRIMARY);
    private final UiKit.SoftButton btnCek = new UiKit.SoftButton("Cek Status Pembayaran", UiKit.Variant.GHOST);
    private final UiKit.SoftButton btnSimulasi = new UiKit.SoftButton("Mode Simulasi (Offline)", UiKit.Variant.GHOST);

    public MidtransPaymentDialog(Window owner, String orderId, BigDecimal amount, String customerName) {
        super(owner, "Pilih Metode Pembayaran Midtrans", ModalityType.APPLICATION_MODAL);
        this.orderId = orderId;
        this.amount = amount;
        this.customerName = customerName;

        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Theme.CANVAS);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopPolling();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INK, 2),
                new EmptyBorder(22, 26, 22, 26)));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("PEMBAYARAN GATEWAY MIDTRANS");
        title.setFont(Theme.display(Font.BOLD, 16));
        title.setForeground(Theme.ACCENT);
        header.add(title, BorderLayout.WEST);

        JLabel lblOrder = new JLabel("Order #" + orderId);
        lblOrder.setFont(Theme.mono(Font.BOLD, 11));
        lblOrder.setForeground(Theme.MUTED);
        header.add(lblOrder, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 16, 0));

        JLabel lblTagihan = new JLabel("Total Tagihan: " + Fmt.rupiah(amount));
        lblTagihan.setFont(Theme.mono(Font.BOLD, 22));
        lblTagihan.setForeground(Theme.INK);
        lblTagihan.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblTagihan);
        body.add(Box.createVerticalStrut(14));

        JLabel lblPilih = new JLabel("Pilih Metode Pembayaran (QRIS / VA / E-Wallet):");
        lblPilih.setFont(Theme.text(Font.BOLD, 12));
        lblPilih.setForeground(Theme.INK_SOFT);
        lblPilih.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblPilih);
        body.add(Box.createVerticalStrut(6));

        cmbMetode.setFont(Theme.text(Font.PLAIN, 13));
        cmbMetode.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbMetode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cmbMetode.setPreferredSize(new Dimension(0, 38));
        cmbMetode.addActionListener(e -> updateInstruksiMetode());
        body.add(cmbMetode);
        body.add(Box.createVerticalStrut(12));

        UiKit.SurfacePanel infoBox = new UiKit.SurfacePanel(new BorderLayout(12, 0));
        infoBox.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        infoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        lblInstruksi.setFont(Theme.text(Font.PLAIN, 12));
        lblInstruksi.setForeground(Theme.INK);
        infoBox.add(lblInstruksi, BorderLayout.CENTER);
        body.add(infoBox);
        body.add(Box.createVerticalStrut(12));

        boolean configured = midtransService.isConfigured();
        JLabel lblMode = new JLabel(configured
                ? "<html>Mode: <b>MIDTRANS ASLI</b> (" + (midtransService.isProduction() ? "Production" : "Sandbox") + ") - butuh koneksi internet. QR / nomor VA asli ada di halaman Midtrans, bukan di dialog ini.</html>"
                : "<html>Mode: <b>SIMULASI</b> - Server Key Midtrans belum diisi di menu Pengaturan, jadi pembayaran asli belum bisa dipakai. Transaksi simulasi akan ditandai SIMULASI di riwayat.</html>");
        lblMode.setFont(Theme.text(Font.PLAIN, 12));
        lblMode.setForeground(configured ? Theme.INK : Theme.ACCENT);
        lblMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblMode);
        body.add(Box.createVerticalStrut(10));

        lblStatus.setFont(Theme.text(Font.BOLD, 12));
        lblStatus.setForeground(Theme.MUTED);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblStatus);

        root.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        btnWeb.setFont(Theme.text(Font.BOLD, 12));
        btnWeb.setToolTipText("Buat transaksi Snap ke Midtrans lalu buka halaman pembayarannya di browser");
        btnWeb.setEnabled(configured);
        btnWeb.addActionListener(e -> bukaHalamanMidtransWeb());
        actions.add(btnWeb);

        btnCek.setToolTipText("Tanya status terbaru transaksi ini langsung ke server Midtrans");
        btnCek.setEnabled(false);
        btnCek.addActionListener(e -> checkPaymentAsync(true));
        actions.add(btnCek);

        btnSimulasi.setToolTipText("Hanya untuk demo offline tanpa Server Key. Hasilnya ditandai SIMULASI, bukan pembayaran asli.");
        btnSimulasi.setVisible(!configured);
        btnSimulasi.addActionListener(e -> selesaikanSimulasi());
        actions.add(btnSimulasi);

        UiKit.SoftButton btnBatal = new UiKit.SoftButton("Batal", UiKit.Variant.GHOST);
        btnBatal.addActionListener(e -> {
            stopPolling();
            dispose();
        });
        actions.add(btnBatal);

        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(620, 440);
        setLocationRelativeTo(owner);

        updateInstruksiMetode();
    }

    public boolean isPaid() {
        return paid;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getPaymentCode() {
        MetodePilihan m = (MetodePilihan) cmbMetode.getSelectedItem();
        return m == null ? "qris" : m.code;
    }

    private void updateInstruksiMetode() {
        MetodePilihan m = (MetodePilihan) cmbMetode.getSelectedItem();
        if (m == null) return;
        lblInstruksi.setText("<html>" + m.instruksi + "</html>");
        if (snapMethodCode != null && !snapMethodCode.equals(m.code)) {
            lastRedirectUrl = null;
            snapMethodCode = null;
            btnCek.setEnabled(false);
            stopPolling();
            lblStatus.setText("Metode diganti - klik 'Buka Halaman Pembayaran Midtrans' lagi untuk metode baru.");
        }
    }

    private void bukaHalamanMidtransWeb() {
        if (!midtransService.isConfigured()) {
            Dialogs.peringatan(this, "Server Key Midtrans belum dikonfigurasi di menu Pengaturan.\n"
                    + "Isi Server Key + Client Key Sandbox dari dashboard.sandbox.midtrans.com > Settings > Access Keys,\n"
                    + "atau gunakan Mode Simulasi (hasilnya ditandai SIMULASI).");
            return;
        }
        if (lastRedirectUrl != null) {
            bukaBrowser(lastRedirectUrl);
            startPolling();
            return;
        }

        MetodePilihan m = (MetodePilihan) cmbMetode.getSelectedItem();
        String code = m == null ? "AUTO" : m.code;
        btnWeb.setEnabled(false);
        lblStatus.setText("Menghubungi Midtrans (butuh internet)...");

        new SwingWorker<SnapResponse, Void>() {
            @Override
            protected SnapResponse doInBackground() throws Exception {
                return midtransService.createSnapTransaction(orderId, amount, customerName, null, code);
            }

            @Override
            protected void done() {
                btnWeb.setEnabled(true);
                try {
                    SnapResponse snap = get();
                    lastRedirectUrl = snap.getRedirectUrl();
                    snapMethodCode = code;
                    btnCek.setEnabled(true);
                    bukaBrowser(lastRedirectUrl);
                    lblStatus.setText("Halaman pembayaran dibuka di browser. Selesaikan pembayaran di sana - status dicek otomatis...");
                    startPolling();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblStatus.setText("Gagal menghubungi Midtrans.");
                    Dialogs.peringatan(MidtransPaymentDialog.this,
                            "Tidak dapat membuat pembayaran Midtrans:\n" + cause.getMessage());
                }
            }
        }.execute();
    }

    private void bukaBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                lblStatus.setText("Buka alamat pembayaran ini di browser: " + url);
            }
        } catch (Exception ex) {
            lblStatus.setText("Gagal membuka browser. Alamat pembayaran: " + url);
        }
    }

    private void selesaikanSimulasi() {
        int ok = JOptionPane.showConfirmDialog(this,
                "Ini MODE SIMULASI: tidak ada uang yang berpindah dan tidak ada verifikasi ke Midtrans.\n"
                        + "Transaksi akan disimpan dengan metode SIMULASI, bukan MIDTRANS.\n\nLanjutkan simulasi?",
                "Konfirmasi Mode Simulasi", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        stopPolling();
        MetodePilihan m = (MetodePilihan) cmbMetode.getSelectedItem();
        paymentType = (m != null ? m.label : "QRIS") + " (SIMULASI)";
        simulation = true;
        paid = true;
        dispose();
    }

    private void startPolling() {
        stopPolling();
        pollingTimer = new Timer(5000, e -> checkPaymentAsync(false));
        pollingTimer.setInitialDelay(5000);
        pollingTimer.start();
    }

    private void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.stop();
            pollingTimer = null;
        }
    }

    private void checkPaymentAsync(boolean manual) {
        if (snapMethodCode == null) return;
        new SwingWorker<StatusResponse, Void>() {
            @Override
            protected StatusResponse doInBackground() throws Exception {
                return midtransService.checkStatus(orderId);
            }

            @Override
            protected void done() {
                try {
                    StatusResponse status = get();
                    if (status.isSuccess()) {
                        stopPolling();
                        paid = true;
                        simulation = false;
                        paymentType = status.getPaymentType().isBlank()
                                ? getPaymentCode().toUpperCase()
                                : status.getPaymentType().toUpperCase().replace("_", " ");
                        lblStatus.setText("PEMBAYARAN SUKSES TERVERIFIKASI MIDTRANS!");
                        Dialogs.info(MidtransPaymentDialog.this, "Pembayaran Midtrans sukses dan terverifikasi oleh server Midtrans!");
                        dispose();
                    } else if (status.isPending()) {
                        lblStatus.setText("Menunggu pembayaran (pending)... Selesaikan di halaman Midtrans, status dicek otomatis.");
                    } else if (status.isFailed()) {
                        stopPolling();
                        lblStatus.setText("Pembayaran " + status.getTransactionStatus() + " - belum lunas, transaksi tidak disimpan sebagai lunas.");
                    } else if (status.isNotFound()) {
                        lblStatus.setText("Midtrans belum mencatat pembayaran untuk order ini. Selesaikan dulu di halaman pembayaran.");
                    }
                } catch (Exception ex) {
                    if (manual) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        Dialogs.peringatan(MidtransPaymentDialog.this, "Cek status gagal:\n" + cause.getMessage());
                    }
                }
            }
        }.execute();
    }
}
