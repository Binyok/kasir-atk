package giriatk.view.dialog;

import giriatk.model.TipeItem;
import giriatk.model.Transaksi;
import giriatk.model.TransaksiItem;
import giriatk.util.Fmt;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;

public class TransaksiDetailDialog extends JDialog {

    public TransaksiDetailDialog(Window owner, Transaksi transaksi) {
        super(owner, "Detail Transaksi " + transaksi.getKode(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));

        root.add(buildHeader(transaksi), BorderLayout.NORTH);

        JPanel tengah = new JPanel();
        tengah.setOpaque(false);
        tengah.setLayout(new BoxLayout(tengah, BoxLayout.Y_AXIS));
        tengah.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        tengah.add(buildInfo(transaksi));
        tengah.add(Box.createVerticalStrut(14));
        tengah.add(buildItems(transaksi));
        tengah.add(Box.createVerticalStrut(14));
        tengah.add(buildTotals(transaksi));
        root.add(tengah, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        UiKit.SoftButton tutup = new UiKit.SoftButton("Tutup", UiKit.Variant.GHOST);
        tutup.addActionListener(e -> dispose());
        buttons.add(tutup);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(560, 620);
        setLocationRelativeTo(owner);
    }

    private JComponent buildHeader(Transaksi transaksi) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(transaksi.getKode());
        title.setFont(Theme.display(Font.BOLD, 20));
        title.setForeground(Theme.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel(Fmt.tanggalWaktu(transaksi.getTanggal()) + "  •  " + transaksi.getTipe().name());
        sub.setFont(Theme.text(Font.PLAIN, 12));
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(sub);
        return panel;
    }

    private JComponent buildInfo(Transaksi transaksi) {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 6));
        grid.setOpaque(false);
        grid.add(kunci("Kasir"));
        grid.add(nilai(transaksi.getUserName() == null ? "-" : transaksi.getUserName()));
        grid.add(kunci("Jumlah item"));
        grid.add(nilai(String.valueOf(transaksi.getItems().size())));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildItems(Transaksi transaksi) {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Rincian Item");
        title.setFont(Theme.text(Font.BOLD, 13));
        title.setForeground(Theme.INK);
        card.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Item", "Tipe", "Qty", "Harga", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (TransaksiItem item : transaksi.getItems()) {
            model.addRow(new Object[]{
                item.getNamaItem(),
                item.getTipe() == TipeItem.BARANG ? "Barang" : "Jasa",
                item.getQty(),
                Fmt.rupiah(item.getHarga()),
                Fmt.rupiah(item.getSubtotal())
            });
        }
        UiKit.AppTable table = new UiKit.AppTable(model);
        table.getColumnModel().getColumn(0).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.LEFT));
        table.getColumnModel().getColumn(1).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
        table.getColumnModel().getColumn(2).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.CENTER));
        table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT));
        table.getColumnModel().getColumn(4).setCellRenderer(new UiKit.PaddedRenderer(SwingConstants.RIGHT, true));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(0, 210));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildTotals(Transaksi transaksi) {
        UiKit.SurfacePanel card = new UiKit.SurfacePanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(baris("Subtotal", Fmt.rupiah(transaksi.getSubtotal()), false));
        rows.add(Box.createVerticalStrut(4));
        rows.add(baris("Total", Fmt.rupiah(transaksi.getTotal()), true));
        rows.add(Box.createVerticalStrut(4));
        rows.add(baris("Bayar", Fmt.rupiah(transaksi.getBayar()), false));
        rows.add(Box.createVerticalStrut(4));
        rows.add(baris("Kembali", Fmt.rupiah(transaksi.getKembali()), false));
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JComponent baris(String label, String value, boolean tebal) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel kiri = new JLabel(label);
        kiri.setFont(Theme.text(tebal ? Font.BOLD : Font.PLAIN, tebal ? 13 : 12));
        kiri.setForeground(tebal ? Theme.INK : Theme.MUTED);
        JLabel kanan = new JLabel(value);
        kanan.setFont(Theme.text(Font.BOLD, tebal ? 15 : 13));
        kanan.setForeground(Theme.INK);
        row.add(kiri, BorderLayout.WEST);
        row.add(kanan, BorderLayout.EAST);
        return row;
    }

    private JLabel kunci(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.text(Font.BOLD, 12));
        label.setForeground(Theme.MUTED);
        return label;
    }

    private JLabel nilai(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.text(Font.PLAIN, 13));
        label.setForeground(Theme.INK);
        return label;
    }
}
