package github.kasuminova.balloonserver.servers.localserver;

import github.kasuminova.balloonserver.gui.RuleEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class AddUpdateRule implements ActionListener {
    private final JList<String> modeList;
    private final List<String> rules;
    private final Container container;

    public AddUpdateRule(JList<String> modeList, List<String> rules, Container container) {
        this.modeList = modeList;
        this.rules = rules;
        this.container = container;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String newRule = JOptionPane.showInputDialog(container,
                "请输入更新规则: ", RuleEditor.TITLE,
                JOptionPane.INFORMATION_MESSAGE);
        if (newRule != null && !newRule.isEmpty()) {
            //防止插入相同内容
            if (rules.contains(newRule)) {
                JOptionPane.showMessageDialog(container,
                        "重复的更新规则", RuleEditor.TITLE,
                        JOptionPane.ERROR_MESSAGE);
            } else {
                rules.add(newRule);
                modeList.setListData(rules.toArray(new String[0]));
            }
        }
    }
}
