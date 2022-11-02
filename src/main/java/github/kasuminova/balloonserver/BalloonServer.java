package github.kasuminova.balloonserver;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import github.kasuminova.balloonserver.configurations.BalloonServerConfig;
import github.kasuminova.balloonserver.configurations.ConfigurationManager;
import github.kasuminova.balloonserver.gui.ConfirmExitDialog;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.gui.panels.AboutPanel;
import github.kasuminova.balloonserver.gui.panels.SettingsPanel;
import github.kasuminova.balloonserver.gui.SetupSwing;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.gui.SwingSystemTray;
import github.kasuminova.balloonserver.servers.localserver.IntegratedServer;
import github.kasuminova.balloonserver.servers.localserver.IntegratedServerInterface;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteIntegratedServerClient;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;
import github.kasuminova.balloonserver.updatechecker.Checker;
import github.kasuminova.balloonserver.utils.FileUtil;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.balloonserver.utils.Security;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static github.kasuminova.balloonserver.utils.SvgIcons.*;

/**
 * 主 窗口/程序。
 */
public final class BalloonServer {
    static {
        //设置全局主题，字体等
        SetupSwing.init();
    }

    public static ThreadPoolExecutor GLOBAL_FILE_THREAD_POOL;
    public static final ThreadPoolExecutor GLOBAL_DIR_THREAD_POOL = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            30, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    public static final ApplicationVersion VERSION = new ApplicationVersion("1.4.0-BETA");
    public static final String TITLE = "BalloonServer " + VERSION;
    //主窗口
    public static final JFrame MAIN_FRAME = new JFrame(TITLE);
    /*
    可执行文件名称。
    如 BalloonServer-GUI-1.2.0-STABLE.jar,
    如果为 exe 格式则为 C:/Users/username/AppData/Local/Temp/e4j+随机缓存名/BalloonServer-GUI-1.2.0-STABLE.jar
     */
    public static final String ARCHIVE_NAME = BalloonServer.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    public static final ImageIcon ICON = new ImageIcon(Objects.requireNonNull(
            BalloonServer.class.getResource("/image/icon_128x128.jpg")));
    public static final JTabbedPane TABBED_PANE = new JTabbedPane(JTabbedPane.BOTTOM);
    public static final JTabbedPane SERVER_TABBED_PANE = new JTabbedPane(JTabbedPane.TOP);
    public static final JTabbedPane REMOTE_SERVER_TABBED_PANE = new JTabbedPane(JTabbedPane.TOP);
    //主窗口 Logger
    public static final GUILogger GLOBAL_LOGGER = new GUILogger("main");
    public static final SmoothProgressBar GLOBAL_STATUS_PROGRESSBAR = new SmoothProgressBar(1000, 250);
    //主面板
    public static final JPanel MAIN_PANEL = new JPanel(new BorderLayout());
    public static final BalloonServerConfig CONFIG = new BalloonServerConfig();
    //可用服务端接口列表，与 SERVER_TABBED_PANE 中的标签页同步
    public static final List<IntegratedServerInterface> AVAILABLE_SERVER_INTERFACES = Collections.synchronizedList(new ArrayList<>());
    //支持放入多个任务的 Timer
    public static final Timer GLOBAL_QUERY_TIMER = new Timer(false);
    private static final long START = System.currentTimeMillis();
    private static final JFrame PREMAIN_FRAME = new JFrame("加载中");
    private static final JPanel STATUS_PANEL = new JPanel(new BorderLayout());
    private static final JProgressBar PRE_LOAD_PROGRESS_BAR = new JProgressBar();

    private static void init() {
        //大小设置
        MAIN_FRAME.setMinimumSize(new Dimension(1150,725));

        //标签页配置
        PRE_LOAD_PROGRESS_BAR.setString("载入主面板...");
        TABBED_PANE.putClientProperty("JTabbedPane.tabAreaAlignment", "fill");
        MAIN_PANEL.add(TABBED_PANE, BorderLayout.CENTER);
        MAIN_FRAME.add(MAIN_PANEL);

        //载入主配置文件
        loadConfig();
        //载入文件计算线程池
        initFileThreadPool();

        //创建一个子面板，存放 SERVER_TABBED_PANE.
        //关于为什么要这么做，因为直接向 TABBED_PANE 放入 SERVER_TABBED_PANE 会导致服务端列表标签页可被删除，目前暂时不清楚问题所在...
        JPanel localServerPanel = new JPanel(new BorderLayout());
        localServerPanel.add(SERVER_TABBED_PANE, BorderLayout.CENTER);
        JPanel remoteServerPanel = new JPanel(new BorderLayout());
        remoteServerPanel.add(REMOTE_SERVER_TABBED_PANE, BorderLayout.CENTER);

        TABBED_PANE.addTab("本地服务端实例列表", SERVER_LIST_ICON, localServerPanel);
        TABBED_PANE.addTab("远程服务端实例列表", SERVER_LIST_ICON, remoteServerPanel);
        TABBED_PANE.addTab("主程序控制面板", SETTINGS_ICON, SettingsPanel.getPanel());
        TABBED_PANE.addTab("关于本程序", ABOUT_ICON, AboutPanel.createPanel());

        loadServerTabbedPaneProperty();
        loadRemoteServerTabbedPaneProperty();

        Thread serverThread = new Thread(() -> {
            loadDefaultIntegratedServer();
            loadDefaultRemoteIntegratedServerClient();
        });
        serverThread.start();

        loadStatusBar();
        loadMenuBar();

        ThreadUtil.execute(BalloonServer::loadSystemTrayFeature);

        //等待主服务器面板完成创建
        ThreadUtil.waitForDie(serverThread);

        //主窗口
        MAIN_FRAME.setIconImage(ICON.getImage());
        MAIN_FRAME.setLocationRelativeTo(null);
        GLOBAL_LOGGER.info(String.format("程序已加载完成, 耗时 %sms", System.currentTimeMillis() - START));

        MAIN_FRAME.setVisible(true);
        PREMAIN_FRAME.dispose();

        loadAutoUpdateFeature();
    }

    /**
     * 载入自动更新功能
     */
    private static void loadAutoUpdateFeature() {
        //当前是否在检查更新，防止长时间静置未操作弹出多个对话框
        AtomicBoolean isCheckingUpdate = new AtomicBoolean(false);
        //更新检查线程，每一小时检查一次最新版本
        GLOBAL_QUERY_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                if (CONFIG.isAutoCheckUpdates() && !isCheckingUpdate.get()) {
                    GLOBAL_LOGGER.info("开始检查更新...");
                    isCheckingUpdate.set(true);
                    ThreadUtil.execute(() -> {
                        Checker.checkUpdates();
                        isCheckingUpdate.set(false);
                    });
                }
            }
        }, 0, 3600000);
    }

    /**
     * 载入系统托盘
     */
    private static void loadSystemTrayFeature() {
        long start = System.currentTimeMillis();
        SwingSystemTray.initSystemTrayAndFrame(MAIN_FRAME);
        MAIN_FRAME.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (CONFIG.getCloseOperation() == BalloonServerConfig.QUERY.getOperation()) {
                    ConfirmExitDialog confirmExitDialog = new ConfirmExitDialog(MAIN_FRAME, CONFIG);
                    MAIN_FRAME.setEnabled(false);
                    confirmExitDialog.setVisible(true);
                } else if (CONFIG.getCloseOperation() == BalloonServerConfig.HIDE_ON_CLOSE.getOperation()) {
                    MAIN_FRAME.setVisible(false);
                } else {
                    CONFIG.setCloseOperation(BalloonServerConfig.EXIT_ON_CLOSE.getOperation());
                    saveConfig();
                    System.exit(0);
                }
            }
        });
        GLOBAL_LOGGER.info(String.format("任务栏已加载完毕, 耗时 %sms", System.currentTimeMillis() - start));
    }

    /**
     * 载入服务器标签页配置
     */
    private static void loadServerTabbedPaneProperty() {
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabClosable", true);
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.hideTabAreaWithOneTab", true);
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabCloseToolTipText", "关闭这个标签页。且只能关闭自定义服务器实例。");
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabCloseCallback", (BiConsumer<JTabbedPane, Integer>) (tabbedPane, tabIndex) -> {
            //检查是否为默认服务端
            if (tabIndex == 0) {
                JOptionPane.showMessageDialog(MAIN_FRAME, "你不可以删除默认服务端！", BalloonServer.TITLE, JOptionPane.WARNING_MESSAGE);
                return;
            }
            //定义变量
            IntegratedServerInterface serverInterface = AVAILABLE_SERVER_INTERFACES.get(tabIndex);
            String serverName = serverInterface.getServerName();
            if (!stopIntegratedServer(serverInterface, serverName, tabIndex, true)) return;

            SERVER_TABBED_PANE.removeTabAt(tabIndex);
            AVAILABLE_SERVER_INTERFACES.remove((int) tabIndex);
        });
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabsPopupPolicy", "asNeeded");
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.scrollButtonsPolicy", "asNeeded");
        SERVER_TABBED_PANE.putClientProperty("JTabbedPane.scrollButtonsPlacement", "both");
    }

    private static void initFileThreadPool() {
        if (CONFIG.isLowIOPerformanceMode()) {
            GLOBAL_FILE_THREAD_POOL = new ThreadPoolExecutor(
                    1, 1,
                    10, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());
        } else if (CONFIG.getFileThreadPoolSize() == 0){
            GLOBAL_FILE_THREAD_POOL = new ThreadPoolExecutor(
                    Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 2,
                    10, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());
        } else {
            GLOBAL_FILE_THREAD_POOL = new ThreadPoolExecutor(
                    CONFIG.getFileThreadPoolSize(), CONFIG.getFileThreadPoolSize(),
                    10, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());
        }
        GLOBAL_LOGGER.info("文件计算线程池大小为 {} 线程", GLOBAL_FILE_THREAD_POOL.getMaximumPoolSize());
    }

    private static void loadRemoteServerTabbedPaneProperty() {
//        REMOTE_SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabClosable", true);
        REMOTE_SERVER_TABBED_PANE.putClientProperty("JTabbedPane.hideTabAreaWithOneTab", true);

        REMOTE_SERVER_TABBED_PANE.putClientProperty("JTabbedPane.tabsPopupPolicy", "asNeeded");
        REMOTE_SERVER_TABBED_PANE.putClientProperty("JTabbedPane.scrollButtonsPolicy", "asNeeded");
        REMOTE_SERVER_TABBED_PANE.putClientProperty("JTabbedPane.scrollButtonsPlacement", "both");
    }

    /**
     * 载入默认集成服务端
     */
    private static void loadDefaultIntegratedServer() {
        IntegratedServer abstractIntegratedServer;
        //自动启动服务器检测
        if (CONFIG.isAutoStartServer()) {
            abstractIntegratedServer = new IntegratedServer("littleserver", true);
            //自动启动服务器（仅本次）
        } else if (CONFIG.isAutoStartServerOnce()) {
            abstractIntegratedServer = new IntegratedServer("littleserver", true);

            CONFIG.setAutoStartServerOnce(false);
            SettingsPanel.applyConfiguration();

            try {
                ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
                BalloonServer.GLOBAL_LOGGER.info("已更新主程序配置文件.");
            } catch (IORuntimeException e) {
                GLOBAL_LOGGER.error("保存主程序配置文件失败！", e);
            }
        } else {
            abstractIntegratedServer = new IntegratedServer("littleserver", false);
        }
        SERVER_TABBED_PANE.addTab("集成服务端", DEFAULT_SERVER_ICON, abstractIntegratedServer.getPanel());
        AVAILABLE_SERVER_INTERFACES.add(abstractIntegratedServer.getServerInterface());
    }

    /**
     * 载入默认远程服务端
     */
    private static void loadDefaultRemoteIntegratedServerClient() {
        RemoteIntegratedServerClient client = new RemoteIntegratedServerClient("hyperserver");
        REMOTE_SERVER_TABBED_PANE.addTab("远程服务端", DEFAULT_SERVER_ICON, client.getPanel());
    }

    /**
     * 关闭所有服务端实例
     *
     * @param inquireUser 是否向用户确认关闭正在运行的服务端
     */
    public static void stopAllServers(boolean inquireUser) {
        //停止所有运行的实例
        for (int i = 0; i < AVAILABLE_SERVER_INTERFACES.size(); i++) {
            IntegratedServerInterface serverInterface = AVAILABLE_SERVER_INTERFACES.get(i);
            if (stopIntegratedServer(serverInterface, serverInterface.getServerName(), i, inquireUser)) {
                if (i > 1) {
                    SERVER_TABBED_PANE.removeTabAt(i);
                    AVAILABLE_SERVER_INTERFACES.remove(i);
                    i--;
                }
            }
        }
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
                GLOBAL_LOGGER.info("主程序配置文件不存在, 正在创建文件...");
                ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
                GLOBAL_LOGGER.info("成功生成主程序配置文件.");
            }
        } catch (Exception e) {
            GLOBAL_LOGGER.error("主程序配置文件加载失败！", e);
        }
    }

    /**
     * 保存主程序配置文件
     */
    public static void saveConfig() {
        try {
            ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
            GLOBAL_LOGGER.info("成功保存主程序配置文件.");
        } catch (IORuntimeException e) {
            GLOBAL_LOGGER.error("主程序配置文件保存失败！", e);
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

        JMenuItem createNewLittleServer = new JMenuItem("创建集成服务端实例", PLUS_ICON);
        newMenu.add(createNewLittleServer);
        createNewLittleServer.addActionListener(e -> {
            String serverName = JOptionPane.showInputDialog(MAIN_FRAME, "请输入服务器实例名称", BalloonServer.TITLE, JOptionPane.INFORMATION_MESSAGE);
            if (Security.stringIsUnsafe(MAIN_FRAME, serverName, new String[]{"balloonserver", "littleserver", "res-cache", ".lscfg", ".json"}))
                return;

            if (new File(String.format("./%s.lscfg.json", serverName)).exists()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "已存在此服务器名，请使用载入服务器.", BalloonServer.TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (checkSameServer(serverName)) return;

            ThreadUtil.execute(() -> {
                long start = System.currentTimeMillis();
                GLOBAL_LOGGER.info(String.format("正在创建新的集成服务端实例: %s", serverName));

                IntegratedServer customServer = new IntegratedServer(serverName, false);

                AVAILABLE_SERVER_INTERFACES.add(customServer.getServerInterface());
                SERVER_TABBED_PANE.addTab(serverName, CUSTOM_SERVER_ICON, customServer.getPanel());
                SERVER_TABBED_PANE.setSelectedIndex(SERVER_TABBED_PANE.getTabCount() - 1);

                GLOBAL_LOGGER.info(String.format("实例创建耗时 %sms.", System.currentTimeMillis() - start));
            });
        });

        JMenu loadMenu = new JMenu("实例管理");
        loadMenu.setIcon(RESOURCE_ICON);
        menuBar.add(loadMenu);

        JMenuItem loadLittleServer = new JMenuItem("载入集成服务端实例", PLAY_ICON);
        loadMenu.add(loadLittleServer);
        loadLittleServer.addActionListener(e -> ThreadUtil.execute(() -> {
            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(new FileUtil.SimpleFileFilter(new String[]{"lscfg.json"}, new String[]{"res-cache", "littleserver", "balloonserver"}, "LittleServer 配置文件 (*.lscfg.json)"));

            if (!(fileChooser.showDialog(MAIN_FRAME, "载入选中实例") == JFileChooser.APPROVE_OPTION)) {
                return;
            }
            File[] selectedFiles = fileChooser.getSelectedFiles();

            String[] serverNames = new String[selectedFiles.length];

            for (int i = 0; i < selectedFiles.length; i++) {
                serverNames[i] = StrUtil.removeSuffix(selectedFiles[i].getName(), ".lscfg.json");
            }

            IntegratedServer[] customServers = new IntegratedServer[serverNames.length];
            ArrayList<Thread> threadList = new ArrayList<>();
            //检查是否存在非法名称或已存在的名称
            for (String serverName : serverNames) {
                if (Security.stringIsUnsafe(MAIN_FRAME, serverName, null)) return;
                if (checkSameServer(serverName)) return;
            }
            //循环多线程载入服务器面板
            for (int i = 0; i < serverNames.length; i++) {
                String serverName = serverNames[i];

                int panelArrayIndex = i;
                Thread thread = new Thread(() -> {
                    long start = System.currentTimeMillis();
                    GLOBAL_LOGGER.info(String.format("正在载入集成服务端实例: %s", serverName));
                    IntegratedServer customServer;
                    customServer = new IntegratedServer(serverName, false);
                    customServers[panelArrayIndex] = customServer;
                    GLOBAL_LOGGER.info(String.format("实例载入耗时 %sms.", System.currentTimeMillis() - start));
                });
                threadList.add(thread);
                thread.start();
            }

            //等待所有线程操作完成
            for (Thread thread : threadList) ThreadUtil.waitForDie(thread);

            //按顺序添加面板和接口
            for (int i = 0; i < customServers.length; i++) {
                SERVER_TABBED_PANE.addTab(serverNames[i], CUSTOM_SERVER_ICON, customServers[i].getPanel());
                AVAILABLE_SERVER_INTERFACES.add(customServers[i].getServerInterface());
            }

            //设置为最后一个面板
            SERVER_TABBED_PANE.setSelectedIndex(SERVER_TABBED_PANE.getTabCount() - 1);
        }));

        JMenuItem resetLittleServer = new JMenuItem("重置当前显示的集成服务端实例", RELOAD_ICON);
        loadMenu.add(resetLittleServer);
        resetLittleServer.addActionListener(e -> {
            int selected = SERVER_TABBED_PANE.getSelectedIndex();
            //定义变量
            IntegratedServerInterface serverInterface = AVAILABLE_SERVER_INTERFACES.get(selected);
            String serverName = serverInterface.getServerName();

            if (stopIntegratedServer(serverInterface, serverName, selected, true)) {
                ThreadUtil.execute(() -> {
                    IntegratedServer littleServer;
                    if (serverName.equals("littleserver")) {
                        littleServer = new IntegratedServer("littleserver", false);
                    } else {
                        littleServer = new IntegratedServer(serverName, false);
                    }
                    AVAILABLE_SERVER_INTERFACES.set(selected, littleServer.getServerInterface());
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
     *
     * @param serverInterface 服务器接口
     * @param serverName      服务器名
     * @param index           服务端列表位置
     * @param inquireUser     是否向用户确认关闭服务端
     * @return 用户是否确认关闭了服务器
     */
    private static boolean stopIntegratedServer(IntegratedServerInterface serverInterface, String serverName, int index, boolean inquireUser) {
        boolean isGenerating = serverInterface.isGenerating().get();
        //如果服务器正在生成缓存，并且启用了向用户确认关闭服务端的选项，则弹出警告对话框
        if (isGenerating && inquireUser) {
            JOptionPane.showMessageDialog(MAIN_FRAME,
                    "当前正在生成资源缓存，请稍后再试。", BalloonServer.TITLE,
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        boolean isStarted = serverInterface.isStarted().get();

        //如果服务器已启动，则提示是否关闭服务器
        if (isStarted) {
            if (inquireUser) {
                int selection = JOptionPane.showConfirmDialog(MAIN_FRAME, String.format("%s 实例正在运行，你希望关闭服务器后再关闭此实例吗？", serverName), BalloonServer.TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (!(selection == JOptionPane.YES_OPTION)) return false;
            }

            //停止服务器
            if (!serverInterface.stopServer()) {
                JOptionPane.showMessageDialog(MAIN_FRAME, "无法正常关闭服务器，请检查窗口.", BalloonServer.TITLE, JOptionPane.ERROR_MESSAGE);
                SERVER_TABBED_PANE.setSelectedIndex(index);
                return false;
            }
        }

        //保存配置
        serverInterface.saveConfig();
        //关闭 Logger 文件输出
        try {
            serverInterface.getLogger().closeLogWriter();
        } catch (IOException e) {
            GLOBAL_LOGGER.error("无法关闭 logger", e);
        }
        return true;
    }

    /**
     * 检查服务器列表中是否已存在相应名称的服务器
     *
     * @param serverName 服务器名
     * @return 如果有相同返回 true, 无则返回 false
     */
    private static boolean checkSameServer(String serverName) {
        for (int i = 0; i < SERVER_TABBED_PANE.getTabCount(); i++) {
            if (SERVER_TABBED_PANE.getTitleAt(i).equals(serverName)) {
                JOptionPane.showMessageDialog(MAIN_FRAME, String.format("名为 %s 的配置文件已经载入到服务器中了!", serverName), BalloonServer.TITLE, JOptionPane.ERROR_MESSAGE);
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
        STATUS_PANEL.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(3, 4, 2, 4)));
        JLabel threadCount = new JLabel("当前运行的线程数量: 0");
        STATUS_PANEL.add(threadCount, BorderLayout.WEST);
        //线程数监控 + 内存监控
        Box memBarBox = Box.createHorizontalBox();
        SmoothProgressBar memBar = new SmoothProgressBar(250, 250);
        memBar.setPreferredSize(new Dimension(memBar.getMaximum(), memBar.getHeight()));
        memBar.setBorder(new EmptyBorder(1, 0, 0, 5));
        memBar.setStringPainted(true);
        memBarBox.add(new JLabel("JVM 内存使用情况: "));
        memBarBox.add(memBar);
        //内存清理
        JButton GC = new JButton("清理");
        GC.setPreferredSize(new Dimension(65, 21));
        GC.addActionListener(e -> System.gc());
        memBarBox.add(GC);
        STATUS_PANEL.add(memBarBox, BorderLayout.EAST);
        GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
        GLOBAL_STATUS_PROGRESSBAR.setStringPainted(true);
        GLOBAL_STATUS_PROGRESSBAR.setBorder(new EmptyBorder(0, 25, 0, 25));
        STATUS_PANEL.add(GLOBAL_STATUS_PROGRESSBAR);
        MAIN_PANEL.add(STATUS_PANEL, BorderLayout.SOUTH);
        //新建循环任务，每 500ms 更新一次内存占用和线程数量
        GLOBAL_QUERY_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                long memoryFree = SystemUtil.getFreeMemory();
                long memoryTotal = SystemUtil.getTotalMemory();

                threadCount.setText(String.format("当前运行的线程数量: %s", Thread.activeCount()));

                memBar.setValue((int) ((double) (memoryTotal - memoryFree) * memBar.getMaximum() / memoryTotal));
                memBar.setString(String.format("%s M / %s M - Max: %s M",
                        (memoryTotal - memoryFree) / (1024 * 1024),
                        memoryTotal / (1024 * 1024),
                        SystemUtil.getMaxMemory() / (1024 * 1024)
                ));
            }
        }, 0, 500);
    }

    /**
     * 程序预加载
     */
    private static void preInit() {
        JPanel panel = new JPanel(new VFlowLayout());
        panel.add(new JLabel(new ImageIcon(ICON.getImage().getScaledInstance(96, 96, Image.SCALE_DEFAULT))));
        panel.add(new JLabel("程序启动中，请稍等..."));
        PRE_LOAD_PROGRESS_BAR.setStringPainted(true);
        PRE_LOAD_PROGRESS_BAR.setIndeterminate(true);
        PRE_LOAD_PROGRESS_BAR.setString("...");
        panel.add(PRE_LOAD_PROGRESS_BAR);
        PREMAIN_FRAME.add(panel);
        PREMAIN_FRAME.setSize(240, 150);
        PREMAIN_FRAME.setUndecorated(true);
        PREMAIN_FRAME.setLocationRelativeTo(null);
        PREMAIN_FRAME.setVisible(true);

        //运行环境检测
        PRE_LOAD_PROGRESS_BAR.setString("检查运行环境...");
        int version = Integer.parseInt(System.getProperty("java.specification.version"));
        GLOBAL_LOGGER.info("Java 版本为 " + version);
        GLOBAL_LOGGER.info("程序分支为 " + VERSION.getBranch());
        if (VERSION.getBranch().equals("ALPHA")) {
            int selection = JOptionPane.showConfirmDialog(PREMAIN_FRAME, """
                            此版本为 ALPHA 版本，通常引入了一些可能存在破坏性的强大的新功能。
                            在使用之前，您应知悉此版本可能存在无征兆的程序崩溃，报错。
                            请不要将此版本投入生产环境
                            请不要将此版本投入生产环境
                            请不要将此版本投入生产环境
                            在阅读上方提示后，请点击 “是” 启动程序并代表你愿意承担因不稳定版本造成的程序崩溃后果，
                            选择 “否” 退出程序。""",
                    "WARNING", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (!(selection == JOptionPane.YES_OPTION)) {
                System.exit(0);
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
