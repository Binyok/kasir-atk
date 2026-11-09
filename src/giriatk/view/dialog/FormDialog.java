package giriatk.view.dialog;

import giriatk.util.Dialogs;
import giriatk.util.Icons;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Locale;

public abstract class FormDialog extends JDialog {

    private final JPanel form = new JPanel(new GridBagLayout());
    private final JPanel formCard = new JPanel(new BorderLayout());
    private final JScrollPane scroll;
    private int cell;
    private boolean tersimpan;

    protected FormDialog(Window owner, String title, String subtitle) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Theme.INK));

        root.add(buildHeader(title), BorderLayout.NORTH);

        form.setOpaque(false);
        form.setBorder(new EmptyBorder(20, 26, 4, 26));
        formCard.setOpaque(false);
        formCard.add(form, BorderLayout.NORTH);

        scroll = new JScrollPane(formCard);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        root.add(buildButtons(), BorderLayout.SOUTH);
        setContentPane(root);
        setResizable(false);
    }

    private JComponent buildHeader(String title) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.INK),
                new EmptyBorder(20, 26, 16, 20)));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.display(Font.BOLD, 24));
        heading.setForeground(Theme.INK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(heading);

        JLabel code = new JLabel("FORMULIR DATA · FRM/01");
        code.setFont(Theme.mono(Font.BOLD, 9));
        code.setForeground(Theme.FAINT);
        code.setBorder(new EmptyBorder(4, 0, 0, 0));
        code.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(code);
        header.add(texts, BorderLayout.CENTER);

        UiKit.IconButton close = new UiKit.IconButton(Icons.Name.CLOSE, Theme.INK, 14, true);
        close.addActionListener(e -> dispose());
        header.add(close, BorderLayout.EAST);
        return header;
    }

    private JComponent buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_STRONG),
                new EmptyBorder(16, 26, 20, 26)));

        UiKit.SoftButton batal = new UiKit.SoftButton("Batal", UiKit.Variant.GHOST);
        batal.addActionListener(e -> dispose());

        UiKit.SoftButton simpan = new UiKit.SoftButton("Simpan Data", UiKit.Variant.PRIMARY);
        simpan.addActionListener(e -> simpan());

        panel.add(batal);
        panel.add(simpan);
        return panel;
    }

    protected void addField(String label, JComponent field) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(label.toUpperCase(Locale.of("id", "ID")));
        lbl.setFont(Theme.text(Font.BOLD, 10));
        lbl.setForeground(Theme.INK_SOFT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(lbl);
        block.add(Box.createVerticalStrut(7));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(field);

        int col = cell % 2;
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = col;
        gc.gridy = cell / 2;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(cell < 2 ? 0 : 15, col == 0 ? 0 : 9, 0, col == 0 ? 9 : 0);
        form.add(block, gc);
        cell++;
    }

    protected void addFullWidth(JComponent component) {
        if (cell % 2 != 0) {
            cell++;
        }
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = cell / 2;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(15, 0, 0, 0);
        form.add(component, gc);
        cell += 2;
    }

    protected void batasiTinggi() {
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        int maxTinggi = Math.max(360, layar.height - 140);
        Dimension isi = formCard.getPreferredSize();
        if (isi.height > maxTinggi) {
            scroll.setPreferredSize(new Dimension(isi.width, maxTinggi));
        }
    }

    protected JComponent fieldWidth(JComponent field, int width) {
        field.setPreferredSize(new Dimension(width, field.getPreferredSize().height));
        return field;
    }

    @Override
    public void pack() {
        super.pack();
        if (getWidth() < 660) {
            setSize(660, getHeight());
        }
        setLocationRelativeTo(getOwner());
    }

    protected void error(String message) {
        Dialogs.error(this, message);
    }

    protected void sukses() {
        tersimpan = true;
        dispose();
    }

    public boolean isTersimpan() {
        return tersimpan;
    }

    protected abstract void simpan();
}
