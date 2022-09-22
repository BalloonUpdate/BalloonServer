package github.kasuminova.balloonserver.GUI.CheckBoxTree;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.GUI.VFlowLayout;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;

public class RuleEditor extends JDialog {
    public static final String VERSION = "1.0-STABLE";

    List<String> result = new ArrayList<>();

    public List<String> getResult() {
        return result;
    }

    public RuleEditor(JSONArray jsonArray, String resDirPath) {
        setTitle("RuleEditor " + VERSION);
        setIconImage(BalloonServer.ICON.getImage());
        JPanel contentPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP, VFlowLayout.MIDDLE, 5, 5, 5, 5, true, true));

        getContentPane().add(contentPanel);
        setSize(750,605);
        setResizable(false);
        setLocationRelativeTo(null);

        contentPanel.add(new JLabel("更新规则编辑器是为不熟悉正则表达式的小白用户准备的，能够兼容大部分情况下的应用场景。"));
        contentPanel.add(new JLabel("如果您对正则表达式稍有理解，请使用“添加更新规则”。"));

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
        JScrollPane scroll = new JScrollPane(tree);

        contentPanel.add(scroll);
        JButton complete = new JButton("完成");
        complete.addActionListener(e -> {
            ArrayList<String> rules = getSelectedFiles(rootNode);

            for (String rule : rules) {
                result.add(rule.replace(resDirPath + "/", ""));
            }
            dispose();
        });
        contentPanel.add(complete);

        JButton cancel = new JButton("取消");
        cancel.addActionListener(e -> dispose());
        contentPanel.add(cancel);
    }

    /**
     * 获取选中的文件，自动获取父节点
     * @param root 节点
     * @return 选中的文件
     */
    private static ArrayList<String> getSelectedFiles(CheckBoxTreeNode root) {
        ArrayList<String> abstractSimpleFileObjects = new ArrayList<>();

        if (root.isRoot() && root.isSelected) {
            abstractSimpleFileObjects.add("**");
            return abstractSimpleFileObjects;
        }

        StringBuilder childPath;

        if (!root.isSelected()) {
            if (!root.isLeaf()) {
                for (int i = 0; i < root.getChildCount(); i++) {
                    abstractSimpleFileObjects.addAll(getSelectedFiles(root.getChildAt(i)));
                }
            }
        } else if (root.getAllowsChildren()) {
            childPath = new StringBuilder();
            for (TreeNode treeNode : root.getPath()) {
                childPath.append(treeNode).append("/");
            }
            abstractSimpleFileObjects.add(childPath + "**");
        } else {
            childPath = new StringBuilder();
            for (TreeNode treeNode : root.getPath()) {
                childPath.append(treeNode).append("/");
            }
            //去除最后一个斜杠
            childPath.delete(childPath.length() - 1, childPath.length());
            abstractSimpleFileObjects.add(childPath.toString());
        }

        return abstractSimpleFileObjects;
    }

    /**
     * 根据传入的 JSONArray 构建 JTree 的子节点
     * @param jsonArray JSONArray
     * @return JTree 子节点
     */
    private static ArrayList<CheckBoxTreeNode> scanDirAndBuildTree(JSONArray jsonArray) {
        ArrayList<CheckBoxTreeNode> treeNodes = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.getString("children") != null) {
                CheckBoxTreeNode directory = new CheckBoxTreeNode(jsonObject.getString("name"));
                directory.setAllowsChildren(true);

                treeNodes.add(directory);

                ArrayList<CheckBoxTreeNode> fileList = scanDirAndBuildTree(jsonObject.getJSONArray("children"));
                for (CheckBoxTreeNode checkBoxTreeNode : fileList) {
                    directory.add(checkBoxTreeNode);
                }
            } else {
                CheckBoxTreeNode file = new CheckBoxTreeNode(jsonObject.getString("name"));
                file.setAllowsChildren(false);
                treeNodes.add(file);
            }
        }

        return treeNodes;
    }
}
