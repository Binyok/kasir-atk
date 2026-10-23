package giriatk.view.component;

import giriatk.util.UiKit;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class SegmentedTabs extends JPanel {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final List<UiKit.ToggleButton> buttons = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();

    public SegmentedTabs() {
        setOpaque(false);
        setLayout(new java.awt.BorderLayout(0, 14));
        tabBar.setOpaque(false);
        add(tabBar, java.awt.BorderLayout.NORTH);
        content.setOpaque(false);
        add(content, java.awt.BorderLayout.CENTER);
    }

    public void addTab(String key, String label, JComponent component) {
        UiKit.ToggleButton button = new UiKit.ToggleButton(label, giriatk.util.Theme.ACCENT);
        button.addActionListener(e -> show(key));
        buttons.add(button);
        keys.add(key);
        tabBar.add(button);
        content.add(component, key);
        if (buttons.size() == 1) {
            show(key);
        }
    }

    public void show(String key) {
        cards.show(content, key);
        for (int i = 0; i < keys.size(); i++) {
            buttons.get(i).setActive(keys.get(i).equals(key));
        }
    }
}
