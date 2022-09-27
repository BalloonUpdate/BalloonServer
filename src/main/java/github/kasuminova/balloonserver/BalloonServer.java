package github.kasuminova.balloonserver;

import github.kasuminova.balloonserver.Configurations.BalloonServerConfig;
import github.kasuminova.balloonserver.Configurations.ConfigurationManager;
import github.kasuminova.balloonserver.GUI.*;
import github.kasuminova.balloonserver.GUI.LayoutManager.VFlowLayout;
import github.kasuminova.balloonserver.GUI.Panels.AboutPanel;
import github.kasuminova.balloonserver.GUI.Panels.SettingsPanel;
import github.kasuminova.balloonserver.Servers.LittleServer;
import github.kasuminova.balloonserver.Servers.LittleServerInterface;
import github.kasuminova.balloonserver.UpdateChecker.ApplicationVersion;
import github.kasuminova.balloonserver.Utils.FileUtil;
import github.kasuminova.balloonserver.Utils.GUILogger;
import github.kasuminova.balloonserver.Utils.Security;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static github.kasuminova.balloonserver.Utils.SvgIcons.*;

/**
 * 主 窗口/程序。
 */
public class BalloonServer {
    static {
        //设置全局主题，字体等
        SetupSwing.init();
    }
    public static final ApplicationVersion VERSION = new ApplicationVersion("1.2.0-STABLE-PREVIEW_2");
    private static final long START = System.currentTimeMillis();
    private static final JFrame PREMAIN_FRAME = new JFrame("加载中");
    private static final JPanel STATUS_PANEL = new JPanel(new BorderLayout());
    private static final JProgressBar PRE_LOAD_PROGRESS_BAR = new JProgressBar();
    public static final ImageIcon ICON = new ImageIcon(Objects.requireNonNull(
            BalloonServer.class.getResource("/image/icon_128x128.jpg")));
    //主窗口
    public static final JFrame MAIN_FRAME = new JFrame("BalloonServer " + VERSION);
    public static final JTabbedPane TABBED_PANE = new JTabbedPane(JTabbedPane.BOTTOM);
    public static final JTabbedPane SERVER_TABBED_PANE = new JTabbedPane(JTabbedPane.TOP);
    //主窗口 Logger
    public static final Logger GLOBAL_LOGGER = Logger.getLogger("MAIN");
    public static final JProgressBar GLOBAL_STATUS_PROGRESSBAR = new JProgressBar(0,1000);
    //全局线程池
    public static final ExecutorService GLOBAL_THREAD_POOL = Executors.newCachedThreadPool();
    //主面板
    public static final JPanel mainPanel = new JPanel(new BorderLayout());
    public static final BalloonServerConfig CONFIG = new BalloonServerConfig();
    //可用服务端接口列表，与 Tab 同步
    public static final List<LittleServerInterface> availableCustomServerInterfaces = Collections.synchronizedList(new ArrayList<>());
    private static void init() {
        //大小设置
        MAIN_FRAME.setSize(1350,785);
        MAIN_FRAME.setMinimumSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.8), MAIN_FRAME.getHeight()));

        //标签页配置
        PRE_LOAD_PROGRESS_BAR.setString("载入主面板...");
        TABBED_PANE.putClientProperty("JTabbedPane.tabAreaAlignment", "fill");

        //载入主配置文件
        loadConfig();

        //创建一个子面板，存放 SERVER_TABBED_PANE.
        //关于为什么要这么做，因为直接向 TABBED_PANE 放入 SERVER_TABBED_PANE 会导致服务端列表标签页可被删除，目前暂时不清楚问题所在...
        JPanel subMainPanel = new JPanel(new BorderLayout());
        subMainPanel.add(SERVER_TABBED_PANE, BorderLayout.CENTER);

        TABBED_PANE.addTab("服务端列表", SERVER_LIST_ICON, subMainPanel);
        TABBED_PANE.addTab("主程序控制面板", SETTINGS_ICON, SettingsPanel.getPanel());
        TABBED_PANE.addTab("关于本程序", ABOUT_ICON, AboutPanel.createPanel());

        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabClosable", true);
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.hideTabAreaWithOneTab", true);
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabCloseToolTipText", "关闭这个标签页。且只能关闭自定义服务器实例。");
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabCloseCallback", (BiConsumer<JTabbedPane, Integer>) (tabbedPane, tabIndex) -> {
            //检查是否为默认服务端
            if (tabIndex == 0) {
                JOptionPane.showMessageDialog(MAIN_FRAME, "你不可以删除默认服务端！", "警告", JOptionPane.WARNING_MESSAGE);
                return;
            }
            //定义变量
            LittleServerInterface serverInterface = availableCustomServerInterfaces.get(tabIndex);
            String serverName = serverInterface.getServerName();
            if (!stopLittleServer(serverInterface, serverName, tabIndex)) return;

            SERVER_TABBED_PANE.removeTabAt(tabIndex);
            availableCustomServerInterfaces.remove((int) tabIndex);
        });
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabsPopupPolicy", "asNeeded");
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.scrollButtonsPolicy", "asNeeded");
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.scrollButtonsPlacement", "both");

        Thread serverThread = new Thread(() -> {
            LittleServer littleServer;
            //自动启动服务器检测
            if (CONFIG.isAutoStartServer()) {
                littleServer = new LittleServer("littleserver", true);
                //自动启动服务器（仅本次）
            } else if (CONFIG.isAutoStartServerOnce()) {
                littleServer = new LittleServer("littleserver", true);

                CONFIG.setAutoStartServerOnce(false);

                try {
                    ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
                    BalloonServer.GLOBAL_LOGGER.info("已更新主程序配置文件.");
                } catch (IOException e) {
                    GLOBAL_LOGGER.warning("保存主程序配置文件失败！");
                }
            } else {
                littleServer = new LittleServer("littleserver", false);
            }
            availableCustomServerInterfaces.add(littleServer.getServerInterface());
            SERVER_TABBED_PANE.addTab("主服务端", DEFAULT_SERVER_ICON, littleServer.getPanel());
            mainPanel.add(TABBED_PANE, BorderLayout.CENTER);
        });
        serverThread.start();

        loadStatusBar();
        loadMenuBar();

        GLOBAL_THREAD_POOL.execute(() -> {
            long start = System.currentTimeMillis();
            SwingSystemTray.initSystemTrayAndFrame(MAIN_FRAME);
            MAIN_FRAME.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (CONFIG.getCloseOperation() == BalloonServerConfig.QUERY.getOperation()) {
                        ConfirmExitDialog confirmExitDialog = new ConfirmExitDialog(MAIN_FRAME, CONFIG);
                        confirmExitDialog.setVisible(true);
                    } else if (CONFIG.getCloseOperation() == BalloonServerConfig.HIDE_ON_CLOSE.getOperation()){
                        MAIN_FRAME.setVisible(false);
                    } else {
                        CONFIG.setCloseOperation(BalloonServerConfig.EXIT_ON_CLOSE.getOperation());
                        saveConfig();
                        System.exit(0);
                    }
                }
            });
            GLOBAL_LOGGER.info(String.format("任务栏已加载完毕, 耗时 %sms", System.currentTimeMillis() - start));
        });

        //等待主服务器面板完成创建
        try {
            serverThread.join();
        } catch (InterruptedException ignored) {}

        //主窗口
        MAIN_FRAME.add(mainPanel);
        MAIN_FRAME.setIconImage(ICON.getImage());
        MAIN_FRAME.setLocationRelativeTo(null);
        GLOBAL_LOGGER.info(String.format("程序已加载完成, 耗时 %sms", System.currentTimeMillis() - START));

        MAIN_FRAME.setVisible(true);
        PREMAIN_FRAME.dispose();
    }
    /**
     * 载入配置文件
     */
    private static void loadConfig() {
        try {
            if (new File("./balloonserver.json").exists()) {
                ConfigurationManager.loadBalloonServerConfigFromFile("./balloonserver.json", CONFIG);
                GLOBAL_LOGGER.info("成功载入主程序配置文件.");
            } else {
                GLOBAL_LOGGER.info("主程序配置文件不存在，正在创建文件...");
                ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
                GLOBAL_LOGGER.info("成功生成主程序配置文件.");
            }
        } catch (Exception e) {
            GLOBAL_LOGGER.warning("主程序配置文件加载失败！\n" + GUILogger.stackTraceToString(e));
        }
    }

    /**
     * 保存配置文件
     */
    private static void saveConfig() {
        try {
            ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
            GLOBAL_LOGGER.info("成功保存主程序配置文件.");
        } catch (IOException e) {
            GLOBAL_LOGGER.warning("主程序配置文件保存失败！\n" + GUILogger.stackTraceToString(e));
        }
    }

    /**
     * 菜单栏组装
     */
    private static void loadMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu newMenu = new JMenu("新建");
        newMenu.setIcon(PLUS_ICON);
        menuBar.add(newMenu);

        JMenuItem createNewLittleServer = new JMenuItem("创建一个新的更新服务器实例", PLUS_ICON);
        newMenu.add(createNewLittleServer);
        createNewLittleServer.addActionListener(e -> {
            String serverName = JOptionPane.showInputDialog(MAIN_FRAME,"请输入服务器实例名称","创建",JOptionPane.INFORMATION_MESSAGE);
            if (Security.stringIsUnsafe(MAIN_FRAME, serverName, new String[]{"balloonserver","littleserver","res-cache",".lscfg",".json"})) return;

            if (new File(String.format("./%s.lscfg.json", serverName)).exists()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "已存在此服务器名，请使用载入服务器.","已存在服务器",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (checkSameServer(serverName)) return;

            GLOBAL_THREAD_POOL.execute(() -> {
                long start = System.currentTimeMillis();
                GLOBAL_LOGGER.info(String.format("正在创建新的服务器实例：%s", serverName));

                LittleServer customServer = new LittleServer(serverName, false);

                SERVER_TABBED_PANE.addTab(serverName, CUSTOM_SERVER_ICON, customServer.getPanel());
                SERVER_TABBED_PANE.setSelectedIndex(SERVER_TABBED_PANE.getTabCount() - 1);

                GLOBAL_LOGGER.info(String.format("实例创建耗时 %sms.", System.currentTimeMillis() - start));
            });
        });

        JMenu loadMenu = new JMenu("实例管理");
        loadMenu.setIcon(RESOURCE_ICON);
        menuBar.add(loadMenu);

        JMenuItem loadLittleServer = new JMenuItem("载入更新服务器实例", PLAY_ICON);
        loadMenu.add(loadLittleServer);
        loadLittleServer.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(new FileUtil.SimpleFileFilter(new String[]{"lscfg.json"}, new String[]{"res-cache", "littleserver", "balloonserver"} ,"LittleServer 配置文件 (*.lscfg.json)"));

            if (!(fileChooser.showDialog(MAIN_FRAME, "载入选中实例") == JFileChooser.APPROVE_OPTION)) {
                return;
            }
            File[] selectedFiles = fileChooser.getSelectedFiles();
            String[] serverNames = new String[selectedFiles.length];

            for (int i = 0; i < selectedFiles.length; i++) {
                serverNames[i] = selectedFiles[i].getName().replace(".lscfg.json", "");
            }

            LittleServer[] customServers = new LittleServer[serverNames.length];
            ArrayList<Thread> threadList = new ArrayList<>();
            //检查是否存在非法名称或已存在的名称
            for (String serverName : serverNames) {
                if (Security.stringIsUnsafe(MAIN_FRAME, serverName, null)) {
                    return;
                }
                if (checkSameServer(serverName)) {
                    return;
                }
            }
            //循环多线程载入服务器面板
            for (int i = 0; i < serverNames.length; i++) {
                String serverName = serverNames[i];

                int panelArrayIndex = i;
                Thread thread = new Thread(() -> {
                    long start = System.currentTimeMillis();
                    GLOBAL_LOGGER.info(String.format("正在载入服务器实例：%s", serverName));
                    LittleServer customServer = new LittleServer(serverName, false);
                    customServers[panelArrayIndex] = customServer;
                    GLOBAL_LOGGER.info(String.format("实例载入耗时 %sms.", System.currentTimeMillis() - start));
                });
                threadList.add(thread);
                thread.start();
            }

            //等待所有线程操作完成
            for (Thread thread : threadList) {
                try {
                    thread.join();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            //按顺序添加面板和接口
            for (int i = 0; i < customServers.length; i++) {
                SERVER_TABBED_PANE.addTab(serverNames[i], CUSTOM_SERVER_ICON, customServers[i].getPanel());
                availableCustomServerInterfaces.add(customServers[i].getServerInterface());
            }

            //设置为最后一个面板
            SERVER_TABBED_PANE.setSelectedIndex(SERVER_TABBED_PANE.getTabCount() - 1);
        });

        JMenuItem resetLittleServer = new JMenuItem("重置当前显示的服务器实例", RELOAD_ICON);
        loadMenu.add(resetLittleServer);
        resetLittleServer.addActionListener(e -> {
            int selected = SERVER_TABBED_PANE.getSelectedIndex();
            //定义变量
            LittleServerInterface serverInterface = availableCustomServerInterfaces.get(selected);
            String serverName = serverInterface.getServerName();

            if (stopLittleServer(serverInterface, serverName, selected)) {
                GLOBAL_THREAD_POOL.execute(() -> {
                    LittleServer littleServer;
                    if (serverName.equals("littleserver")) {
                        littleServer = new LittleServer("littleserver", false);
                    } else {
                        littleServer = new LittleServer(serverName, false);
                    }
                    availableCustomServerInterfaces.set(selected, littleServer.getServerInterface());
                    SERVER_TABBED_PANE.setComponentAt(selected, littleServer.getPanel());
                    //回收旧实例内存
                    System.gc();
                });
            }
        });

        MAIN_FRAME.setJMenuBar(menuBar);
    }

    /**
     * 保存并停止对应服务端实例，并从服务端列表上移除
     * @param serverInterface 服务器接口
     * @param serverName 服务器名
     * @param index 服务端列表位置
     * @return 用户是否确认关闭了服务器
     */
    private static boolean stopLittleServer(LittleServerInterface serverInterface, String serverName, int index) {
        boolean isStarted = serverInterface.isStarted().get();
        //如果服务器已启动，则提示是否关闭服务器
        if (isStarted) {
            int selection = JOptionPane.showConfirmDialog(MAIN_FRAME, String.format("%s 实例正在运行，你希望关闭服务器后再关闭此实例吗？", serverName), "警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (!(selection == JOptionPane.YES_OPTION)) return false;
            if (!serverInterface.stopServer()) {
                JOptionPane.showMessageDialog(MAIN_FRAME, "无法正常关闭服务器，请检查窗口.", "错误", JOptionPane.ERROR_MESSAGE);
                SERVER_TABBED_PANE.setSelectedIndex(index);
                return false;
            }
        } else {
            int selection = JOptionPane.showConfirmDialog(MAIN_FRAME, String.format("你要关闭 %s 这个实例吗？", serverName) , "提示", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (!(selection == JOptionPane.YES_OPTION)) return false;
        }
        //保存配置并移除
        serverInterface.saveConfig();
        return true;
    }

    /**
     * 检查服务器列表中是否已存在相应名称的服务器
     * @param serverName 服务器名
     * @return 如果有相同返回 true, 无则返回 false
     */
    private static boolean checkSameServer(String serverName) {
        for (int i = 0; i < SERVER_TABBED_PANE.getTabCount(); i++) {
            if (SERVER_TABBED_PANE.getTitleAt(i).equals(serverName)) {
                JOptionPane.showMessageDialog(MAIN_FRAME, String.format("名为 %s 的配置文件已经载入到服务器中了!", serverName), "错误", JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
        return false;
    }

    /**
     * 主窗口状态栏面板组装
     */
    private static void loadStatusBar() {
        //状态栏
        STATUS_PANEL.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(4, 4, 4, 4)));
        JLabel threadCount = new JLabel("当前运行的线程数量：0");
        STATUS_PANEL.add(threadCount, BorderLayout.WEST);
        //线程数监控 + 内存监控
        Box memBarBox = Box.createHorizontalBox();
        JProgressBar memBar = new JProgressBar(0,225);
        memBar.setPreferredSize(new Dimension(memBar.getMaximum(),memBar.getHeight()));
        memBar.setBorder(new EmptyBorder(0,0,0,5));
        memBar.setStringPainted(true);
        memBarBox.add(new JLabel("JVM 内存使用情况："));
        memBarBox.add(memBar);
        //内存清理
        JButton GC = new JButton("清理");
        GC.setPreferredSize(new Dimension(65,22));
        GC.addActionListener(e -> System.gc());
        memBarBox.add(GC);
        STATUS_PANEL.add(memBarBox, BorderLayout.EAST);
        GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
        GLOBAL_STATUS_PROGRESSBAR.setStringPainted(true);
        GLOBAL_STATUS_PROGRESSBAR.setBorder(new EmptyBorder(0, 25, 0, 25));
        GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
        STATUS_PANEL.add(GLOBAL_STATUS_PROGRESSBAR);
        mainPanel.add(STATUS_PANEL, BorderLayout.SOUTH);
        //定时器, 更新内存和线程信息
        Timer statusTimer = new Timer(500, e -> {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long memoryUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long memoryTotal = memoryMXBean.getHeapMemoryUsage().getCommitted();

            threadCount.setText(String.format("当前运行的线程数量：%s", Thread.activeCount()));

            memBar.setValue((int) ((double) memoryUsed * memBar.getMaximum() / memoryTotal));
            memBar.setString(String.format("%s M / %s M - Max: %s M",
                    memoryUsed / (1024 * 1024),
                    memoryTotal / (1024 * 1024),
                    memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024)
            ));
        });
        statusTimer.start();
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
