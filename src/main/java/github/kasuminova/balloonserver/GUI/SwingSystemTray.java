package github.kasuminova.balloonserver.GUI;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.Configurations.ConfigurationManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;

import static github.kasuminova.balloonserver.BalloonServer.CONFIG;

/**
 * 中文系统托盘弹出菜单不乱码。
 * 网上抄的（）
 * @author Kasumi_Nova
 */
public class SwingSystemTray {
    /**
     * 载入托盘
     * @param frame 主窗口
     * @param isHideOnClose 关闭窗口是否最小化至托盘
     * @param isExitOnClose 关闭窗口是否退出程序
     */
    public static void initSystemTrayAndFrame(JFrame frame, boolean isHideOnClose, boolean isExitOnClose) {
        //如果系统不支持任务栏，则设置为关闭窗口时退出程序
        if (!SystemTray.isSupported() || isExitOnClose) {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            return;
        }
        if (isHideOnClose) {
            frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        } else {
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }
        //使用JDialog 作为JPopupMenu载体
        JDialog dialog = new JDialog();
        //关闭JDialog的装饰器
        dialog.setUndecorated(true);
        //jDialog作为JPopupMenu载体不需要多大的size
        dialog.setSize(1, 1);

        //创建JPopupMenu
        //重写firePopupMenuWillBecomeInvisible
        //消失后将绑定的组件一起消失
        JPopupMenu jPopupMenu = new JPopupMenu() {
            @Override
            public void firePopupMenuWillBecomeInvisible() {
                dialog.setVisible(false);
            }
        };
        jPopupMenu.setSize(100, 30);

        //添加菜单选项
        JMenuItem exit = new JMenuItem("退出程序");
        exit.addActionListener(e -> {
            try {
                ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
                BalloonServer.GLOBAL_LOGGER.info("已保存主程序配置文件.");
            } catch (IOException ex) {
                BalloonServer.GLOBAL_LOGGER.warning("保存主程序配置文件失败！");
            }
            System.exit(0);
        });
        JMenuItem showMainFrame = new JMenuItem("显示窗口");
        showMainFrame.addActionListener(e -> {
            //显示窗口
            frame.setVisible(true);
            frame.toFront();
        });

        jPopupMenu.add(showMainFrame);
        jPopupMenu.add(exit);

        URL resource = SwingSystemTray.class.getResource("/image/icon_16x16.png");
        // 创建托盘图标
        Image image = Toolkit.getDefaultToolkit().createImage(resource);
        // 创建系统托盘图标
        TrayIcon trayIcon = new TrayIcon(image);
        trayIcon.setToolTip("BalloonServer " + BalloonServer.VERSION);
        // 自动调整系统托盘图标大小
        trayIcon.setImageAutoSize(true);

        // 给托盘图标添加鼠标监听
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                //左键点击
                if (e.getButton() == 1) {
                    //显示窗口
                    frame.setVisible(true);
                    frame.toFront();
                } else if (e.getButton() == MouseEvent.BUTTON3 && e.isPopupTrigger()) {
                    // 右键点击弹出JPopupMenu绑定的载体以及JPopupMenu
                    dialog.setLocation(e.getX() + 5, e.getY() - 5 - jPopupMenu.getHeight());
                    // 显示载体
                    dialog.setVisible(true);
                    // 在载体的 0,0 处显示对话框
                    jPopupMenu.show(dialog, 0, 0);
                }
            }
        });
        // 添加托盘图标到系统托盘
        systemTrayAdd(trayIcon);
        // 关闭窗口时显示信息
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                trayIcon.displayMessage("提示","程序已最小化至后台运行。", TrayIcon.MessageType.INFO);
            }
        });
    }

    /**
     * 添加托盘图标到系统托盘中。
     *
     * @param trayIcon 系统托盘图标。
     */
    private static void systemTrayAdd(TrayIcon trayIcon) {
        // 将托盘图标添加到系统的托盘实例中
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException ex) {
            ex.printStackTrace();
        }
    }
}

