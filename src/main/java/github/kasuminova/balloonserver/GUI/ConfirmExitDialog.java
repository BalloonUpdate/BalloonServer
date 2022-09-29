package github.kasuminova.balloonserver.GUI;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.Configurations.BalloonServerConfig;
import github.kasuminova.balloonserver.Configurations.ConfigurationManager;
import github.kasuminova.balloonserver.GUI.LayoutManager.VFlowLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_LOGGER;
import static github.kasuminova.balloonserver.BalloonServer.stopAllServers;

public class ConfirmExitDialog extends JDialog {
    public ConfirmExitDialog(JFrame frame, BalloonServerConfig config) {
        setTitle("提示");
        setIconImage(BalloonServer.ICON.getImage());
        setSize(360,160);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new VFlowLayout());
        contentPane.add(new JLabel("请选择点击关闭按钮时程序的操作："));

        //选择 退出程序 或 最小化任务栏
        ButtonGroup selections = new ButtonGroup();
        JRadioButton miniSizeToTray = new JRadioButton("最小化到任务栏", true);
        JRadioButton exit = new JRadioButton("退出程序");
        selections.add(miniSizeToTray);
        selections.add(exit);
        JPanel radioButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
        radioButtonsPanel.add(miniSizeToTray);
        radioButtonsPanel.add(exit);
        contentPane.add(radioButtonsPanel);

        //始终保存选项
        JCheckBox saveSelection = new JCheckBox("保存选项，下次不再提醒");
        saveSelection.setBorder(new EmptyBorder(0,10,0,0));
        contentPane.add(saveSelection);

        Box buttonBox = new Box(BoxLayout.LINE_AXIS);
        buttonBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        //确定取消按钮
        JButton yes = new JButton("确定");
        yes.addActionListener(e -> {
            //保存配置并退出程序
            if (exit.isSelected()) {
                //如果始终保存选项选中，则写入配置
                if (saveSelection.isSelected()) {
                    config.setCloseOperation(BalloonServerConfig.EXIT_ON_CLOSE.getOperation());
                    try {
                        ConfigurationManager.saveConfigurationToFile(config, "./", "balloonserver");
                    } catch (Exception ex) {
                        GLOBAL_LOGGER.error("主程序配置文件保存失败！", ex);
                    }
                }
                //停止所有正在运行的服务器并保存配置
                stopAllServers(true);
                System.exit(0);
            }
            //保存配置并最小化窗口
            if (miniSizeToTray.isSelected()) {
                frame.setVisible(false);

                //如果始终保存选项选中，则写入配置
                if (saveSelection.isSelected()) {
                    frame.setDefaultCloseOperation(HIDE_ON_CLOSE);
                    config.setCloseOperation(BalloonServerConfig.HIDE_ON_CLOSE.getOperation());
                    try {
                        ConfigurationManager.saveConfigurationToFile(config, "./", "balloonserver");
                    } catch (Exception ex) {
                        GLOBAL_LOGGER.error("主程序配置文件保存失败！", ex);
                    }
                }
            }
            setVisible(false);
        });
        JButton cancel = new JButton("取消");
        cancel.addActionListener(e -> setVisible(false));
        buttonBox.add(cancel);
        buttonBox.add(yes);
        contentPane.add(buttonBox);
    }
}
