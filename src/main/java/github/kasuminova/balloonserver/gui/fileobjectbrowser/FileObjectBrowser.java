package github.kasuminova.balloonserver.gui.fileobjectbrowser;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeCellRenderer;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeNode;
import github.kasuminova.balloonserver.gui.checkboxtree.CheckBoxTreeNodeSelectionListener;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;
import github.kasuminova.balloonserver.utils.fileobject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleFileObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Kasumi_Nova
 */
public class FileObjectBrowser extends JDialog {
    public static final ApplicationVersion VERSION = new ApplicationVersion("1.0.0-BETA");
    public static final String TITLE = "FileObjectBrowser " + VERSION;
    public static final int WINDOW_WIDTH = 750;
    public static final int WINDOW_HEIGHT = 600;

    public FileObjectBrowser(SimpleDirectoryObject directoryObject) {
        setTitle(TITLE);
        setIconImage(BalloonServer.ICON.getImage());
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);

        Container contentPane = getContentPane();
        contentPane.setLayout(new VFlowLayout());

        JPanel treePanel = new JPanel(new VFlowLayout(VFlowLayout.TOP, VFlowLayout.MIDDLE, 5, 5, 5, 5, true, false));
        treePanel.setBorder(new TitledBorder("服务端文件列表"));

        JTree tree = new JTree();
        CheckBoxTreeNode rootNode = new CheckBoxTreeNode("服务端文件夹");
        DefaultTreeModel model = new DefaultTreeModel(rootNode);

        tree.addMouseListener(new CheckBoxTreeNodeSelectionListener());
        tree.setModel(model);
        tree.setCellRenderer(new CheckBoxTreeCellRenderer());

        for (CheckBoxTreeNode checkBoxTreeNode : scanDirAndBuildTree(directoryObject)) {
            rootNode.add(checkBoxTreeNode);
        }

        tree.expandPath(new TreePath(rootNode.getPath()));

        JScrollPane treeScroll = new JScrollPane(tree);
        treePanel.add(treeScroll);

        contentPane.add(treePanel);

        setLocationRelativeTo(null);
    }

    private static ArrayList<CheckBoxTreeNode> scanDirAndBuildTree(SimpleDirectoryObject directoryObject) {
        ArrayList<CheckBoxTreeNode> treeNodes = new ArrayList<>(0);

        ArrayList<AbstractSimpleFileObject> fileObjects = directoryObject.getChildren();
        fileObjects.forEach((obj -> {
            if (obj instanceof SimpleFileObject fileObject) {
                CheckBoxTreeNode file = new CheckBoxTreeNode(fileObject.getName());
                file.setAllowsChildren(false);

                treeNodes.add(file);
            } else if (obj instanceof SimpleDirectoryObject dirObject){
                CheckBoxTreeNode dir = new CheckBoxTreeNode(dirObject.getName());
                dir.setAllowsChildren(true);
                scanDirAndBuildTree(dirObject).forEach(dir::add);

                treeNodes.add(dir);
            }
        }));

        return treeNodes;
    }
}
