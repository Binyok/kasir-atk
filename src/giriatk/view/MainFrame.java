package giriatk.view;

import giriatk.config.AppConfig;
import giriatk.model.User;
import giriatk.util.Icons;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.TitleBar;
import giriatk.view.panel.AntrianPanel;
import giriatk.view.panel.BasePanel;
import giriatk.view.panel.DashboardPanel;
import giriatk.view.panel.LaporanPanel;
import giriatk.view.panel.LayananPanel;
import giriatk.view.panel.PengaturanPanel;
import giriatk.view.panel.RiwayatPanel;
import giriatk.view.panel.StokPanel;
import giriatk.view.panel.TransaksiPanel;
import giriatk.view.panel.UserPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private static final int SIDEBAR_EXPANDED = 232;
    private static final int SIDEBAR_RAIL = 64;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, UiKit.NavItem> navItems = new LinkedHashMap<>();
    private final Map<String, JLabel> navHeaders = new LinkedHashMap<>();
    private final Map<String, BasePanel> panels = new LinkedHashMap<>();
    private final JPanel sidebar = new JPanel(new BorderLayout());
    private final JPanel navContainer = new JPanel();
    private final JLabel wordmark = new JLabel();
    private final JLabel railMark = new JLabel("G", JLabel.CENTER);
    private final JLabel wordmarkSub = new JLabel("KASIR · STOK · CETAK");
    private final JLabel userName = new JLabel();
    private final JLabel userRole = new JLabel();
    private final JLabel themeNote = new JLabel("Tema: Kertas (Terang)");
    private UiKit.IconButton collapseButton;
    private UiKit.IconButton logoutButton;
    private TitleBar titleBar;
    private boolean collapsed;
    private String currentKey;
    private String baseTitle;

    public MainFrame() {
        User user = Session.user();
        baseTitle = "GiriATK & Print";
        setTitle(baseTitle);
        setUndecorated(true);
        setBackground(Theme.INK);
        setSize(1400, 880);
        setMinimumSize(new Dimension(1160, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildRoot());
        collapsed = AppConfig.getUiBool("ui.sidebar.collapsed", false);
        applyCollapse();
        showPage("dashboard");
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Theme.INK));

        titleBar = new TitleBar(this, baseTitle + " — Dashboard");
        root.add(titleBar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(buildSidebar(), BorderLayout.WEST);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 30, 24, 30));
        registerPages();
        body.add(content, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private void registerPages() {
        navContainer.removeAll();
        navItems.clear();
        navHeaders.clear();
        panels.clear();

        addNavHeader("OPERASIONAL");
        addPage("dashboard", "Dashboard", Icons.Name.DASHBOARD, false, false, new DashboardPanel());
        addPage("transaksi", "Transaksi (POS)", Icons.Name.CART, false, false, new TransaksiPanel());
        addPage("antrian", "Antrian Jasa", Icons.Name.QUEUE, false, false, new AntrianPanel());
        addPage("riwayat", "Riwayat Transaksi", Icons.Name.RECEIPT, false, false, new RiwayatPanel());

        addNavHeader("INVENTARIS");
        addPage("stok", "Stok & Barang", Icons.Name.BOX, false, false, new StokPanel());
        addPage("layanan", "Layanan Jasa", Icons.Name.PRINTER, false, false, new LayananPanel());

        addNavHeader("KELOLA");
        addPage("laporan", "Laporan", Icons.Name.CHART, false, false, new LaporanPanel());
        addPage("pengguna", "Pengguna", Icons.Name.USERS, false, false, new UserPanel());
        addPage("pengaturan", "Pengaturan", Icons.Name.SETTINGS, false, false, new PengaturanPanel());
        navContainer.add(Box.createVerticalGlue());
    }

    private void addNavHeader(String label) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.mono(Font.BOLD, 9));
        lbl.setForeground(Theme.SIDEBAR_FAINT);
        lbl.setBorder(new EmptyBorder(14, 10, 5, 10));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        navHeaders.put(label, lbl);
        navContainer.add(lbl);
    }

    private void addPage(String key, String label, Icons.Name icon, boolean ownerOnly, boolean kasirOnly, BasePanel panel) {
        boolean owner = Session.isOwner();
        if (ownerOnly && !owner) {
            return;
        }
        if (kasirOnly && owner) {
            return;
        }
        UiKit.NavItem item = new UiKit.NavItem(label, icon, true);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        item.setPreferredSize(new Dimension(204, 38));
        item.addActionListener(e -> showPage(key));
        navItems.put(key, item);
        panels.put(key, panel);
        navContainer.add(item);
        navContainer.add(Box.createVerticalStrut(2));
        content.add(panel, key);
    }

    private JComponent buildSidebar() {
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_EXPANDED, 0));
        sidebar.setBorder(new EmptyBorder(22, 14, 16, 14));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        wordmark.setText("<html><font color='#F3ECDC'>GiriATK</font><br>"
                + "<i><font color='#C93A1E'>&amp; Print</font></i></html>");
        wordmark.setFont(Theme.display(Font.PLAIN, 26));
        wordmark.setForeground(Theme.SIDEBAR_TEXT);
        wordmark.setAlignmentX(Component.LEFT_ALIGNMENT);
        wordmark.setBorder(new EmptyBorder(0, 10, 2, 0));
        header.add(wordmark);

        railMark.setFont(Theme.display(Font.BOLD, 26));
        railMark.setForeground(Theme.SIDEBAR_TEXT);
        railMark.setAlignmentX(Component.CENTER_ALIGNMENT);
        railMark.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        railMark.setToolTipText("GiriATK & Print");
        header.add(railMark);

        wordmarkSub.setFont(Theme.mono(Font.BOLD, 9));
        wordmarkSub.setForeground(Theme.SIDEBAR_MUTED);
        wordmarkSub.setBorder(new EmptyBorder(4, 10, 16, 0));
        wordmarkSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(wordmarkSub);

        JPanel divider = new DashedLine();
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(divider);
        header.add(Box.createVerticalStrut(12));

        JPanel collapseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        collapseRow.setOpaque(false);
        collapseRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        collapseButton = new UiKit.IconButton(Icons.Name.CHEVRON_LEFT, Theme.SIDEBAR_MUTED, 16);
        collapseButton.setBorder(new EmptyBorder(0, 6, 0, 0));
        collapseButton.addActionListener(e -> {
            collapsed = !collapsed;
            AppConfig.setUi("ui.sidebar.collapsed", String.valueOf(collapsed));
            applyCollapse();
        });
        collapseRow.add(collapseButton);
        header.add(collapseRow);

        sidebar.add(header, BorderLayout.NORTH);

        navContainer.setOpaque(false);
        navContainer.setLayout(new BoxLayout(navContainer, BoxLayout.Y_AXIS));
        navContainer.setBorder(new EmptyBorder(4, 0, 8, 0));

        JScrollPane scrollNav = new JScrollPane(navContainer);
        scrollNav.setBorder(null);
        scrollNav.setOpaque(false);
        scrollNav.getViewport().setOpaque(false);
        scrollNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollNav.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(scrollNav, BorderLayout.CENTER);

        sidebar.add(buildUserChip(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JComponent buildUserChip() {
        JPanel foot = new JPanel();
        foot.setOpaque(false);
        foot.setLayout(new BoxLayout(foot, BoxLayout.Y_AXIS));

        JPanel divider = new DashedLine();
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        foot.add(divider);
        foot.add(Box.createVerticalStrut(12));

        User user = Session.user();
        String nama = user == null ? "Pengguna" : user.getNamaLengkap();
        String role = user == null ? "GUEST" : "PENGELOLA";
        userName.setText(nama);
        userName.setFont(Theme.text(Font.BOLD, 13));
        userName.setForeground(Theme.SIDEBAR_TEXT);
        userRole.setText(role);
        userRole.setFont(Theme.mono(Font.BOLD, 9));
        userRole.setForeground(Theme.SIDEBAR_MUTED);

        JPanel chip = new JPanel(new BorderLayout(10, 0));
        chip.setOpaque(false);
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        chip.add(new Avatar(initials(nama)), BorderLayout.WEST);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(userName);
        texts.add(Box.createVerticalStrut(2));
        texts.add(userRole);
        chip.add(texts, BorderLayout.CENTER);
        foot.add(chip);

        JPanel controls = new JPanel(new BorderLayout(6, 0));
        controls.setOpaque(false);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        themeNote.setFont(Theme.text(Font.PLAIN, 10));
        themeNote.setForeground(Theme.SIDEBAR_FAINT);
        themeNote.setBorder(new EmptyBorder(8, 2, 0, 0));
        controls.add(themeNote, BorderLayout.CENTER);

        logoutButton = new UiKit.IconButton(Icons.Name.LOGOUT, Theme.SIDEBAR_MUTED, 16);
        logoutButton.setToolTipText("Keluar");
        logoutButton.addActionListener(e -> handleLogout());
        controls.add(logoutButton, BorderLayout.EAST);
        foot.add(controls);
        return foot;
    }

    private String initials(String nama) {
        String[] parts = nama.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private void applyCollapse() {
        sidebar.setPreferredSize(new Dimension(collapsed ? SIDEBAR_RAIL : SIDEBAR_EXPANDED, 0));
        sidebar.setBorder(new EmptyBorder(22, collapsed ? 8 : 14, 16, collapsed ? 8 : 14));
        wordmark.setVisible(!collapsed);
        railMark.setVisible(collapsed);
        wordmarkSub.setVisible(!collapsed);
        themeNote.setVisible(!collapsed);
        userName.setVisible(!collapsed);
        userRole.setVisible(!collapsed);
        navHeaders.values().forEach(l -> l.setVisible(!collapsed));

        int itemWidth = collapsed ? 40 : 204;
        for (UiKit.NavItem item : navItems.values()) {
            item.setRail(collapsed);
            item.setPreferredSize(new Dimension(itemWidth, 38));
            item.setMaximumSize(new Dimension(collapsed ? 40 : Integer.MAX_VALUE, 38));
        }
        collapseButton.setIconName(collapsed ? Icons.Name.CHEVRON_RIGHT : Icons.Name.CHEVRON_LEFT);
        collapseButton.setToolTipText(collapsed ? "Bentangkan sidebar" : "Ciutkan sidebar");
        collapseButton.revalidate();
        collapseButton.repaint();

        if (currentKey != null) {
            updateTitle();
        }
        sidebar.revalidate();
        sidebar.repaint();
        revalidate();
        repaint();
    }

    private void showPage(String key) {
        BasePanel panel = panels.get(key);
        if (panel == null) {
            return;
        }
        currentKey = key;
        cards.show(content, key);
        navItems.forEach((k, item) -> item.setActive(k.equals(key)));
        updateTitle();
        try {
            panel.onShow();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal memuat halaman: " + ex.getMessage(),
                    "Kesalahan", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void updateTitle() {
        String label = navItems.entrySet().stream()
                .filter(e -> e.getKey().equals(currentKey))
                .map(e -> e.getValue().getText())
                .findFirst().orElse("Dashboard");
        titleBar.setWindowTitle("GiriATK & Print — " + label);
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Keluar dari sesi terminal?", "Konfirmasi Keluar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        Session.logout();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static final class DashedLine extends JPanel {
        DashedLine() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Theme.SIDEBAR_LINE);
            for (int x = 0; x < getWidth(); x += 5) {
                g2.drawLine(x, 0, Math.min(x + 2, getWidth()), 0);
            }
            g2.dispose();
        }
    }

    private static final class Avatar extends JPanel {
        private final String initials;

        Avatar(String initials) {
            this.initials = initials;
            setOpaque(false);
            setPreferredSize(new Dimension(36, 36));
            setMaximumSize(new Dimension(36, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.ACCENT);
            g2.fillOval(0, 0, 35, 35);
            g2.setColor(Theme.SIDEBAR_TEXT);
            g2.drawOval(0, 0, 34, 34);
            g2.setFont(Theme.display(Font.BOLD, 14));
            g2.setColor(Theme.SURFACE);
            int w = g2.getFontMetrics().stringWidth(initials);
            g2.drawString(initials, (36 - w) / 2, 23);
            g2.dispose();
        }
    }
}
