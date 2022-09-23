package github.kasuminova.balloonserver.Servers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class DeleteUpdateRule implements ActionListener {
    private final JList<String> modeList;
    private final List<String> rules;
    private final Container container;

    public DeleteUpdateRule(JList<String> modeList, List<String> rules, Container container) {
        this.modeList = modeList;
        this.rules = rules;
        this.container = container;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<String> selected = modeList.getSelectedValuesList();

        if (!selected.isEmpty()) {
            rules.removeAll(selected);
            modeList.setListData(rules.toArray(new String[0]));
        } else {
            JOptionPane.showMessageDialog(container,
                    "请选择一个规则后再删除.", "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
