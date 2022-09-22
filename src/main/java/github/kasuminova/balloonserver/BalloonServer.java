package github.kasuminova.balloonserver;

import github.kasuminova.balloonserver.GUI.AboutPanel;
import github.kasuminova.balloonserver.GUI.VFlowLayout;
import github.kasuminova.balloonserver.Servers.LittleServer;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 主 窗口/程序。
 * <p>
 *     TODO:计划添加多实例化
 * </p>
 */
public class BalloonServer {
    static {
        //设置全局主题，字体等
        SetupSwing.init();
    }
    public static final ImageIcon ICON = new ImageIcon(Objects.requireNonNull(BalloonServer.class.getResource("/image/icon_128x128.jpg")));
    public static final String VERSION = "1.1.3-BETA";
    //主窗口
    public static final JFrame frame = new JFrame("BalloonServer " + VERSION);
    //主窗口 Logger
    public static final Logger logger = Logger.getLogger("MAIN");
    public static final JProgressBar statusProgressBar = new JProgressBar(0,1000);
    private static final JFrame premainFrame = new JFrame("加载中");
    private static final JPanel statusPanel = new JPanel(new BorderLayout());
    private static final JProgressBar preLoadProgressBar = new JProgressBar();
    private static final long start = System.currentTimeMillis();
    private static void init() {
        //大小设置
        frame.setSize(1400,775);
        frame.setMinimumSize(new Dimension((int) (frame.getWidth() * 0.8), frame.getHeight()));
        //主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        //标签页组装
        preLoadProgressBar.setString("载入主面板...");
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        LittleServer littleServer = new LittleServer("./littleserver.json");
        tabbedPane.add(littleServer.getPanel(), "LittleServer");
        tabbedPane.add(AboutPanel.createPanel(), "关于本程序");
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        new Thread(() -> {
            //状态栏
            statusPanel.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(4, 4, 4, 4)));
            JLabel threadCount = new JLabel("当前运行的线程数量：0");
            statusPanel.add(threadCount, BorderLayout.WEST);
            //线程数监控 + 内存监控
            Box memBarBox = Box.createHorizontalBox();
            JProgressBar memBar = new JProgressBar(0,200);
            memBar.setPreferredSize(new Dimension(225,memBar.getHeight()));
            memBar.setBorder(new EmptyBorder(0,0,0,5));
            memBar.setStringPainted(true);
            memBarBox.add(new JLabel("内存使用情况："));
            memBarBox.add(memBar);
            //内存清理
            JButton GC = new JButton("清理");
            GC.setPreferredSize(new Dimension(65,22));
            GC.addActionListener(e -> System.gc());
            memBarBox.add(GC);
            statusPanel.add(memBarBox, BorderLayout.EAST);
            statusProgressBar.setVisible(false);
            statusProgressBar.setStringPainted(true);
            statusProgressBar.setBorder(new EmptyBorder(0, 40, 0, 40));
            statusProgressBar.setVisible(false);
            statusPanel.add(statusProgressBar);
            mainPanel.add(statusPanel, BorderLayout.SOUTH);
            //定时器, 更新内存和线程信息
            Timer timer = new Timer(500, e -> {
                MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                long memoryUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
                long memoryTotal = memoryMXBean.getHeapMemoryUsage().getInit();

                threadCount.setText(String.format("当前运行的线程数量：%s", Thread.activeCount()));

                memBar.setValue((int) ((double) memoryUsed * 200 / memoryTotal));
                memBar.setString(memoryUsed/(1024 * 1024) + " M / " + memoryTotal/(1024 * 1024) + " M");
            });
            timer.start();
            //托盘窗口
            if (SystemTray.isSupported()) {
                SwingSystemTray.initSystemTray(frame);
            } else {
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        }).start();
        //主窗口
        frame.add(mainPanel);
        frame.setIconImage(ICON.getImage());
        frame.setLocationRelativeTo(null);

        premainFrame.dispose();

        frame.setVisible(true);
        logger.info(String.format("程序已启动, 耗时 %sms", System.currentTimeMillis() - start));
    }

    /**
     * 程序预加载
     */
    private static void preInit() {
        JPanel panel = new JPanel(new VFlowLayout());
        panel.add(new JLabel(new ImageIcon(ICON.getImage().getScaledInstance(96,96,Image.SCALE_DEFAULT))));
        panel.add(new JLabel("程序启动中，请稍等..."));
        preLoadProgressBar.setStringPainted(true);
        preLoadProgressBar.setIndeterminate(true);
        preLoadProgressBar.setString("预加载...");
        panel.add(preLoadProgressBar);
        premainFrame.add(panel);
        premainFrame.setSize(240,150);
        premainFrame.setUndecorated(true);
        premainFrame.setLocationRelativeTo(null);
        premainFrame.setVisible(true);

        //运行环境检测
        preLoadProgressBar.setString("检查运行环境...");
        int version = Integer.parseInt(System.getProperty("java.specification.version"));
        logger.info("Java 版本为 " + version);
        if (version < 17) {
            int selection = JOptionPane.showConfirmDialog(premainFrame,
                            String.format("""
                                    检测到当前 Java 运行库版本为 JAVA %s
                                    程序要求的运行库版本应为 JAVA 17 及以上, 您要强制使用不兼容的版本启动程序吗?
                                    选择 “是” 启用不安全模式, 选择 “否” 退出程序""", version),
                            "不受支持的 JAVA 版本",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
            if (selection == JOptionPane.YES_OPTION) {
                frame.setTitle(frame.getTitle() + " (Unsafe Mode)");
            } else {
                System.exit(0);
            }
        }
    }

    /**
     * 重置主窗口状态栏进度条
     */
    public static void resetStatusProgressBar() {
        statusProgressBar.setVisible(false);
        statusProgressBar.setIndeterminate(false);
        statusProgressBar.setValue(0);
    }

    /**
     * 主程序
     */
    public static void main(String[] args) {
        preInit();
        init();
    }
}
