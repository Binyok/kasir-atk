package giriatk.view.dialog;

import giriatk.util.Dialogs;
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

public abstract class FormDialog extends JDialog {

    private final JPanel form = new JPanel(new GridBagLayout());
    private final UiKit.SurfacePanel formCard = new UiKit.SurfacePanel(new BorderLayout());
    private final JScrollPane scroll;
    private int row;
    private boolean tersimpan;

    protected FormDialog(Window owner, String title, String subtitle) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title);
        heading.setFont(Theme.display(Font.BOLD, 18));
        heading.setForeground(Theme.INK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(heading);
        if (subtitle != null && !subtitle.isBlank()) {
            JLabel sub = new JLabel(subtitle);
            sub.setFont(Theme.text(Font.PLAIN, 12));
            sub.setForeground(Theme.MUTED);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(Box.createVerticalStrut(3));
            header.add(sub);
        }
        root.add(header, BorderLayout.NORTH);

        formCard.setBorder(new EmptyBorder(18, 18, 18, 18));
        form.setOpaque(false);
        formCard.add(form, BorderLayout.NORTH);

        scroll = new JScrollPane(formCard);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(14, 0, 4, 0));
        center.add(scroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        root.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(root);
        setResizable(false);
    }

    private JComponent buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        UiKit.SoftButton batal = new UiKit.SoftButton("Batal", UiKit.Variant.GHOST);
        batal.addActionListener(e -> dispose());

        UiKit.SoftButton simpan = new UiKit.SoftButton("Simpan Data", UiKit.Variant.PRIMARY);
        simpan.addActionListener(e -> simpan());

        panel.add(batal);
        panel.add(simpan);
        return panel;
    }

    protected void addField(String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.text(Font.BOLD, 12));
        lbl.setForeground(Theme.INK_SOFT);

        GridBagConstraints gcLabel = new GridBagConstraints();
        gcLabel.gridx = 0;
        gcLabel.gridy = row;
        gcLabel.anchor = GridBagConstraints.WEST;
        gcLabel.insets = new Insets(row == 0 ? 0 : 12, 0, 0, 16);
        form.add(lbl, gcLabel);

        GridBagConstraints gcField = new GridBagConstraints();
        gcField.gridx = 1;
        gcField.gridy = row;
        gcField.weightx = 1;
        gcField.fill = GridBagConstraints.HORIZONTAL;
        gcField.insets = new Insets(row == 0 ? 0 : 12, 0, 0, 0);
        form.add(field, gcField);
        row++;
    }

    protected void addFullWidth(JComponent component) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(12, 0, 0, 0);
        form.add(component, gc);
        row++;
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
