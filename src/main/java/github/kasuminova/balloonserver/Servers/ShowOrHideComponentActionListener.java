package github.kasuminova.balloonserver.Servers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ShowOrHideComponentActionListener implements ActionListener {
    private final Component component;
    private final String componentName;
    private final JButton button;

    public ShowOrHideComponentActionListener(Component component, String componentName, JButton button) {
        this.component = component;
        this.componentName = componentName;
        this.button = button;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (component.isVisible()) {
            component.setVisible(false);
            button.setText(String.format("显示%s", componentName));
        } else {
            component.setVisible(true);
            button.setText(String.format("隐藏%s", componentName));
        }
    }
}
