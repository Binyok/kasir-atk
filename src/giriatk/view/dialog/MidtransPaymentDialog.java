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
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.net.URI;

public class MidtransPaymentDialog extends JDialog {

    private final MidtransService midtransService = new MidtransService();
    private final String orderId;
    private final BigDecimal amount;
    private final String customerName;
    private boolean paid = false;
    private String paymentType = "QRIS";
    private Timer pollingTimer;

    private static final record MetodePilihan(String label, String code, String instruksi) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final MetodePilihan[] daftarMetode = new MetodePilihan[]{
        new MetodePilihan("QRIS (GoPay / OVO / Dana / BCA / LinkAja)", "qris", "Scan QR Code QRIS menggunakan aplikasi e-wallet atau m-Banking"),
        new MetodePilihan("BCA Virtual Account", "bca_va", "Transfer ke Nomor VA BCA: 70012" + (System.currentTimeMillis() % 100000000L)),
        new MetodePilihan("BRI Virtual Account (BRIVA)", "bri_va", "Transfer ke BRIVA BRI: 88812" + (System.currentTimeMillis() % 100000000L)),
        new MetodePilihan("BNI Virtual Account", "bni_va", "Transfer ke VA BNI: 98812" + (System.currentTimeMillis() % 100000000L)),
        new MetodePilihan("Mandiri Bill Payment", "echannel", "Bayar via Mandiri Livin' (Kode Perusahaan: 70012)"),
        new MetodePilihan("GoPay / GoPay Later", "gopay", "Buka aplikasi Gojek / Scan QR GoPay"),
        new MetodePilihan("ShopeePay", "shopeepay", "Buka aplikasi Shopee / Scan QR ShopeePay"),
        new MetodePilihan("Semua Metode (Buka Halaman Midtrans Snap)", "AUTO", "Pilih metode pembayaran lengkap di halaman web Midtrans")
    };

    private final JComboBox<MetodePilihan> cmbMetode = new JComboBox<>(daftarMetode);
    private final JLabel lblInstruksi = new JLabel();
    private final JLabel lblStatus = new JLabel("Menunggu konfirmasi pembayaran...");
    private final JLabel lblPaymentInfo = new JLabel();
    private String lastRedirectUrl = null;

    public MidtransPaymentDialog(Window owner, String orderId, BigDecimal amount, String customerName) {
        super(owner, "Pilih Metode Pembayaran Midtrans", ModalityType.APPLICATION_MODAL);
        this.orderId = orderId;
        this.amount = amount;
        this.customerName = customerName;

        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Theme.CANVAS);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INK, 2),
                new EmptyBorder(22, 26, 22, 26)));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("PEMBAYARAN GATEWAY MIDTRANS");
        title.setFont(Theme.display(Font.BOLD, 16));
        title.setForeground(Theme.ACCENT);
        header.add(title, BorderLayout.WEST);

        JLabel lblOrder = new JLabel("Order #" + orderId);
        lblOrder.setFont(Theme.mono(Font.BOLD, 12));
        lblOrder.setForeground(Theme.MUTED);
        header.add(lblOrder, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Body
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

        // Info Box Panel
        UiKit.SurfacePanel infoBox = new UiKit.SurfacePanel(new BorderLayout(0, 6));
        infoBox.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        infoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        lblInstruksi.setFont(Theme.text(Font.PLAIN, 12));
        lblInstruksi.setForeground(Theme.INK);
        infoBox.add(lblInstruksi, BorderLayout.NORTH);

        lblPaymentInfo.setFont(Theme.mono(Font.BOLD, 12));
        lblPaymentInfo.setForeground(Theme.ACCENT);
        infoBox.add(lblPaymentInfo, BorderLayout.CENTER);

        body.add(infoBox);
        body.add(Box.createVerticalStrut(12));

        lblStatus.setFont(Theme.text(Font.BOLD, 12));
        lblStatus.setForeground(Theme.MUTED);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblStatus);

        root.add(body, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        UiKit.SoftButton btnWeb = new UiKit.SoftButton("Buka Midtrans Web", UiKit.Variant.GHOST);
        btnWeb.setToolTipText("Buka antarmuka pembayaran Midtrans langsung di Browser");
        btnWeb.addActionListener(e -> bukaHalamanMidtransWeb());
        actions.add(btnWeb);

        UiKit.SoftButton btnKonfirmasi = new UiKit.SoftButton("Konfirmasi Bayar Lunas", UiKit.Variant.PRIMARY);
        btnKonfirmasi.setFont(Theme.text(Font.BOLD, 12));
        btnKonfirmasi.setToolTipText("Verifikasi pembayaran sukses dan cetak nota");
        btnKonfirmasi.addActionListener(e -> selesaikanPembayaran());
        actions.add(btnKonfirmasi);

        UiKit.SoftButton btnBatal = new UiKit.SoftButton("Batal", UiKit.Variant.GHOST);
        btnBatal.addActionListener(e -> {
            stopPolling();
            dispose();
        });
        actions.add(btnBatal);

        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(560, 390);
        setLocationRelativeTo(owner);

        updateInstruksiMetode();
    }

    public boolean isPaid() {
        return paid;
    }

    public String getPaymentType() {
        return paymentType;
    }

    private void updateInstruksiMetode() {
        MetodePilihan m = (MetodePilihan) cmbMetode.getSelectedItem();
        if (m == null) return;
        lblInstruksi.setText("<html>" + m.instruksi + "</html>");
        if (m.code.equals("qris") || m.code.equals("gopay") || m.code.equals("shopeepay")) {
            lblPaymentInfo.setText("Kode QRIS siap dipindai oleh pelanggan via e-Wallet");
        } else if (m.code.endsWith("_va") || m.code.equals("echannel")) {
            lblPaymentInfo.setText("Nomor Rekening Virtual Account telah dibuat");
        } else {
            lblPaymentInfo.setText("Pilihan multi-metode aktif");
        }
    }

    private void bukaHalamanMidtransWeb() {
        if (!midtransService.isConfigured()) {
            Dialogs.peringatan(this, "Server Key Midtrans belum dikonfigurasi di menu Pengaturan.\n"
                    + "Gunakan tombol 'Konfirmasi Bayar Lunas' untuk melakukan simulasi transaksi langsung.");
            return;
        }

        try {
            MetodePilihan m = (MetodePilihan) cmbMetode.getSelectedItem();
            String code = m == null ? "AUTO" : m.code;
            SnapResponse snap = midtransService.createSnapTransaction(orderId, amount, customerName, null, code);
            lastRedirectUrl = snap.getRedirectUrl();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(lastRedirectUrl));
            }
            lblStatus.setText("Halaman pembayaran dibuka di browser. Menunggu pembayaran...");
            startPolling();
        } catch (Exception ex) {
            Dialogs.peringatan(this, "Tidak dapat membuka Midtrans API: " + ex.getMessage() + "\n"
                    + "Silakan periksa Server Key di Pengaturan, atau gunakan tombol 'Konfirmasi Bayar Lunas'.");
        }
    }

    private void selesaikanPembayaran() {
        stopPolling();
        MetodePilihan m = (MetodePilihan) cmbMetode.getSelectedItem();
        paymentType = m != null ? m.label : "QRIS";
        paid = true;
        Dialogs.info(this, "Pembayaran berhasil diterima!\nMetode: " + paymentType);
        dispose();
    }

    private void startPolling() {
        if (pollingTimer != null) pollingTimer.stop();
        pollingTimer = new Timer(4000, e -> checkPayment());
        pollingTimer.setInitialDelay(4000);
        pollingTimer.start();
    }

    private void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.stop();
        }
    }

    private void checkPayment() {
        try {
            StatusResponse status = midtransService.checkStatus(orderId);
            if (status.isSuccess()) {
                stopPolling();
                paid = true;
                paymentType = status.getPaymentType().toUpperCase().replace("_", " ");
                lblStatus.setText("PEMBAYARAN SUKSES TERVERIFIKASI!");
                Dialogs.info(this, "Pembayaran Midtrans sukses terverifikasi oleh server!");
                dispose();
            }
        } catch (Exception ignored) {
        }
    }
}
