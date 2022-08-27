package github.kasuminova.balloonserver;

import github.kasuminova.balloonserver.Servers.LittleServer;
import github.kasuminova.balloonserver.Utils.LogManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class BalloonServer {
    static {
        SetupSwing.init();
    }
    public static final String version = "1.0.2-BETA";
    public static JFrame frame = new JFrame("BalloonServer " + version);
    public static JProgressBar statusProgressBar = new JProgressBar();
    public static ChangeListener changeListener;
    //全局 LOGGER
    public static LogManager logger;
    public static void loadGUI(){
        //主窗口
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        frame.setSize(1300,700);

        //状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(4, 4, 4, 4)));

        //线程数监控
        JLabel threadCount = new JLabel("当前运行的线程数量：0");
        statusPanel.add(threadCount, BorderLayout.WEST);
        //内存监控
        Box memBarBox = Box.createHorizontalBox();
        JProgressBar memBar = new JProgressBar();
        memBar.setMaximum(100);
        memBar.setPreferredSize(new Dimension(225,memBar.getHeight()));
        memBar.setStringPainted(true);
        memBarBox.add(new JLabel("内存使用情况："));
        memBarBox.add(memBar);
        statusPanel.add(memBarBox, BorderLayout.EAST);

        statusProgressBar.setStringPainted(true);
        statusProgressBar.setBorder(new EmptyBorder(0, 40, 0, 40));
        statusProgressBar.setVisible(false);
        statusPanel.add(statusProgressBar);

        //定时器
        Timer timer = new Timer(500, e -> {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long memoryUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long memoryTotal = memoryMXBean.getHeapMemoryUsage().getInit();

            threadCount.setText("当前运行的线程数量：" + Thread.activeCount());

            memBar.setValue((int) ((double) memoryUsed * 100/memoryTotal));
            memBar.setString(memoryUsed/(1024 * 1024) + " M / " + memoryTotal/(1024 * 1024) + " M");
        });
        timer.start();

        //标签页组装
        tabbedPane.add(LittleServer.createPanel(), "LittleServer");
        //主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        //窗口
        frame.setMinimumSize(new Dimension((int) (frame.getWidth() * 0.8), frame.getHeight()));
        frame.add(mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        loadGUI();
    }
}
