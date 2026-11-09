package giriatk.view;

import giriatk.model.User;
import giriatk.service.AuthService;
import giriatk.util.Dialogs;
import giriatk.util.Icons;
import giriatk.util.Session;
import giriatk.util.Theme;
import giriatk.util.UiKit;
import giriatk.view.component.TitleBar;

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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private UiKit.SoftButton btnLogin;
    private JLabel lblError;

    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("GiriATK & Print — Masuk");
        setUndecorated(true);
        setBackground(Theme.INK);
        setSize(1180, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Theme.INK));
        root.add(new TitleBar(this, "GiriATK & Print — Masuk"), BorderLayout.NORTH);

        JPanel split = new JPanel(new BorderLayout());
        split.setOpaque(false);
        split.add(createBrandPanel(), BorderLayout.WEST);
        split.add(createFormPanel(), BorderLayout.CENTER);
        root.add(split, BorderLayout.CENTER);
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

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.INK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0xFAF6EF));
                for (int y = 0; y < getHeight(); y += 11) {
                    for (int x = 0; x < getWidth(); x += 11) {
                        g2.fillOval(x, y, 2, 2);
                    }
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(640, 0));
        panel.setBorder(new EmptyBorder(40, 62, 32, 62));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cropLeft = cropMark();
        JLabel cropRight = cropMark();
        top.add(cropLeft, BorderLayout.WEST);
        top.add(cropRight, BorderLayout.EAST);
        panel.add(top);
        panel.add(Box.createVerticalStrut(6));

        JLabel kicker = new JLabel("TOKO ALAT TULIS · JASA CETAK · SEJAK 2016");
        kicker.setFont(Theme.mono(Font.BOLD, 10));
        kicker.setForeground(Theme.ACCENT);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(kicker);
        panel.add(Box.createVerticalStrut(20));

        JLabel wordmark = new JLabel("<html><font color='#F3ECDC'>GiriATK</font><br>"
                + "<i><font color='#C93A1E'>&amp; Print</font></i></html>");
        wordmark.setFont(Theme.display(Font.BOLD, 72));
        wordmark.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(wordmark);
        panel.add(Box.createVerticalStrut(16));

        JPanel rule = new JPanel();
        rule.setBackground(Theme.ACCENT);
        rule.setPreferredSize(new Dimension(118, 5));
        rule.setMaximumSize(new Dimension(118, 5));
        rule.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(rule);
        panel.add(Box.createVerticalStrut(18));

        JLabel tag = new JLabel("<html><div style='width:430px'>Kasir &amp; manajemen toko ATK + jasa cetak — "
                + "<font color='#FAF6EF'><b>print, fotokopi, laminating, dan jilid</b></font> "
                + "— dalam satu terminal kerja.</div></html>");
        tag.setFont(Theme.text(Font.PLAIN, 17));
        tag.setForeground(Theme.SIDEBAR_TEXT);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(tag);

        panel.add(Box.createVerticalGlue());

        JPanel dash = new JPanel();
        dash.setOpaque(false);
        dash.setPreferredSize(new Dimension(0, 1));
        dash.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        dash.setAlignmentX(Component.LEFT_ALIGNMENT);
        dash.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SIDEBAR_LINE));
        panel.add(Box.createVerticalStrut(14));
        panel.add(dash);
        panel.add(Box.createVerticalStrut(12));

        DateTimeFormatter day = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.of("id", "ID"));
        JLabel telemetry = new JLabel("<html>SISTEM&nbsp;&nbsp;: KASIR v2.6 — TERMINAL 01<br>"
                + "DATABASE : <font color='#7FB98E'>TERHUBUNG</font> · MySQL / giriatk<br>"
                + "HARI INI : " + day.format(LocalDate.now()).toUpperCase() + " · SHIFT PAGI 07.00–15.00</html>");
        telemetry.setFont(Theme.mono(Font.PLAIN, 11));
        telemetry.setForeground(Theme.SIDEBAR_MUTED);
        telemetry.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(telemetry);
        panel.add(Box.createVerticalStrut(10));

        Barcode barcode = new Barcode();
        barcode.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(barcode);
        panel.add(Box.createVerticalStrut(4));

        JLabel barcodeCap = new JLabel("G I R I - A T K - 2 0 2 6");
        barcodeCap.setFont(Theme.mono(Font.PLAIN, 9));
        barcodeCap.setForeground(Theme.SIDEBAR_FAINT);
        barcodeCap.setAlignmentX(Component.LEFT_ALIGNMENT);
        barcodeCap.setPreferredSize(new Dimension(300, 16));
        barcodeCap.setMinimumSize(new Dimension(300, 16));
        barcodeCap.setMaximumSize(new Dimension(300, 16));
        panel.add(barcodeCap);
        panel.add(Box.createVerticalStrut(4));
        return panel;
    }

    private JLabel cropMark() {
        JLabel crop = new JLabel("+");
        crop.setFont(Theme.mono(Font.PLAIN, 20));
        crop.setForeground(Theme.ACCENT);
        return crop;
    }

    private static final class Barcode extends JPanel {
        Barcode() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 36));
            setMinimumSize(new Dimension(300, 36));
            setMaximumSize(new Dimension(300, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0xF3ECDC));
            int[] widths = {2, 3, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 4, 1, 2, 3, 1, 2};
            int x = 0;
            int i = 0;
            while (x < getWidth()) {
                int w = widths[i % widths.length];
                g2.fillRect(x, 0, w, getHeight());
                x += w + 3;
                i++;
            }
            g2.dispose();
        }
    }

    private JPanel createFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.CANVAS);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(28, 25, 23, 14));
                for (int y = 0; y < getHeight(); y += 12) {
                    for (int x = 0; x < getWidth(); x += 12) {
                        g2.fillOval(x, y, 2, 2);
                    }
                }
                g2.dispose();
            }
        };
        outer.setBackground(Theme.CANVAS);

        ShadowCard card = new ShadowCard();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(30, 40, 28, 40));
        card.setPreferredSize(new Dimension(444, 580));
        card.setMaximumSize(new Dimension(444, 620));

        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        int[] rowIdx = {0};
        java.util.function.BiConsumer<java.awt.Component, Integer> add = (comp, gap) -> {
            gc.gridy = rowIdx[0]++;
            gc.insets = new java.awt.Insets(0, 0, gap, 0);
            card.add(comp, gc);
        };

        JPanel meta = new JPanel(new BorderLayout());
        meta.setOpaque(false);
        meta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_STRONG),
                new EmptyBorder(0, 0, 10, 0)));
        JLabel formNo = new JLabel("FORMULIR MASUK");
        formNo.setFont(Theme.mono(Font.BOLD, 10));
        formNo.setForeground(Theme.FAINT);
        JLabel formCode = new JLabel("NO. 01/A");
        formCode.setFont(Theme.mono(Font.BOLD, 10));
        formCode.setForeground(Theme.FAINT);
        meta.add(formNo, BorderLayout.WEST);
        meta.add(formCode, BorderLayout.EAST);
        add.accept(meta, 18);

        JLabel heading = new JLabel("Masuk ke Terminal");
        heading.setFont(Theme.display(Font.BOLD, 28));
        heading.setForeground(Theme.INK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        add.accept(heading, 5);

        JLabel sub = new JLabel("Masuk dengan akun pengelola untuk mengakses seluruh sistem.");
        sub.setFont(Theme.text(Font.PLAIN, 13));
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        add.accept(sub, 22);

        add.accept(fieldLabel("USERNAME"), 7);
        txtUsername = UiKit.textField("Ketik username", true);
        txtUsername.setText("owner");
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        add.accept(txtUsername, 18);

        add.accept(fieldLabel("PASSWORD"), 7);
        txtPassword = UiKit.passwordField("Ketik password");
        txtPassword.setText("owner123");
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pwWrap = new JPanel(new BorderLayout());
        pwWrap.setOpaque(false);
        pwWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        pwWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        pwWrap.add(txtPassword, BorderLayout.CENTER);
        UiKit.IconButton eye = new UiKit.IconButton(Icons.Name.EYE, Theme.MUTED, 17);
        eye.setToolTipText("Perlihatkan sandi");
        eye.addActionListener(e -> txtPassword.setEchoChar(txtPassword.getEchoChar() == 0 ? '\u2022' : (char) 0));
        pwWrap.add(eye, BorderLayout.EAST);
        add.accept(pwWrap, 10);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JCheckBox remember = UiKit.checkBox("Ingat terminal ini", true);
        row.add(remember, BorderLayout.WEST);
        JLabel forgot = new JLabel("Lupa password?");
        forgot.setFont(Theme.text(Font.BOLD, 12));
        forgot.setForeground(Theme.ACCENT);
        forgot.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Dialogs.info(LoginFrame.this, "Hubungi pemilik toko untuk mengatur ulang password akun.");
            }
        });
        row.add(forgot, BorderLayout.EAST);
        add.accept(row, 12);

        lblError = new JLabel(" ");
        lblError.setFont(Theme.text(Font.BOLD, 11));
        lblError.setForeground(Theme.DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        add.accept(lblError, 8);

        btnLogin = new UiKit.SoftButton("Masuk", UiKit.Variant.PRIMARY);
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnLogin.setPreferredSize(new Dimension(0, 50));
        btnLogin.setFont(Theme.text(Font.BOLD, 13));
        btnLogin.addActionListener(e -> handleLogin());
        add.accept(btnLogin, 18);

        JPanel demo = new JPanel(new BorderLayout());
        demo.setOpaque(true);
        demo.setBackground(Theme.ACCENT_SOFT);
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        demo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(Theme.ACCENT, 1.5f, 4f, 3f, false),
                new EmptyBorder(8, 14, 8, 14)));
        JLabel demoText = new JLabel("<html><font color='#7A2413'><b>AKUN PENGELOLA TOKO</b><br>"
                + "Username: <font face='Cascadia Mono, Consolas'>owner</font> &nbsp;·&nbsp; "
                + "Password: <font face='Cascadia Mono, Consolas'>owner123</font></font></html>");
        demoText.setFont(Theme.text(Font.PLAIN, 11));
        demo.add(demoText, BorderLayout.CENTER);
        add.accept(demo, 16);

        JLabel foot = new JLabel("© 2026 Giri ATK & Print · Banyuwangi");
        foot.setFont(Theme.text(Font.PLAIN, 10));
        foot.setForeground(Theme.FAINT);
        foot.setAlignmentX(Component.CENTER_ALIGNMENT);
        foot.setHorizontalAlignment(JLabel.CENTER);
        foot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        add.accept(foot, 0);

        outer.add(card);
        return outer;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.text(Font.BOLD, 10));
        label.setForeground(Theme.INK_SOFT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
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
        btnLogin.setText(loading ? "MEMVERIFIKASI..." : "MASUK");
        txtUsername.setEnabled(!loading);
        txtPassword.setEnabled(!loading);
    }

    private static final class ShadowCard extends JPanel {
        ShadowCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.INK);
            g2.fillRect(6, 6, getWidth() - 6, getHeight() - 6);
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth() - 6, getHeight() - 6);
            g2.setColor(Theme.INK);
            g2.drawRect(0, 0, getWidth() - 7, getHeight() - 7);
            g2.drawRect(1, 1, getWidth() - 9, getHeight() - 9);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
