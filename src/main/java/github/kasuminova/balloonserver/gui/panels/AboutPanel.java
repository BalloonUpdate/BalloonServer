package github.kasuminova.balloonserver.gui.panels;

import cn.hutool.core.img.ImgUtil;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.utils.MiscUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author Kasumi_Nova
 */
public class AboutPanel {
    private static final int ABOUT_BUTTON_WIDTH = 170;
    private static final int ABOUT_BUTTON_HEIGHT = 30;
    private static final float TITLE_FONT_SIZE = 36F;
    private static final float LICENSE_LABEL_FONT_SIZE = 18F;

    public static JPanel createPanel() {
        //主面板
        JPanel aboutPanel = new JPanel(new BorderLayout());
        Box descBox = Box.createVerticalBox();
        //标题容器
        Box titleBox = Box.createHorizontalBox();
        titleBox.setBorder(new EmptyBorder(10,0,0,0));
        //LOGO, 并缩放图标
        titleBox.add(new JLabel(new ImageIcon(ImgUtil.scale(BalloonServer.ICON.getImage(), 80F / BalloonServer.ICON.getIconWidth()))));
        //标题
        JLabel title = new JLabel("BalloonServer " + BalloonServer.VERSION);
        title.setBorder(new EmptyBorder(0,10,0,0));
        //设置字体
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));
        titleBox.add(title);
        //描述
        JPanel descPanel = new JPanel(new VFlowLayout(0, VFlowLayout.MIDDLE, 5, 5, 5, 5, false, false));
        descPanel.setBorder(new EmptyBorder(10,0,0,0));
        descPanel.add(new JLabel("BalloonServer 是 LittleServer 的衍生图形化版本, 基于 Netty-IO 的增强实现.", JLabel.CENTER));
        descPanel.add(new JLabel("提示: BalloonServer 内嵌了可视化更新规则编辑器, 你可以通过右键更新模式列表打开.", JLabel.CENTER));
        descPanel.add(new JLabel("提示: BalloonServer 支持启动多个服务端, 你可以使用窗口左上角菜单来管理多个实例.", JLabel.CENTER));
        //链接
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10,5));
        //仓库链接
        JButton openProjectLink = new JButton("点击打开仓库链接");
        openProjectLink.addActionListener(e -> MiscUtils.openLinkInBrowser("https://github.com/BalloonUpdate/BalloonServer"));
        openProjectLink.setPreferredSize(new Dimension(ABOUT_BUTTON_WIDTH, ABOUT_BUTTON_HEIGHT));
        linkPanel.add(openProjectLink);
        //项目链接
        JButton openOrganizationLink = new JButton("点击打开项目链接");
        openOrganizationLink.addActionListener(e -> MiscUtils.openLinkInBrowser("https://github.com/BalloonUpdate"));
        openOrganizationLink.setPreferredSize(new Dimension(ABOUT_BUTTON_WIDTH, ABOUT_BUTTON_HEIGHT));
        linkPanel.add(openOrganizationLink);
        //Issues 链接
        JButton openIssuesLink = new JButton("戳我提交 Issue!");
        openIssuesLink.addActionListener(e -> MiscUtils.openLinkInBrowser("https://github.com/BalloonUpdate/BalloonServer/issues/new"));
        openIssuesLink.setPreferredSize(new Dimension(ABOUT_BUTTON_WIDTH, ABOUT_BUTTON_HEIGHT));
        linkPanel.add(openIssuesLink);
        descPanel.add(linkPanel);

        descPanel.add(new JLabel("BalloonServer 的诞生离不开这些贡献: ", JLabel.CENTER));
        descPanel.add(new JLabel("Netty 为 BalloonServer 提供了高性能的并发网络框架；", JLabel.CENTER));
        descPanel.add(new JLabel("Alibaba FastJson2 为 BalloonServer 提供了高性能的 JSON 解析功能；", JLabel.CENTER));
        descPanel.add(new JLabel("FlatLaf, FlatLaf-Extra 为 BalloonServer 提供了一套完美的用户界面体验；", JLabel.CENTER));
        descPanel.add(new JLabel("Hutools 为 BalloonServer 提供了一系列的实用工具；实现了实时文件监听器的功能；", JLabel.CENTER));
        descPanel.add(new JLabel("以及任何积极使用该软件和为此软件出谋划策的用户和开发者们~", JLabel.CENTER));

        //协议
        JLabel licenseLabel = new JLabel("本软件使用 AGPLv3 协议.", JLabel.RIGHT);
        licenseLabel.setFont(licenseLabel.getFont().deriveFont(LICENSE_LABEL_FONT_SIZE));
        licenseLabel.setBorder(new EmptyBorder(0,0,10,10));

        descBox.add(titleBox);
        descBox.add(descPanel);
        aboutPanel.add(descBox);
        aboutPanel.add(licenseLabel, BorderLayout.SOUTH);
        return aboutPanel;
    }
}
