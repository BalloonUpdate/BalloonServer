package github.kasuminova.balloonserver.GUI;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.GUI.CheckBoxTree.CheckBoxTreeCellRenderer;
import github.kasuminova.balloonserver.GUI.CheckBoxTree.CheckBoxTreeNode;
import github.kasuminova.balloonserver.GUI.CheckBoxTree.CheckBoxTreeNodeSelectionListener;
import github.kasuminova.balloonserver.Servers.AddUpdateRule;
import github.kasuminova.balloonserver.Servers.DeleteUpdateRule;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 可视化更新规则编辑器
 */
public class RuleEditor extends JDialog {
    public static final String VERSION = "1.2-STABLE";
    private final List<String> result = new ArrayList<>();
    private final ArrayList<String> rules = new ArrayList<>();
    public List<String> getResult() {
        return result;
    }

    public RuleEditor(JSONArray jsonArray, String resDirPath) {
        setTitle("RuleEditor " + VERSION);
        setIconImage(BalloonServer.ICON.getImage());
        setSize(800,855);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new VFlowLayout());

        contentPane.add(new JLabel("更新规则编辑器是为不熟悉正则表达式的小白用户准备的，能够兼容大部分情况下的应用场景。"));
        contentPane.add(new JLabel("如果您对正则表达式稍有理解，请使用“添加更新规则”。"));
        contentPane.add(new JLabel("下方为资源文件夹结构列表，如打勾即为添加至规则，未打勾即忽略。"));

        JPanel treePanel = new JPanel(new VFlowLayout(VFlowLayout.TOP, VFlowLayout.MIDDLE, 5, 5, 5, 5, true, false));
        treePanel.setBorder(new TitledBorder("资源文件夹结构表"));
        JTree tree = new JTree();
        CheckBoxTreeNode rootNode = new CheckBoxTreeNode(resDirPath);

        ArrayList<CheckBoxTreeNode> fileList = scanDirAndBuildTree(jsonArray);

        for (CheckBoxTreeNode checkBoxTreeNode : fileList) {
            rootNode.add(checkBoxTreeNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        tree.addMouseListener(new CheckBoxTreeNodeSelectionListener());
        tree.setModel(model);
        tree.setCellRenderer(new CheckBoxTreeCellRenderer());
        JScrollPane treeScroll = new JScrollPane(tree);
        treePanel.add(treeScroll);
        contentPane.add(treePanel);

        contentPane.add(new JLabel("下方为额外更新规则列表，这些内容会与上方规则一同加入服务器规则列表中。"));
        JPanel ruleListPanel = new JPanel(new VFlowLayout());
        ruleListPanel.setBorder(new TitledBorder("额外更新规则"));
        JList<String> ruleList = new JList<>();
        ruleList.setVisibleRowCount(6);
        JPopupMenu ruleListMenu = new JPopupMenu();
        //添加更新规则
        JMenuItem addRule = new JMenuItem("添加更新规则");
        addRule.addActionListener(new AddUpdateRule(ruleList,rules,contentPane));
        ruleListMenu.add(addRule);
        //删除更新规则
        JMenuItem removeRule = new JMenuItem("删除选定的规则");
        removeRule.addActionListener(new DeleteUpdateRule(ruleList,rules,contentPane));
        ruleListMenu.add(removeRule);
        //鼠标监听
        ruleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()){
                    ruleListMenu.show(ruleList,e.getX(),e.getY());
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
            rules.addAll(getSelectedFiles(rootNode));

            for (String rule : rules) {
                result.add(rule.replace(resDirPath + "/", ""));
            }
            dispose();
        });
        contentPane.add(complete);

        JButton cancel = new JButton("取消");
        cancel.addActionListener(e -> dispose());
        contentPane.add(cancel);
    }

    /**
     * 可复用变量（降低内存使用率）
     */
    private static final StringBuilder childPath = new StringBuilder();
    /**
     * 获取选中的文件，自动获取父节点
     * @param root 节点
     * @return 选中的文件
     */
    private static ArrayList<String> getSelectedFiles(CheckBoxTreeNode root) {
        ArrayList<String> abstractSimpleFileObjects = new ArrayList<>();

        if (root.isRoot() && root.isSelected()) {
            abstractSimpleFileObjects.add("**");
            return abstractSimpleFileObjects;
        }

        if (!root.isSelected()) {
            if (!root.isLeaf()) {
                for (int i = 0; i < root.getChildCount(); i++) {
                    abstractSimpleFileObjects.addAll(getSelectedFiles(root.getChildAt(i)));
                }
            }
        } else if (root.getAllowsChildren()) {
            childPath.setLength(0);
            for (TreeNode treeNode : root.getPath()) {
                childPath.append(treeNode.toString().intern()).append("/");
            }
            abstractSimpleFileObjects.add(childPath + "**");
        } else {
            childPath.setLength(0);
            for (TreeNode treeNode : root.getPath()) {
                childPath.append(treeNode.toString().intern()).append("/");
            }
            //去除最后一个斜杠
            childPath.delete(childPath.length() - 1, childPath.length());
            abstractSimpleFileObjects.add(childPath.toString());
        }

        return abstractSimpleFileObjects;
    }

    private static final JSONObject jsonObject = new JSONObject();
    /**
     * 根据传入的 JSONArray 构建 JTree 的子节点
     * @param jsonArray JSONArray
     * @return JTree 子节点
     */
    private static ArrayList<CheckBoxTreeNode> scanDirAndBuildTree(JSONArray jsonArray) {
        ArrayList<CheckBoxTreeNode> treeNodes = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject.clear();
            jsonObject.putAll(jsonArray.getJSONObject(i));

            if (jsonObject.getString("children") != null) {
                CheckBoxTreeNode directory = new CheckBoxTreeNode(jsonObject.getString("name"));

                for (CheckBoxTreeNode checkBoxTreeNode : scanDirAndBuildTree(jsonObject.getJSONArray("children"))) {
                    directory.add(checkBoxTreeNode);
                }

                treeNodes.add(directory);
            } else {
                CheckBoxTreeNode file = new CheckBoxTreeNode(jsonObject.getString("name"));
                file.setAllowsChildren(false);
                treeNodes.add(file);
            }
        }

        return treeNodes;
    }
}
