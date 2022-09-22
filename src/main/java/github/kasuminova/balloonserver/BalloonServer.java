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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static final long START = System.currentTimeMillis();
    private static final JFrame PREMAIN_FRAME = new JFrame("加载中");
    private static final JPanel STATUS_PANEL = new JPanel(new BorderLayout());
    private static final JProgressBar PRE_LOAD_PROGRESS_BAR = new JProgressBar();
    public static final ImageIcon ICON = new ImageIcon(Objects.requireNonNull(BalloonServer.class.getResource("/image/icon_128x128.jpg")));
    public static final String VERSION = "1.1.3-BETA";
    //主窗口
    public static final JFrame MAIN_FRAME = new JFrame("BalloonServer " + VERSION);
    //主窗口 Logger
    public static final Logger GLOBAL_LOGGER = Logger.getLogger("MAIN");
    public static final JProgressBar GLOBAL_STATUS_PROGRESSBAR = new JProgressBar(0,1000);
    //全局线程池
    public static final ExecutorService GLOBAL_THREAD_POOL = Executors.newCachedThreadPool();
    private static void init() {
        //大小设置
        MAIN_FRAME.setSize(1400,775);
        MAIN_FRAME.setMinimumSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.8), MAIN_FRAME.getHeight()));
        //主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        //标签页组装
        PRE_LOAD_PROGRESS_BAR.setString("载入主面板...");
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        Thread serverThread = new Thread(() -> {
            LittleServer littleServer = new LittleServer("./littleserver.json");
            tabbedPane.add(littleServer.getPanel(), "LittleServer");
            tabbedPane.add(AboutPanel.createPanel(), "关于本程序");
            mainPanel.add(tabbedPane, BorderLayout.CENTER);
        });
        serverThread.start();

        //状态栏
        STATUS_PANEL.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(4, 4, 4, 4)));
        JLabel threadCount = new JLabel("当前运行的线程数量：0");
        STATUS_PANEL.add(threadCount, BorderLayout.WEST);
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
        STATUS_PANEL.add(memBarBox, BorderLayout.EAST);
        GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
        GLOBAL_STATUS_PROGRESSBAR.setStringPainted(true);
        GLOBAL_STATUS_PROGRESSBAR.setBorder(new EmptyBorder(0, 40, 0, 40));
        GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
        STATUS_PANEL.add(GLOBAL_STATUS_PROGRESSBAR);
        mainPanel.add(STATUS_PANEL, BorderLayout.SOUTH);
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

        GLOBAL_THREAD_POOL.execute(() -> {
            long start = System.currentTimeMillis();
            //托盘窗口
            if (SystemTray.isSupported()) {
                SwingSystemTray.initSystemTray(MAIN_FRAME);
            } else {
                MAIN_FRAME.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
            GLOBAL_LOGGER.info(String.format("任务栏已加载完毕, 耗时 %sms", System.currentTimeMillis() - start));
        });

        try {
            serverThread.join();
        } catch (InterruptedException ignored) {}

        //主窗口
        MAIN_FRAME.add(mainPanel);
        MAIN_FRAME.setIconImage(ICON.getImage());
        MAIN_FRAME.setLocationRelativeTo(null);
        GLOBAL_LOGGER.info(String.format("程序已加载完成, 耗时 %sms", System.currentTimeMillis() - START));

        PREMAIN_FRAME.dispose();
        MAIN_FRAME.setVisible(true);
    }

    /**
     * 程序预加载
     */
    private static void preInit() {
        JPanel panel = new JPanel(new VFlowLayout());
        panel.add(new JLabel(new ImageIcon(ICON.getImage().getScaledInstance(96,96,Image.SCALE_DEFAULT))));
        panel.add(new JLabel("程序启动中，请稍等..."));
        PRE_LOAD_PROGRESS_BAR.setStringPainted(true);
        PRE_LOAD_PROGRESS_BAR.setIndeterminate(true);
        PRE_LOAD_PROGRESS_BAR.setString("...");
        panel.add(PRE_LOAD_PROGRESS_BAR);
        PREMAIN_FRAME.add(panel);
        PREMAIN_FRAME.setSize(240,150);
        PREMAIN_FRAME.setUndecorated(true);
        PREMAIN_FRAME.setLocationRelativeTo(null);
        PREMAIN_FRAME.setVisible(true);

        //运行环境检测
        PRE_LOAD_PROGRESS_BAR.setString("检查运行环境...");
        int version = Integer.parseInt(System.getProperty("java.specification.version"));
        GLOBAL_LOGGER.info("Java 版本为 " + version);
        if (version < 17) {
            int selection = JOptionPane.showConfirmDialog(PREMAIN_FRAME,
                            String.format("""
                                    检测到当前 Java 运行库版本为 JAVA %s
                                    程序要求的运行库版本应为 JAVA 17 及以上, 您要强制使用不兼容的版本启动程序吗?
                                    选择 “是” 启用不安全模式, 选择 “否” 退出程序""", version),
                            "不受支持的 JAVA 版本",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
            if (selection == JOptionPane.YES_OPTION) {
                MAIN_FRAME.setTitle(MAIN_FRAME.getTitle() + " (Unsafe Mode)");
            } else {
                System.exit(1);
            }
        }
    }

    /**
     * 重置主窗口状态栏进度条
     */
    public static void resetStatusProgressBar() {
        GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
        GLOBAL_STATUS_PROGRESSBAR.setIndeterminate(false);
        GLOBAL_STATUS_PROGRESSBAR.setValue(0);
    }

    /**
     * 主程序
     */
    public static void main(String[] args) {
        preInit();
        init();
    }
}
