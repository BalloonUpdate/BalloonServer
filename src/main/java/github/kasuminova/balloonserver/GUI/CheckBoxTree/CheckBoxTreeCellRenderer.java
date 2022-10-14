package github.kasuminova.balloonserver.GUI.CheckBoxTree;

import github.kasuminova.balloonserver.Utils.SvgIcons;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;


public class CheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
    protected final JCheckBox check;
    protected final CheckBoxTreeLabel label;

    public CheckBoxTreeCellRenderer() {
        setLayout(null);
        add(check = new JCheckBox());
        add(label = new CheckBoxTreeLabel());
        check.setBackground(UIManager.getColor("Tree.textBackground"));
        label.setForeground(UIManager.getColor("Tree.textForeground"));
    }

    /**
     * 返回的是一个<code>JPanel</code>对象，该对象中包含一个<code>JCheckBox</code>对象
     * 和一个<code>JLabel</code>对象。并且根据每个结点是否被选中来决定<code>JCheckBox</code>
     * 是否被选中。
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        setEnabled(tree.isEnabled());
        check.setSelected(((CheckBoxTreeNode) value).isSelected());
        label.setFont(tree.getFont());
        label.setText(stringValue);
        label.setSelected(selected);
        label.setFocus(hasFocus);
        if (((CheckBoxTreeNode) value).getAllowsChildren()) {
            label.setIcon(SvgIcons.DIR_ICON);
//            label.setIcon(UIManager.getIcon("Tree.closedIcon"));
//        } else if (expanded) {
//            label.setIcon(UIManager.getIcon("Tree.openIcon"));
        } else {
            setFileIcon();
        }

        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dCheck = check.getPreferredSize();
        Dimension dLabel = label.getPreferredSize();
        return new Dimension(dCheck.width + dLabel.width, Math.max(dCheck.height, dLabel.height));
    }

    @Override
    public void doLayout() {
        Dimension dCheck = check.getPreferredSize();
        Dimension dLabel = label.getPreferredSize();
        int yCheck = 0;
        int yLabel = 0;
        if (dCheck.height < dLabel.height)
            yCheck = (dLabel.height - dCheck.height) / 2;
        else
            yLabel = (dCheck.height - dLabel.height) / 2;
        check.setLocation(0, yCheck);
        check.setBounds(0, yCheck, dCheck.width, dCheck.height);
        label.setLocation(dCheck.width, yLabel);
        label.setBounds(dCheck.width, yLabel, dLabel.width, dLabel.height);
    }

    @Override
    public void setBackground(Color color) {
        if (color instanceof ColorUIResource)
            color = null;
        super.setBackground(color);
    }

    public void setFileIcon() {
        String fileName = label.getText();

        if (fileName.endsWith(".class")) {
            label.setIcon(SvgIcons.CLASS_FILE_ICON);
        } else if (fileName.endsWith("doc") || fileName.endsWith("docx")) {
            label.setIcon(SvgIcons.DOC_FILE_ICON);
        } else if (fileName.endsWith("ppt") || fileName.endsWith("pptx")) {
            label.setIcon(SvgIcons.PPT_FILE_ICON);
        } else if (fileName.endsWith("xls") || fileName.endsWith("xlsx")) {
            label.setIcon(SvgIcons.XLS_FILE_ICON);
        } else if (fileName.endsWith("exe")) {
            label.setIcon(SvgIcons.EXE_FILE_ICON);
        } else if (fileName.endsWith("jar")) {
            label.setIcon(SvgIcons.JAR_FILE_ICON);
        } else if (fileName.endsWith("java")) {
            label.setIcon(SvgIcons.JAVA_FILE_ICON);
        } else if (fileName.endsWith("jpg")) {
            label.setIcon(SvgIcons.JPG_FILE_ICON);
        } else if (fileName.endsWith("json")) {
            label.setIcon(SvgIcons.JSON_FILE_ICON);
        } else if (fileName.endsWith("md")) {
            label.setIcon(SvgIcons.MD_FILE_ICON);
        } else if (fileName.endsWith("txt")) {
            label.setIcon(SvgIcons.TXT_FILE_ICON);
        } else if (fileName.endsWith("xml")) {
            label.setIcon(SvgIcons.XML_FILE_ICON);
        } else if (fileName.endsWith("yml")) {
            label.setIcon(SvgIcons.YML_FILE_ICON);
        } else if (fileName.endsWith("zip")) {
            label.setIcon(SvgIcons.ZIP_FILE_ICON);
        } else {
            label.setIcon(SvgIcons.FILE_ICON);
        }
    }
}