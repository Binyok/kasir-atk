package giriatk.view;

import giriatk.model.User;
import giriatk.service.AuthService;
import giriatk.util.Icons;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private UiKit.SoftButton btnLogin;
    private JLabel lblError;

    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("GiriATK & Print — POS Terminal");
        setSize(980, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.add(createBrandTelemetryPanel(), BorderLayout.WEST);
        root.add(createFormPanel(), BorderLayout.CENTER);
        setContentPane(root);

        KeyAdapter enter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        txtUsername.addKeyListener(enter);
        txtPassword.addKeyListener(enter);
    }

    private JPanel createBrandTelemetryPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background & Grid based on theme
                g2.setColor(Theme.isDark() ? new Color(0x0E1117) : new Color(0xEDECE8));
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Fine technical blueprint lines
                g2.setColor(Theme.isDark() ? new Color(255, 255, 255, 8) : new Color(0, 0, 0, 10));
                for (int x = 24; x < getWidth(); x += 32) {
                    g2.drawLine(x, 0, x, getHeight());
                }
                for (int y = 24; y < getHeight(); y += 32) {
                    g2.drawLine(0, y, getWidth(), y);
                }
                
                // Subtle corner telemetry markers
                g2.setColor(Theme.alpha(Theme.ACCENT, 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(24, 24, 40, 24);
                g2.drawLine(24, 24, 24, 40);
                g2.drawLine(getWidth() - 24, getHeight() - 24, getWidth() - 40, getHeight() - 24);
                g2.drawLine(getWidth() - 24, getHeight() - 24, getWidth() - 24, getHeight() - 40);
                
                g2.dispose();
            }
        };
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(460, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(44, 48, 44, 48));

        // Station Header Tag
        JLabel tag = new JLabel("[ POS STATION // v2.6 ]");
        tag.setFont(Theme.mono(Font.BOLD, 10));
        tag.setForeground(Theme.ACCENT);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tag);
        content.add(Box.createVerticalStrut(18));

        // Brand Logo
        JComponent mark = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Icons.paintLogo(g2, 44, Theme.ACCENT, new Color(0x0A0C10));
                g2.dispose();
            }
        };
        mark.setOpaque(false);
        mark.setPreferredSize(new Dimension(44, 44));
        mark.setMaximumSize(new Dimension(44, 44));
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(mark);
        content.add(Box.createVerticalStrut(20));

        JLabel name = new JLabel("GiriATK & Print");
        name.setFont(Theme.display(Font.BOLD, 30));
        name.setForeground(Theme.INK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(name);

        content.add(Box.createVerticalStrut(8));
        JLabel tagline = new JLabel("<html>Sistem Kasir, Jasa Cetak &amp;<br>Manajemen Inventaris Terintegrasi</html>");
        tagline.setFont(Theme.text(Font.PLAIN, 13));
        tagline.setForeground(Theme.MUTED);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tagline);

        content.add(Box.createVerticalStrut(32));

        // Telemetry Feature Rows
        content.add(telemetryLine("TRANSAKSI_POS", "Penjualan cepat barang & jasa cetak"));
        content.add(Box.createVerticalStrut(14));
        content.add(telemetryLine("QUEUE_MONITOR", "Manajemen antrian print & fotokopi real-time"));
        content.add(Box.createVerticalStrut(14));
        content.add(telemetryLine("INVENTORY_LOG", "Stok minimum otomatis & restock"));

        content.add(Box.createVerticalStrut(36));
        
        // System Status Dot
        JPanel statusRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        statusRow.setOpaque(false);
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel greenDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.GREEN_FG);
                g2.fillOval(0, 1, 8, 8);
                g2.setColor(Theme.alpha(Theme.GREEN_FG, 60));
                g2.drawOval(0, 1, 8, 8);
                g2.dispose();
            }
        };
        greenDot.setOpaque(false);
        greenDot.setPreferredSize(new Dimension(10, 10));
        
        JLabel lblStatus = new JLabel("STATION ONLINE • READY FOR INPUT");
        lblStatus.setFont(Theme.mono(Font.BOLD, 10));
        lblStatus.setForeground(Theme.GREEN_FG);
        statusRow.add(greenDot);
        statusRow.add(lblStatus);
        content.add(statusRow);

        panel.add(content, new GridBagConstraints());
        return panel;
    }

    private JPanel telemetryLine(String code, String desc) {
        JPanel line = new JPanel();
        line.setLayout(new BoxLayout(line, BoxLayout.Y_AXIS));
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblCode = new JLabel("► " + code);
        lblCode.setFont(Theme.mono(Font.BOLD, 11));
        lblCode.setForeground(Theme.ACCENT);
        
        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(Theme.text(Font.PLAIN, 12));
        lblDesc.setForeground(Theme.MUTED);

        line.add(lblCode);
        line.add(Box.createVerticalStrut(2));
        line.add(lblDesc);
        return line;
    }

    private JPanel createFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.CANVAS);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(360, 460));
        form.setMaximumSize(new Dimension(360, 490));

        // Top toggle row with precision Theme Switch
        JPanel topToggleRow = new JPanel(new BorderLayout());
        topToggleRow.setOpaque(false);
        topToggleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        UiKit.ThemeToggleSwitch themeSwitch = new UiKit.ThemeToggleSwitch(LoginFrame.this);
        themeSwitch.setOnToggle(() -> outer.repaint());
        topToggleRow.add(themeSwitch, BorderLayout.EAST);
        form.add(topToggleRow);
        form.add(Box.createVerticalStrut(12));

        JLabel heading = new JLabel("Otentikasi Staf");
        heading.setFont(Theme.display(Font.BOLD, 26));
        heading.setForeground(Theme.INK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Masukkan kredensial untuk membuka terminal.");
        sub.setFont(Theme.text(Font.PLAIN, 13));
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sub);
        form.add(Box.createVerticalStrut(28));

        JLabel userLabel = new JLabel("USERNAME");
        userLabel.setFont(Theme.text(Font.BOLD, 11));
        userLabel.setForeground(Theme.INK_SOFT);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(userLabel);
        form.add(Box.createVerticalStrut(6));

        txtUsername = UiKit.textField("Ketik username");
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(18));

        JLabel passLabel = new JLabel("PASSWORD");
        passLabel.setFont(Theme.text(Font.BOLD, 11));
        passLabel.setForeground(Theme.INK_SOFT);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passLabel);
        form.add(Box.createVerticalStrut(6));

        txtPassword = UiKit.passwordField("Ketik password");
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(8));

        JCheckBox show = UiKit.checkBox("Perlihatkan sandi", false);
        show.setAlignmentX(Component.LEFT_ALIGNMENT);
        show.addActionListener(e -> txtPassword.setEchoChar(show.isSelected() ? (char) 0 : '\u2022'));
        form.add(show);
        form.add(Box.createVerticalStrut(6));

        lblError = new JLabel(" ");
        lblError.setFont(Theme.text(Font.BOLD, 12));
        lblError.setForeground(Theme.DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);
        form.add(Box.createVerticalStrut(10));

        btnLogin = new UiKit.SoftButton("MASUK KE SISTEM", UiKit.Variant.PRIMARY);
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnLogin.setPreferredSize(new Dimension(0, 46));
        btnLogin.setFont(Theme.text(Font.BOLD, 13));
        btnLogin.addActionListener(e -> handleLogin());
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(16));

        JLabel footer = new JLabel("Default root: owner / owner123  •  kasir / kasir123");
        footer.setFont(Theme.mono(Font.PLAIN, 10));
        footer.setForeground(Theme.FAINT);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(footer);

        outer.add(form, new GridBagConstraints());
        return outer;
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Username dan password wajib diisi.");
            return;
        }

        setLoading(true);
        try {
            User user = authService.login(username, password);
            if (user == null) {
                lblError.setText("Username atau password salah.");
                txtPassword.setText("");
                txtPassword.requestFocus();
                return;
            }
            Session.login(user);
            dispose();
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        } catch (SQLException ex) {
            lblError.setText(ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage() : "Tidak bisa terhubung ke database.");
            ex.printStackTrace();
        } finally {
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "MEMVERIFIKASI..." : "MASUK KE SISTEM");
        txtUsername.setEnabled(!loading);
        txtPassword.setEnabled(!loading);
    }
}
