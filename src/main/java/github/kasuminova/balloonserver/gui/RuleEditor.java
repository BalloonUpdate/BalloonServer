package github.kasuminova.balloonserver.gui;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeCellRenderer;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeNode;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeNodeSelectionListener;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.servers.AddUpdateRule;
import github.kasuminova.balloonserver.servers.DeleteUpdateRule;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static github.kasuminova.balloonserver.utils.SvgIcons.PLUS_ICON;
import static github.kasuminova.balloonserver.utils.SvgIcons.REMOVE_ICON;

/**
 * 可视化更新规则编辑器
 */
public class RuleEditor extends JDialog {
    public static final ApplicationVersion VERSION = new ApplicationVersion("1.5.0-STABLE");
    public static final String TITLE = "RuleEditor " + VERSION;
    /**
     * 可复用变量（降低内存使用率）
     */
    private static final StringBuilder childPath = new StringBuilder();
    private static final JSONObject jsonObject = new JSONObject();

    public RuleEditor(JSONArray jsonArray, List<String> rules) {
        setTitle(TITLE);
        setIconImage(BalloonServer.ICON.getImage());
        setSize(750, 840);
        setResizable(false);
        setLocationRelativeTo(null);

        Container contentPane = getContentPane();
        contentPane.setLayout(new VFlowLayout());

        contentPane.add(new JLabel("更新规则编辑器是为不熟悉正则表达式的小白用户准备的, 能够兼容大部分情况下的应用场景。"));
        contentPane.add(new JLabel("如果您对正则表达式稍有理解, 请使用 “添加更新规则”。"));
        contentPane.add(new JLabel("下方为资源文件夹结构列表, 如打勾即为添加至规则, 未打勾即忽略。"));

        JPanel treePanel = new JPanel(new VFlowLayout(VFlowLayout.TOP, VFlowLayout.MIDDLE, 5, 5, 5, 5, true, false));
        treePanel.setBorder(new TitledBorder("资源文件夹结构表"));
        JTree tree = new JTree();
        CheckBoxTreeNode rootNode = new CheckBoxTreeNode("res");

        for (CheckBoxTreeNode checkBoxTreeNode : scanDirAndBuildTree(jsonArray)) {
            rootNode.add(checkBoxTreeNode);
        }

        compareRules(rootNode, rules);

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        tree.addMouseListener(new CheckBoxTreeNodeSelectionListener());
        tree.setModel(model);
        tree.setCellRenderer(new CheckBoxTreeCellRenderer());
        JScrollPane treeScroll = new JScrollPane(tree);
        treePanel.add(treeScroll);
        contentPane.add(treePanel);

        contentPane.add(new JLabel("下方为额外更新规则列表, 这些内容会与上方规则一同加入服务器规则列表中。"));
        JPanel ruleListPanel = new JPanel(new VFlowLayout());
        ruleListPanel.setBorder(new TitledBorder("额外更新规则"));
        JList<String> ruleList = new JList<>();
        ruleList.setVisibleRowCount(6);
        JPopupMenu ruleListMenu = new JPopupMenu();
        //添加更新规则
        JMenuItem addRule = new JMenuItem("添加更新规则");
        addRule.addActionListener(new AddUpdateRule(ruleList, rules, contentPane));
        addRule.setIcon(PLUS_ICON);
        ruleListMenu.add(addRule);
        //删除更新规则
        JMenuItem removeRule = new JMenuItem("删除选定的规则");
        removeRule.addActionListener(new DeleteUpdateRule(ruleList, rules, contentPane));
        removeRule.setIcon(REMOVE_ICON);
        ruleListMenu.add(removeRule);
        //鼠标监听
        ruleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    ruleListMenu.show(ruleList, e.getX(), e.getY());
                }
            }
        });

        JScrollPane ruleListScrollPane = new JScrollPane(
                ruleList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        ruleListPanel.add(ruleListScrollPane);

        contentPane.add(ruleListPanel);

        JButton complete = new JButton("完成");
        complete.addActionListener(e -> {
            //构建规则
            rules.addAll(buildRules(rootNode));

            //复制
            List<String> listTmp = new ArrayList<>(rules);

            rules.clear();
            //添加去重后的 List
            rules.addAll(listTmp.stream().distinct().toList());
            ruleList.setListData(rules.toArray(new String[0]));
            dispose();
        });
        contentPane.add(complete);

        JButton cancel = new JButton("取消");
        cancel.addActionListener(e -> dispose());
        contentPane.add(cancel);
    }

    /**
     * 获取选中的文件，自动获取父节点
     *
     * @param root 节点
     * @return 选中的文件
     */
    private static ArrayList<String> buildRules(CheckBoxTreeNode root) {
        ArrayList<String> rules = new ArrayList<>();

        if (root.isRoot() && root.isSelected()) {
            rules.add("**");
            return rules;
        }

        if (!root.isSelected()) {
            if (!root.isLeaf()) {
                for (int i = 0; i < root.getChildCount(); i++) {
                    rules.addAll(buildRules(root.getChildAt(i)));
                }
            }
        } else if (root.getAllowsChildren()) {
            childPath.setLength(0);
            for (int i = 1; i < root.getPath().length; i++) {
                childPath.append(root.getPath()[i].toString().intern()).append("/");
            }
            rules.add(childPath + "**");
        } else {
            childPath.setLength(0);
            for (int i = 1; i < root.getPath().length; i++) {
                childPath.append(root.getPath()[i].toString().intern()).append("/");
            }
            //去除最后一个斜杠
            childPath.setLength(childPath.length() - 1);
            rules.add(childPath.toString());
        }

        return rules;
    }

    /**
     * 遍历文件树，将匹配更新规则的对象选中
     */
    private static void compareRules(CheckBoxTreeNode root, List<String> rules) {
        for (String rule : rules) {
            if (rule.equals("**")) {
                root.setSelected(true);
                return;
            }
            for (int i = 0; i < root.getChildCount(); i++) {
                compareRule(root.getChildAt(i), rule);
            }
        }
    }

    private static void compareRule(CheckBoxTreeNode node, String rule) {
        childPath.setLength(0);

        if (node.getAllowsChildren()) {
            for (int i = 1; i < node.getPath().length; i++) {
                childPath.append(node.getPath()[i].toString().intern()).append("/");
            }
            childPath.append("**");
            if (childPath.toString().equals(rule)) {
                node.setSelected(true);
                return;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                compareRule(node.getChildAt(i), rule);
            }
        } else {
            for (int i = 1; i < node.getPath().length; i++) {
                childPath.append(node.getPath()[i].toString().intern()).append("/");
            }
            //去除最后一个斜杠
            childPath.delete(childPath.length() - 1, childPath.length());
            if (childPath.toString().equals(rule)) {
                node.setSelected(true);
            }
        }
    }

    /**
     * 根据传入的 JSONArray 构建 JTree 的子节点
     *
     * @param jsonArray JSONArray
     * @return JTree 子节点
     */
    private static ArrayList<CheckBoxTreeNode> scanDirAndBuildTree(JSONArray jsonArray) {
        ArrayList<CheckBoxTreeNode> treeNodes = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject.clear();
            jsonObject.putAll(jsonArray.getJSONObject(i));

            if (jsonObject.getString("children") == null) {
                CheckBoxTreeNode file = new CheckBoxTreeNode(jsonObject.getString("name"));
                file.setAllowsChildren(false);
                treeNodes.add(file);
            } else {
                CheckBoxTreeNode directory = new CheckBoxTreeNode(jsonObject.getString("name"));

                for (CheckBoxTreeNode checkBoxTreeNode : scanDirAndBuildTree(jsonObject.getJSONArray("children"))) {
                    directory.add(checkBoxTreeNode);
                }

                treeNodes.add(directory);
            }
        }
        return treeNodes;
    }
}
