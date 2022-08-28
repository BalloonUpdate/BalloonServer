package github.kasuminova.balloonserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

/**
 * 中文系统托盘弹出菜单不乱码。
 */
public class SwingSystemTray {
    public static void initSystemTray(JFrame frame) {
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
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
        exit.addActionListener(e -> System.exit(0));
        JMenuItem showMainFrame = new JMenuItem("显示窗口");
        showMainFrame.addActionListener(e -> {
            //显示窗口
            frame.setVisible(true);
        });

        jPopupMenu.add(showMainFrame);
        jPopupMenu.add(exit);

        URL resource = SwingSystemTray.class.getResource("/image/icon_16x16.png");
        // 创建托盘图标
        Image image = Toolkit.getDefaultToolkit().createImage(resource);
        // 创建系统托盘图标
        TrayIcon trayIcon = new TrayIcon(image);
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
                } else if (e.getButton() == 3 && e.isPopupTrigger()) {
                    // 右键点击弹出JPopupMenu绑定的载体以及JPopupMenu
                    dialog.setLocation(e.getX() + 5, e.getY() - 5 - jPopupMenu.getHeight());
                    // 显示载体
                    dialog.setVisible(true);
                    // 在载体的0,0处显示对话框
                    jPopupMenu.show(dialog, 0, 0);
                }
            }
        });
        // 添加托盘图标到系统托盘
        systemTrayAdd(trayIcon);
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

