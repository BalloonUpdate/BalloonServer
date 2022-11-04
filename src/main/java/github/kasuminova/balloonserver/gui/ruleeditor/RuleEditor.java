package github.kasuminova.balloonserver.gui.ruleeditor;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeCellRenderer;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeNode;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeNodeSelectionListener;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.servers.localserver.AddUpdateRule;
import github.kasuminova.balloonserver.servers.localserver.DeleteUpdateRule;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;
import github.kasuminova.balloonserver.utils.GUILogger;

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
    public static final ApplicationVersion VERSION = new ApplicationVersion("2.0.0-BETA");
    public static final String TITLE = "RuleEditor " + VERSION;
    public static final int WINDOW_WIDTH = 750;
    public static final int WINDOW_HEIGHT = 840;
    private final GUILogger logger;

    /**
     * 可复用变量（降低内存使用率）
     */
    private final StringBuilder childPath = new StringBuilder(16);
    public RuleEditor(JSONArray jsonArray, List<String> rules, GUILogger logger) {
        this.logger = logger;

        setTitle(TITLE);
        setIconImage(BalloonServer.ICON.getImage());
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
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

        for (CheckBoxTreeNode checkBoxTreeNode : scanDirAndBuildTree(jsonArray, new JSONObject())) {
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
        ruleList.addMouseListener(new RuleListMouseAdapter(ruleListMenu, ruleList));

        JScrollPane ruleListScrollPane = new JScrollPane(
                ruleList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        ruleListPanel.add(ruleListScrollPane);

        contentPane.add(ruleListPanel);

        JButton complete = new JButton("完成");
        complete.addActionListener(e -> {
            //复制
            List<String> listTmp = new ArrayList<>(rules);

            listTmp.addAll(buildRules(rootNode, childPath));

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
    private static ArrayList<String> buildRules(CheckBoxTreeNode root, StringBuilder childPath) {
        ArrayList<String> rules = new ArrayList<>(0);

        if (root.isRoot() && root.isSelected()) {
            rules.add("**");
            return rules;
        }

        if (!root.isSelected()) {
            if (!root.isLeaf()) {
                for (int i = 0; i < root.getChildCount(); i++) {
                    rules.addAll(buildRules(root.getChildAt(i), childPath));
                }
            }
        } else if (root.getAllowsChildren()) {
            childPath.setLength(0);
            for (int i = 1; i < root.getPath().length; i++) {
                childPath.append(root.getPath()[i].toString().intern()).append("/");
            }

            rules.add(childPath.append("**").insert(0, "@").toString().
                    replace("[", "\\[").
                    replace("]", "\\]").
                    replace(".", "\\."));
        } else {
            childPath.setLength(0);
            for (int i = 1; i < root.getPath().length; i++) {
                childPath.append(root.getPath()[i].toString().intern()).append("/");
            }
            //去除最后一个斜杠
            childPath.setLength(childPath.length() - 1);

            rules.add(childPath.insert(0, "@").toString().
                    replace("[", "\\[").
                    replace("]", "\\]").
                    replace(".", "\\."));
        }

        return rules;
    }

    /**
     * 遍历文件树，将匹配更新规则的对象选中
     */
    private void compareRules(CheckBoxTreeNode root, List<String> rules) {
        for (String rule : rules) {
            if (rule.equals("**")) {
                root.setSelected(true);
                return;
            }
            for (int i = 0; i < root.getChildCount(); i++) {
                compareRule(root.getChildAt(i), rule.replace("@", "").replace("**", "*"));
            }
        }
    }

    private void compareRule(CheckBoxTreeNode node, String rule) {
        childPath.setLength(0);

        if (node.getAllowsChildren()) {
            for (int i = 1; i < node.getPath().length; i++) {
                childPath.append(node.getPath()[i].toString().intern()).append("/");
            }

            if (ReUtil.contains(rule, childPath.toString())) {
                node.setSelected(true);
                if (BalloonServer.CONFIG.isDebugMode()) {
                    logger.debug("ContainRule: {}, Content: {}", rule, childPath.toString());
                }
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

            if (ReUtil.contains(rule, childPath.toString())) {
                node.setSelected(true);
                if (BalloonServer.CONFIG.isDebugMode()) {
                    logger.debug("ContainRule: {}, Content: {}", rule, childPath.toString());
                }
            }
        }
    }

    /**
     * 根据传入的 JSONArray 构建 JTree 的子节点
     *
     * @param jsonArray JSONArray
     * @return JTree 子节点
     */
    private static ArrayList<CheckBoxTreeNode> scanDirAndBuildTree(JSONArray jsonArray, JSONObject jsonObject) {
        ArrayList<CheckBoxTreeNode> treeNodes = new ArrayList<>(8);

        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject.clear();
            jsonObject.putAll(jsonArray.getJSONObject(i));

            if (jsonObject.getString("children") == null) {
                CheckBoxTreeNode file = new CheckBoxTreeNode(jsonObject.getString("name"));
                file.setAllowsChildren(false);
                treeNodes.add(file);
            } else {
                CheckBoxTreeNode directory = new CheckBoxTreeNode(jsonObject.getString("name"));

                for (CheckBoxTreeNode checkBoxTreeNode : scanDirAndBuildTree(jsonObject.getJSONArray("children"), jsonObject)) {
                    directory.add(checkBoxTreeNode);
                }

                treeNodes.add(directory);
            }
        }
        return treeNodes;
    }

    private static class RuleListMouseAdapter extends MouseAdapter {
        private final JPopupMenu ruleListMenu;
        private final JList<String> ruleList;

        private RuleListMouseAdapter(JPopupMenu ruleListMenu, JList<String> ruleList) {
            this.ruleListMenu = ruleListMenu;
            this.ruleList = ruleList;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                ruleListMenu.show(ruleList, e.getX(), e.getY());
            }
        }
    }
}
