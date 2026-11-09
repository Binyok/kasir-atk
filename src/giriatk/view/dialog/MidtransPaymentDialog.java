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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.net.URI;

public class MidtransPaymentDialog extends JDialog {

    private final MidtransService midtransService = new MidtransService();
    private final String orderId;
    private final BigDecimal amount;
    private boolean paid = false;
    private String paymentType = "";
    private Timer pollingTimer;

    private final JLabel lblStatus = new JLabel("Menunggu pembayaran...");
    private final JLabel lblPaymentType = new JLabel("-");
    private final JLabel lblAmount = new JLabel("-");

    public MidtransPaymentDialog(Window owner, String orderId, BigDecimal amount, String customerName) {
        super(owner, "Pembayaran Midtrans", ModalityType.APPLICATION_MODAL);
        this.orderId = orderId;
        this.amount = amount;
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Theme.CANVAS);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INK, 2),
                new EmptyBorder(24, 28, 24, 28)));

        JLabel title = new JLabel("PEMBAYARAN MIDTRANS");
        title.setFont(Theme.display(Font.BOLD, 16));
        title.setForeground(Theme.ACCENT);
        root.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 16, 0));

        lblAmount.setText(Fmt.rupiah(amount));
        lblAmount.setFont(Theme.mono(Font.BOLD, 22));
        lblAmount.setForeground(Theme.INK);
        lblAmount.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblStatus.setFont(Theme.text(Font.BOLD, 13));
        lblStatus.setForeground(Theme.MUTED);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblPaymentType.setFont(Theme.mono(Font.PLAIN, 11));
        lblPaymentType.setForeground(Theme.FAINT);
        lblPaymentType.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblOrder = new JLabel("No. Order: " + orderId);
        lblOrder.setFont(Theme.mono(Font.PLAIN, 11));
        lblOrder.setForeground(Theme.MUTED);
        lblOrder.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(lblAmount);
        body.add(Box.createVerticalStrut(8));
        body.add(lblOrder);
        body.add(Box.createVerticalStrut(12));
        body.add(lblStatus);
        body.add(Box.createVerticalStrut(4));
        body.add(lblPaymentType);
        root.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        UiKit.SoftButton batal = new UiKit.SoftButton("Batal / Tutup", UiKit.Variant.GHOST);
        batal.addActionListener(e -> {
            stopPolling();
            dispose();
        });
        actions.add(batal);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(420, 300);
        setLocationRelativeTo(owner);

        SwingUtilities.invokeLater(() -> createAndOpenSnap(customerName));
    }

    public boolean isPaid() {
        return paid;
    }

    public String getPaymentType() {
        return paymentType;
    }

    private void createAndOpenSnap(String customerName) {
        try {
            SnapResponse snap = midtransService.createSnapTransaction(orderId, amount, customerName, null);
            lblStatus.setText("Menunggu pembayaran... (halaman pembayaran sudah terbuka di browser)");
            lblPaymentType.setText("Snap Token: " + snap.getToken());
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(snap.getRedirectUrl()));
            }
            startPolling();
        } catch (Exception ex) {
            lblStatus.setText("GAGAL membuat transaksi Midtrans.");
            lblPaymentType.setText(ex.getMessage());
            Dialogs.error(this, "Gagal memulai pembayaran Midtrans:\n" + ex.getMessage());
        }
    }

    private void startPolling() {
        pollingTimer = new Timer(4000, e -> checkPayment());
        pollingTimer.setInitialDelay(5000);
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
                paymentType = status.getPaymentType();
                lblStatus.setText("PEMBAYARAN BERHASIL!");
                lblStatus.setForeground(Theme.GREEN_FG);
                lblPaymentType.setText("Metode: " + paymentType.toUpperCase().replace("_", " "));
                Dialogs.info(this, "Pembayaran Midtrans berhasil!\nMetode: " + paymentType);
                dispose();
            } else if (status.isFailed()) {
                stopPolling();
                lblStatus.setText("Pembayaran gagal / dibatalkan.");
                lblStatus.setForeground(Theme.DANGER);
                lblPaymentType.setText("Status: " + status.getTransactionStatus());
            } else {
                lblStatus.setText("Menunggu pembayaran... (status: " + status.getTransactionStatus() + ")");
            }
        } catch (Exception ignored) {
        }
    }
}
