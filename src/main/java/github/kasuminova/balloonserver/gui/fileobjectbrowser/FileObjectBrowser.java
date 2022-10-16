package github.kasuminova.balloonserver.gui.fileobjectbrowser;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;
import github.kasuminova.messages.fileobject.AbstractLiteFileObject;
import github.kasuminova.messages.fileobject.LiteDirectoryObject;

import javax.swing.*;
import java.awt.*;

public class FileObjectBrowser extends JDialog {
    public static final ApplicationVersion VERSION = new ApplicationVersion("1.0.0-BETA");
    public static final String TITLE = "FileObjectBrowser " + VERSION;

    public FileObjectBrowser(LiteDirectoryObject directoryObject) {
        setTitle(TITLE);
        setIconImage(BalloonServer.ICON.getImage());
        setSize(600, 450);
        setResizable(false);
        setLocationRelativeTo(null);

        Container contentPane = getContentPane();
        contentPane.setLayout(new VFlowLayout());

        JList<AbstractLiteFileObject> list = new JList<>();
        JScrollPane listScrollPane = new JScrollPane(list);

        DefaultListModel<AbstractLiteFileObject> listModel = new DefaultListModel<>();
        contentPane.add(listScrollPane);

        list.setCellRenderer(new ImageListCellRenderer());
        listModel.addAll(directoryObject.getChildren());
        list.setModel(listModel);
        list.setVisibleRowCount(20);

        setLocationRelativeTo(null);
    }
}
