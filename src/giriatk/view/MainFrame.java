package giriatk.view;

import giriatk.model.User;
import giriatk.util.Icons;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;
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
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, UiKit.NavItem> navItems = new LinkedHashMap<>();
    private final Map<String, BasePanel> panels = new LinkedHashMap<>();
    private final JLabel lblClock = new JLabel();
    private String currentKey;

    public MainFrame() {
        User user = Session.user();
        setTitle("GiriATK & Print — Station Controller (" + (user == null ? "" : user.getNamaLengkap()) + ")");
        setSize(1360, 840);
        setMinimumSize(new Dimension(1160, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildRoot());
        
        startClock();
        showPage("dashboard");
    }

    private void startClock() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm:ss", Locale.of("id", "ID"));
        Timer timer = new Timer(1000, e -> lblClock.setText(dtf.format(LocalDateTime.now()).toUpperCase()));
        timer.setInitialDelay(0);
        timer.start();
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        registerPages();
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SURFACE);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        // Left Branding & Telemetry
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setBorder(new EmptyBorder(0, 20, 0, 0));

        JComponent logo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Icons.paintLogo(g2, 26, Theme.ACCENT, new Color(0x0A0C10));
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(26, 26));
        logo.setMaximumSize(new Dimension(26, 26));
        left.add(logo);
        left.add(Box.createHorizontalStrut(12));

        JLabel title = new JLabel("GiriATK & Print");
        title.setFont(Theme.display(Font.BOLD, 15));
        title.setForeground(Theme.INK);
        left.add(title);
        left.add(Box.createHorizontalStrut(16));

        // Status badge
        JLabel statusPill = new JLabel("● SYSTEM ONLINE");
        statusPill.setFont(Theme.mono(Font.BOLD, 10));
        statusPill.setForeground(Theme.GREEN_FG);
        left.add(statusPill);

        bar.add(left, BorderLayout.WEST);

        // Center Live Clock
        lblClock.setFont(Theme.mono(Font.PLAIN, 11));
        lblClock.setForeground(Theme.MUTED);
        lblClock.setHorizontalAlignment(JLabel.CENTER);
        bar.add(lblClock, BorderLayout.CENTER);

        // Right User Quick Profile & Logout
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setBorder(new EmptyBorder(0, 0, 0, 16));

        User user = Session.user();
        String nama = user == null ? "User" : user.getNamaLengkap();
        String role = user == null ? "GUEST" : (user.isOwner() ? "OWNER" : "KASIR");

        JLabel roleBadge = new JLabel(" " + role + " ");
        roleBadge.setFont(Theme.mono(Font.BOLD, 10));
        roleBadge.setOpaque(true);
        roleBadge.setBackground(user != null && user.isOwner() ? Theme.ACCENT_SOFT : Theme.BLUE_BG);
        roleBadge.setForeground(user != null && user.isOwner() ? Theme.ACCENT : Theme.BLUE_FG);
        roleBadge.setBorder(BorderFactory.createLineBorder(user != null && user.isOwner() ? Theme.ACCENT : Theme.BLUE_FG));
        right.add(roleBadge);
        right.add(Box.createHorizontalStrut(10));

        JLabel userLabel = new JLabel(nama);
        userLabel.setFont(Theme.text(Font.BOLD, 12));
        userLabel.setForeground(Theme.INK);
        right.add(userLabel);
        right.add(Box.createHorizontalStrut(14));

        // Precision Hardware Theme Toggle Switch (Sun / Moon)
        UiKit.ThemeToggleSwitch themeSwitch = new UiKit.ThemeToggleSwitch(MainFrame.this);
        themeSwitch.setOnToggle(() -> content.repaint());
        right.add(themeSwitch);
        right.add(Box.createHorizontalStrut(12));

        UiKit.SoftButton logout = new UiKit.SoftButton("Keluar", UiKit.Variant.GHOST);
        logout.setPreferredSize(new Dimension(80, 32));
        logout.setMaximumSize(new Dimension(80, 32));
        logout.addActionListener(e -> handleLogout());
        right.add(logout);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private final JPanel navContainer = new JPanel();

    private void registerPages() {
        navContainer.removeAll();
        navItems.clear();
        panels.clear();

        // 1. Group Operasional
        addNavHeader("OPERASIONAL");
        addPage("dashboard", "Dashboard", Icons.Name.DASHBOARD, false, false, new DashboardPanel());
        addPage("transaksi", "Transaksi (POS)", Icons.Name.CART, false, true, new TransaksiPanel());
        addPage("antrian", "Antrian Jasa", Icons.Name.QUEUE, false, true, new AntrianPanel());
        addPage("riwayat", "Riwayat Transaksi", Icons.Name.RECEIPT, false, false, new RiwayatPanel());

        // 2. Group Inventaris
        addNavHeader("INVENTARIS");
        addPage("stok", "Stok & Barang", Icons.Name.BOX, false, false, new StokPanel());
        addPage("layanan", "Layanan Jasa", Icons.Name.PRINTER, true, false, new LayananPanel());

        // 3. Group Manajemen
        if (Session.isOwner()) {
            addNavHeader("MANAJEMEN");
            addPage("laporan", "Laporan Keuangan", Icons.Name.CHART, true, false, new LaporanPanel());
            addPage("pengguna", "Kelola Pengguna", Icons.Name.USERS, true, false, new UserPanel());
            addPage("pengaturan", "Pengaturan & Backup", Icons.Name.SETTINGS, true, false, new PengaturanPanel());
        }
    }

    private void addNavHeader(String label) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.mono(Font.BOLD, 10));
        lbl.setForeground(Theme.FAINT);
        lbl.setBorder(new EmptyBorder(12, 16, 4, 16));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        item.setPreferredSize(new Dimension(220, 40));
        item.addActionListener(e -> showPage(key));
        navItems.put(key, item);
        panels.put(key, panel);
        navContainer.add(item);
        navContainer.add(Box.createVerticalStrut(2));
        content.add(panel, key);
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SURFACE);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        navContainer.setOpaque(false);
        navContainer.setLayout(new BoxLayout(navContainer, BoxLayout.Y_AXIS));
        navContainer.setBorder(new EmptyBorder(12, 10, 12, 10));

        JScrollPane scrollNav = new JScrollPane(navContainer);
        scrollNav.setBorder(null);
        scrollNav.setOpaque(false);
        scrollNav.getViewport().setOpaque(false);
        sidebar.add(scrollNav, BorderLayout.CENTER);

        // Footer Telemetry
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(new EmptyBorder(12, 16, 14, 16));
        
        JLabel ver = new JLabel("v2.6 PRO • TERMINAL READY");
        ver.setFont(Theme.mono(Font.PLAIN, 10));
        ver.setForeground(Theme.FAINT);
        foot.add(ver, BorderLayout.WEST);

        sidebar.add(foot, BorderLayout.SOUTH);
        return sidebar;
    }

    private void showPage(String key) {
        BasePanel panel = panels.get(key);
        if (panel == null) {
            return;
        }
        currentKey = key;
        cards.show(content, key);
        navItems.forEach((k, item) -> item.setActive(k.equals(key)));
        try {
            panel.onShow();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal memuat halaman: " + ex.getMessage(),
                    "Kesalahan", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
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
}
