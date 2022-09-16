package github.kasuminova.balloonserver.GUI;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatArcDarkContrastIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkContrastIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme;
import github.kasuminova.balloonserver.BalloonServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static github.kasuminova.balloonserver.BalloonServer.frame;

public class AboutPanel {
    static final int globalButtonWidth = 165;
    public static JPanel createPanel() {
        //主面板
        JPanel aboutPanel = new JPanel(new BorderLayout());
        Box descBox = Box.createVerticalBox();
        //标题容器
        Box titleBox = Box.createHorizontalBox();
        titleBox.setBorder(new EmptyBorder(10,0,0,0));
        //LOGO, 并缩放图标
        titleBox.add(new JLabel(new ImageIcon(BalloonServer.image.getImage().getScaledInstance(64,64, Image.SCALE_DEFAULT))));
        //标题
        JLabel title = new JLabel("BalloonServer " + BalloonServer.version);
        title.setBorder(new EmptyBorder(0,30,0,0));
        //设置字体
        try {
            InputStream ttfFile = AboutPanel.class.getResourceAsStream("/font/Saira-Medium.ttf");
            if (ttfFile != null) {
                title.setFont(Font.createFont(Font.TRUETYPE_FONT, ttfFile).deriveFont(35f));
            }
        } catch (Exception e) {
            title.setFont(title.getFont().deriveFont(35f));
        }
        titleBox.add(title);
        //描述
        JPanel descPanel = new JPanel(new VFlowLayout(0, VFlowLayout.MIDDLE, 5, 5, 5, 5, false, false));
        descPanel.setBorder(new EmptyBorder(10,0,0,0));
        descPanel.add(new JLabel("BalloonServer 是 LittleServer 的完全图形化版本, 基于 Netty-IO 的增强实现.", JLabel.CENTER));
        descPanel.add(new JLabel("提示：关闭程序窗口并不会关闭程序, 而是会最小化到托盘.", JLabel.CENTER));
        //链接
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10,5));
        //仓库链接
        JButton openProjectLink = new JButton("点击打开仓库链接");
        openProjectLink.addActionListener(e -> {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create("https://github.com/BalloonUpdate/BalloonServer"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        openProjectLink.setPreferredSize(new Dimension(globalButtonWidth,30));
        linkPanel.add(openProjectLink);
        //项目链接
        JButton openOrganizationLink = new JButton("点击打开项目链接");
        openOrganizationLink.addActionListener(e -> {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create("https://github.com/BalloonUpdate"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        openOrganizationLink.setPreferredSize(new Dimension(globalButtonWidth,30));
        linkPanel.add(openOrganizationLink);
        //Issues 链接
        JButton openIssuesLink = new JButton("戳我提交 Issue!");
        openIssuesLink.addActionListener(e -> {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create("https://github.com/BalloonUpdate/BalloonServer/issues/new"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        openIssuesLink.setPreferredSize(new Dimension(globalButtonWidth,30));
        linkPanel.add(openIssuesLink);
        descPanel.add(linkPanel);
        //协议
        JLabel licenseLabel = new JLabel("本软件使用 AGPLv3 协议.", JLabel.RIGHT);
        licenseLabel.setFont(licenseLabel.getFont().deriveFont(18f));
        licenseLabel.setBorder(new EmptyBorder(0,0,10,10));
        //主题切换
        List<String> themes = new ArrayList<>();
        themes.add("FlatLaf Light");
        themes.add("FlatLaf Dark");
        themes.add("FlatLaf IntelIJ");
        themes.add("FlatLaf Dracula");
        themes.add("Atom One Dark");
        themes.add("Atom One Dark Contrast");
        themes.add("Arc Dark Contrast");

        JList<String> themeList = new JList<>();
        themeList.setListData(themes.toArray(new String[0]));
        themeList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        themeList.setSelectedIndex(5);
        themeList.setBorder(new TitledBorder("选择界面主题"));
        themeList.addListSelectionListener(e -> {
            try {
                switch (themeList.getSelectedValue()) {
                    case "FlatLaf Light" -> UIManager.setLookAndFeel(new FlatLightLaf());
                    case "FlatLaf Dark" -> UIManager.setLookAndFeel(new FlatDarkLaf());
                    case "FlatLaf IntelIJ" -> UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    case "FlatLaf Dracula" -> UIManager.setLookAndFeel(new FlatDarculaLaf());
                    case "Atom One Dark" -> UIManager.setLookAndFeel(new FlatAtomOneDarkIJTheme());
                    case "Atom One Dark Contrast" -> UIManager.setLookAndFeel(new FlatAtomOneDarkContrastIJTheme());
                    case "Arc Dark Contrast" -> UIManager.setLookAndFeel(new FlatArcDarkContrastIJTheme());
                }
                SwingUtilities.updateComponentTreeUI(frame);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        descBox.add(titleBox);
        descBox.add(descPanel);
        aboutPanel.add(descBox);
        aboutPanel.add(licenseLabel, BorderLayout.SOUTH);
        aboutPanel.add(themeList, BorderLayout.WEST);
        return aboutPanel;
    }
}
