package giriatk.view.dialog;

import giriatk.model.Barang;
import giriatk.util.Fmt;
import giriatk.util.ImageUtil;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BarangDetailDialog extends JDialog {

    private final Barang barang;
    private boolean direstok = false;
    private boolean diubah = false;

    public BarangDetailDialog(Window owner, Barang barang) {
        super(owner, "Detail Barang — " + barang.getNama(), ModalityType.APPLICATION_MODAL);
        this.barang = barang;
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Theme.CANVAS);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INK, 2),
                new EmptyBorder(20, 24, 20, 24)));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildActions(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setSize(520, Math.max(480, getHeight()));
        setLocationRelativeTo(owner);
    }

    public boolean isDirestok() {
        return direstok;
    }

    public boolean isDiubah() {
        return diubah;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("INFORMASI LENGKAP BARANG");
        title.setFont(Theme.mono(Font.BOLD, 10));
        title.setForeground(Theme.ACCENT);

        JLabel code = new JLabel(barang.getKode());
        code.setFont(Theme.mono(Font.BOLD, 18));
        code.setForeground(Theme.INK);

        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        textWrap.add(title);
        textWrap.add(Box.createVerticalStrut(2));
        textWrap.add(code);

        header.add(textWrap, BorderLayout.WEST);
        header.add(buildStatusBadge(), BorderLayout.EAST);
        return header;
    }

    private JLabel buildStatusBadge() {
        JLabel badge = new JLabel();
        badge.setOpaque(true);
        badge.setFont(Theme.mono(Font.BOLD, 11));
        badge.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        if (barang.isHabis()) {
            badge.setText("HABIS");
            badge.setBackground(new Color(254, 226, 226));
            badge.setForeground(Theme.DANGER);
        } else if (barang.isMenipis()) {
            badge.setText("STOK MENIPIS");
            badge.setBackground(new Color(254, 243, 199));
            badge.setForeground(new Color(180, 83, 9));
        } else {
            badge.setText("STOK AMAN");
            badge.setBackground(new Color(220, 252, 231));
            badge.setForeground(Theme.GREEN_FG);
        }
        return badge;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 0, 16, 0));

        // Gambar Produk
        JLabel imgLabel = new JLabel();
        imgLabel.setPreferredSize(new Dimension(100, 100));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setVerticalAlignment(SwingConstants.CENTER);
        imgLabel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        imgLabel.setIcon(ImageUtil.getThumbnail(barang.getGambar(), 96, 96));
        body.add(imgLabel, BorderLayout.WEST);

        // Details Grid
        JPanel infoGrid = new JPanel(new GridLayout(7, 2, 8, 8));
        infoGrid.setOpaque(false);

        BigDecimal laba = barang.getHargaJual().subtract(barang.getHargaBeli());
        String pctStr = "";
        if (barang.getHargaBeli().signum() > 0) {
            BigDecimal pct = laba.multiply(BigDecimal.valueOf(100))
                    .divide(barang.getHargaBeli(), 1, RoundingMode.HALF_UP);
            pctStr = " (" + pct.toPlainString() + "%)";
        }

        addInfoRow(infoGrid, "Nama Barang", barang.getNama());
        addInfoRow(infoGrid, "Merk / Brand", barang.getMerk() == null || barang.getMerk().isBlank() ? "-" : barang.getMerk());
        addInfoRow(infoGrid, "Jenis / Tipe", barang.getJenis() == null || barang.getJenis().isBlank() ? "-" : barang.getJenis());
        addInfoRow(infoGrid, "Warna / Ukuran", (barang.getWarna() == null ? "" : barang.getWarna()) + " " + (barang.getUkuran() == null ? "" : barang.getUkuran()));
        addInfoRow(infoGrid, "Kategori", barang.getKategoriNama() == null ? "Tanpa Kategori" : barang.getKategoriNama());
        addInfoRow(infoGrid, "Harga Beli / Jual", Fmt.rupiah(barang.getHargaBeli()) + "  →  " + Fmt.rupiah(barang.getHargaJual()));
        addInfoRow(infoGrid, "Margin Laba", Fmt.rupiah(laba) + pctStr);

        body.add(infoGrid, BorderLayout.CENTER);
        return body;
    }

    private void addInfoRow(JPanel grid, String label, String value) {
        JLabel l = new JLabel(label);
        l.setFont(Theme.text(Font.BOLD, 11));
        l.setForeground(Theme.INK_SOFT);

        JLabel v = new JLabel(value);
        v.setFont(Theme.text(Font.PLAIN, 12));
        v.setForeground(Theme.INK);

        grid.add(l);
        grid.add(v);
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        UiKit.SoftButton btnEdit = new UiKit.SoftButton("Ubah Barang", UiKit.Variant.GHOST);
        btnEdit.addActionListener(e -> {
            BarangDialog dlg = new BarangDialog(getOwner(), barang);
            dlg.setVisible(true);
            if (dlg.isTersimpan()) {
                diubah = true;
                dispose();
            }
        });
        actions.add(btnEdit);

        UiKit.SoftButton btnTutup = new UiKit.SoftButton("Tutup", UiKit.Variant.PRIMARY);
        btnTutup.addActionListener(e -> dispose());
        actions.add(btnTutup);

        return actions;
    }
}
