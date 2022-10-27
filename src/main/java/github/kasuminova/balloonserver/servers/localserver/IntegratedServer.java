package github.kasuminova.balloonserver.servers.localserver;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.configurations.ConfigurationManager;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.httpserver.HttpServer;
import github.kasuminova.balloonserver.servers.AbstractServer;
import github.kasuminova.balloonserver.utils.*;
import github.kasuminova.balloonserver.utils.filecacheutils.JsonCacheUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static github.kasuminova.balloonserver.BalloonServer.MAIN_FRAME;
import static github.kasuminova.balloonserver.BalloonServer.TITLE;
import static github.kasuminova.balloonserver.utils.SvgIcons.DELETE_ICON;

/**
 * IntegratedServer 集成服务端面板实例
 */
public class IntegratedServer extends AbstractServer {
    protected final JSONObject index = new JSONObject();
    protected final String resJsonFileExtensionName = "res-cache";
    protected final String legacyResJsonFileExtensionName = "legacy_res-cache";
    protected final String configFileSuffix = ".lscfg.json";
    protected final JLabel statusLabel = new JLabel("状态: 就绪", JLabel.LEFT);
    protected final JButton copyAddressButton = new JButton("复制 API 地址");
    protected final JButton openAddressButton = new JButton("在浏览器中打开 API 地址");
    protected final String serverName;
    protected final GUILogger logger;
    protected final IntegratedServerConfig config = new IntegratedServerConfig();
    protected final JFrame requestListFrame = new JFrame();
    protected final JPanel requestListPanel = new JPanel(new VFlowLayout());
    protected final JScrollPane requestListScrollPane = new JScrollPane(
            requestListPanel,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    protected final JPanel littleServerPanel = new JPanel(new BorderLayout());
    protected final JButton startOrStop = new JButton("保存配置并启动服务器");

    protected HttpServer server;
    protected String indexJson = null;
    protected String resJson = null;
    protected String legacyResJson = null;
    protected IntegratedServerInterface serverInterface;

    /**
     * 创建一个服务器面板，并绑定一个新的服务器实例
     *
     * @param serverName LittleServerConfig 配置文件路径
     * @param autoStart  是否在创建对象后自动启动服务器
     */
    public IntegratedServer(String serverName, boolean autoStart) {
        this.serverName = serverName;

        long start = System.currentTimeMillis();
        //设置 Logger，主体为 logPanel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setMinimumSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.5), 0));

        logPanel.setBorder(new TitledBorder("服务器实例日志"));
        JTextPane logPane = new JTextPane();
        logPane.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logPane);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        //初始化 Logger
        logger = new GUILogger(serverName, logPane);

        //日志窗口菜单
        JPopupMenu logPaneMenu = new JPopupMenu();
        JMenuItem cleanLogPane = new JMenuItem("清空日志", DELETE_ICON);
        cleanLogPane.addActionListener(e -> {
            try {
                logPane.getDocument().remove(0, logPane.getDocument().getLength());
                logger.info("已清空当前服务器实例日志窗口.");
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
        logPaneMenu.add(cleanLogPane);
        logPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    logPaneMenu.show(logPane, e.getX(), e.getY());
                }
            }
        });

        //载入配置文件并初始化 HTTP 服务端
        loadConfigurationFromFile();
        loadHttpServer();

        //服务器窗口组装
        JPanel rightPanel = new JPanel(new BorderLayout());
        //控制面板
        rightPanel.add(loadControlPanel(), BorderLayout.WEST);

        //上传列表窗口
        loadRequestList();

        //组装主面板
        littleServerPanel.add(logPanel, BorderLayout.CENTER);
        littleServerPanel.add(rightPanel, BorderLayout.EAST);
        //载入状态栏
        littleServerPanel.add(loadStatusBar(), BorderLayout.SOUTH);
        logger.info(String.format("载入服务器耗时 %sms.", System.currentTimeMillis() - start));

        if (autoStart) {
            logger.info("检测到自动启动服务器选项已开启, 正在启动服务器...");
            startServer();
        }
    }

    /**
     * 返回当前实例的面板
     *
     * @return 服务器面板
     */
    public JPanel getPanel() {
        return littleServerPanel;
    }

    /**
     * 返回当前服务器实例的接口
     *
     * @return LittleServerInterface
     */
    public IntegratedServerInterface getServerInterface() {
        return serverInterface;
    }

    /**
     * 启动服务器
     */
    protected void startServer() {
        ThreadUtil.execute(() -> {
            //检查当前端口是否被占用
            if (!NetUtil.isUsableLocalPort(config.getPort())) {
                JOptionPane.showMessageDialog(MAIN_FRAME, """
                                当前配置的端口已被占用.
                                请检查是否有程序占用端口或更换端口.""",
                        TITLE,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            isStarting.set(true);
            serverInterface.setStatusLabelText("生成缓存结构中", ModernColors.YELLOW);

            JsonCacheUtils jsonCacheUtils = new JsonCacheUtils(serverInterface, httpServerInterface, startOrStop);

            if (config.isCompatibleMode()) {
                jsonCacheUtils.updateDirCache(
                        FileUtil.loadJsonArrayFromFile(serverName, legacyResJsonFileExtensionName, logger),
                        HashCalculator.SHA1);
            }
            jsonCacheUtils.updateDirCacheAndStartServer(
                    FileUtil.loadJsonArrayFromFile(serverName, resJsonFileExtensionName, logger),
                    HashCalculator.CRC32);

            copyAddressButton.setVisible(true);
            openAddressButton.setVisible(true);

            isStarting.set(false);
        });
    }

    /**
     * 初始化 HTTP 服务器
     */
    protected void loadHttpServer() {
        serverInterface = new IntegratedServerInterface() {
            @Override
            public GUILogger getLogger() {
                return logger;
            }

            @Override
            public IntegratedServerConfig getIntegratedServerConfig() {
                return config;
            }

            @Override
            public JPanel getRequestListPanel() {
                return requestListPanel;
            }

            @Override
            public AtomicBoolean isGenerating() {
                return isGenerating;
            }

            @Override
            public AtomicBoolean isStarted() {
                return isStarted;
            }

            @Override
            public String getResJson() {
                return resJson;
            }

            @Override
            public void setResJson(String newResJson) {
                resJson = newResJson;
            }

            @Override
            public String getLegacyResJson() {
                return legacyResJson;
            }

            @Override
            public void setLegacyResJson(String newLegacyResJson) {
                legacyResJson = newLegacyResJson;
            }

            @Override
            public String getServerName() {
                return serverName;
            }

            @Override
            public String getResJsonFileExtensionName() {
                return resJsonFileExtensionName;
            }

            @Override
            public String getLegacyResJsonFileExtensionName() {
                return legacyResJsonFileExtensionName;
            }

            @Override
            public SmoothProgressBar getStatusProgressBar() {
                return statusProgressBar;
            }

            @Override
            public String getIndexJson() {
                return indexJson;
            }

            @Override
            public void regenCache() {
                regenResCache();
            }

            @Override
            public boolean stopServer() {
                return IntegratedServer.this.stopServer();
            }

            @Override
            public void saveConfig() {
                saveConfigurationToFile();
            }

            @Override
            public void setStatusLabelText(String text, Color fg) {
                statusLabel.setText(String.format("状态: %s", text));
                statusLabel.setForeground(fg);
            }

            @Override
            public void resetStatusProgressBar() {
                IntegratedServer.this.resetStatusProgressBar();
            }
        };
        server = new HttpServer(serverInterface);
        httpServerInterface = () -> server.start();
    }

    /**
     * 重新生成缓存
     */
    protected void regenResCache() {
        ThreadUtil.execute(() -> {
            serverInterface.setStatusLabelText("生成缓存结构中", ModernColors.YELLOW);

            JsonCacheUtils jsonCacheUtil = new JsonCacheUtils(serverInterface, httpServerInterface, startOrStop);

            if (config.isCompatibleMode()) {
                jsonCacheUtil.updateDirCache(
                        FileUtil.loadJsonArrayFromFile(serverName, legacyResJsonFileExtensionName, logger),
                        HashCalculator.SHA1);
            }
            jsonCacheUtil.updateDirCache(
                    FileUtil.loadJsonArrayFromFile(serverName, resJsonFileExtensionName, logger),
                    HashCalculator.CRC32);

            System.gc();
            logger.info("内存已完成回收.");

            if (isStarted.get()) {
                serverInterface.setStatusLabelText("已启动", ModernColors.GREEN);
            } else {
                serverInterface.setStatusLabelText("就绪", ModernColors.BLUE);
            }
        });
    }

    /**
     * 关闭服务器
     *
     * @return 是否关闭成功
     */
    protected boolean stopServer() {
        try {
            server.stop();
            isStarted.set(false);
            copyAddressButton.setVisible(false);
            openAddressButton.setVisible(false);

            startOrStop.setText("重载配置并启动服务器");

            return true;
        } catch (Exception ex) {
            logger.error("无法正常关闭服务器", ex);
            return false;
        }
    }

    /**
     * 保存配置文件至磁盘
     */
    protected void saveConfigurationToFile() {
        reloadConfigurationFromGUI();
        try {
            ConfigurationManager.saveConfigurationToFile(config, "./", serverName + configFileSuffix.replace(".json", ""));
            logger.info("已保存配置至磁盘.");
        } catch (IORuntimeException ex) {
            logger.error("保存配置文件的时候出现了问题...", ex);
        }
    }

    /**
     * 从文件加载配置文件
     */
    protected void loadConfigurationFromFile() {
        if (!new File("./" + serverName + configFileSuffix).exists()) {
            try {
                logger.warn("未找到配置文件，正在尝试在程序当前目录生成配置文件...");
                ConfigurationManager.saveConfigurationToFile(new IntegratedServerConfig(), "./", String.format("%s.lscfg", serverName));
                logger.info("配置生成成功.");
                logger.info("目前正在使用程序默认配置.");
            } catch (Exception e) {
                logger.error("生成配置文件的时候出现了问题...", e);
                logger.info("目前正在使用程序默认配置.");
            }
            return;
        }
        try {
            ConfigurationManager.loadLittleServerConfigFromFile("./" + serverName + configFileSuffix, config);
        } catch (IOException e) {
            logger.error("加载配置文件的时候出现了问题...", e);
            logger.info("目前正在使用程序默认配置.");
            return;
        }
        //IP
        IPTextField.setText(config.getIp());
        //端口
        portSpinner.setValue(config.getPort());
        //资源文件夹
        mainDirTextField.setText(config.getMainDirPath());
        //实时文件监听器
        fileChangeListener.setSelected(config.isFileChangeListener());
        //旧版兼容模式
        compatibleMode.setSelected(config.isCompatibleMode());
        //Jks 证书
        if (!config.getJksFilePath().isEmpty()) {
            File JksSsl = new File(config.getJksFilePath());
            if (JksSsl.exists()) {
                JksSslTextField.setText(JksSsl.getName());
            } else {
                logger.warn("JKS 证书文件不存在.");
            }
        }
        //Jks 证书密码
        JksSslPassField.setText(config.getJksSslPassword());
        //普通模式
        commonModeList.clear();
        commonModeList.addAll(Arrays.asList(config.getCommonMode()));
        commonMode.setListData(config.getCommonMode());
        //补全模式
        onceModeList.clear();
        onceModeList.addAll(Arrays.asList(config.getOnceMode()));
        onceMode.setListData(config.getOnceMode());

        reloadIndexJson();

        logger.info("已载入配置文件.");
    }

    /**
     * 以面板当前配置重载配置
     */
    protected void reloadConfigurationFromGUI() {
        //IP 检查
        String IP = IPTextField.getText();
        String IPType = IPAddressUtil.checkAddress(IP);
        if (IPType == null) {
            IP = "0.0.0.0";
            config.setIp("0.0.0.0");
            logger.warn("配置中的 IP 格式错误，使用默认 IP 地址 0.0.0.0");
        }

        config.setPort((Integer) portSpinner.getValue()) //设置端口
                .setMainDirPath(mainDirTextField.getText()) //设置资源目录
                .setFileChangeListener(fileChangeListener.isSelected()) //设置实时文件监听器
                .setCompatibleMode(compatibleMode.isSelected()) //设置旧版兼容模式
                .setJksSslPassword(String.valueOf(JksSslPassField.getPassword())) //设置 Jks 证书密码
                .setCommonMode(commonModeList.toArray(new String[0])) //设置普通模式
                .setOnceMode(onceModeList.toArray(new String[0])) //设置补全模式
                .setIp(IP);

        reloadIndexJson();

        logger.info("已加载配置.");
    }

    /**
     * 重新构建 index.json 字符串缓存
     */
    protected void reloadIndexJson() {
        index.clear();
        index.put("update", config.getMainDirPath().replace("/", "").intern());
        index.put("hash_algorithm", HashCalculator.CRC32);
        index.put("common_mode", config.getCommonMode());
        index.put("once_mode", config.getOnceMode());

        indexJson = index.toJSONString(JSONWriter.Feature.PrettyFormat);
    }

    protected JPanel loadStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(1, 4, 1, 0)));

        //设置字体颜色
        statusLabel.setForeground(ModernColors.BLUE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 1));

        //状态栏进度条
        statusProgressBar.setVisible(false);
        statusProgressBar.setStringPainted(true);
        statusProgressBar.setBorder(new EmptyBorder(4, 25, 4, 25));

        //隐藏/显示 控制面板
        JButton showOrHideControlPanel = new JButton("隐藏控制面板");
        showOrHideControlPanel.addActionListener(new ShowOrHideComponentActionListener(
                controlPanel, "控制面板", showOrHideControlPanel));

        //隐藏/显示 上传列表
        JButton showOrHideRequestListPanel = new JButton("显示上传列表");
        showOrHideRequestListPanel.addActionListener(new ShowOrHideComponentActionListener(
                requestListFrame, "上传列表", showOrHideRequestListPanel
        ));
        requestListFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showOrHideRequestListPanel.setText("显示上传列表");
            }
        });

        //复制 API 地址
        copyAddressButton.setVisible(false);
        copyAddressButton.addActionListener(e -> {
            ClipboardUtil.setStr(server.getAPIAddress());
            JOptionPane.showMessageDialog(MAIN_FRAME,
                    "已成功复制到剪贴板！", TITLE,
                    JOptionPane.INFORMATION_MESSAGE);
        });

        //在浏览器中打开 API 地址
        openAddressButton.setVisible(false);
        openAddressButton.addActionListener(e -> MiscUtils.openLinkInBrowser(server.getAPIAddress()));

        //组装按钮面板
        buttonPanel.add(showOrHideControlPanel);
        buttonPanel.add(showOrHideRequestListPanel);
        buttonPanel.add(copyAddressButton);
        buttonPanel.add(openAddressButton);

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(statusProgressBar, BorderLayout.CENTER);
        statusPanel.add(buttonPanel, BorderLayout.EAST);

        return statusPanel;
    }

    protected Component loadControlPanel() {
        //控制面板
        controlPanel.setPreferredSize(new Dimension(350, 0));
        controlPanel.setBorder(new TitledBorder("控制面板"));

        //配置窗口
        JPanel configPanel = new JPanel(new VFlowLayout());

        //IP 端口
        configPanel.add(loadIPPortBox());
        //资源文件夹
        configPanel.add(loadMainDirBox());
        //SSL 证书框，仅用于显示。实际操作为右方按钮。
        configPanel.add(loadJksSslBox());
        //Jks 证书密码框
        configPanel.add(loadJksSslPassBox());
        //额外功能
        configPanel.add(loadExtraFeaturesPanel());
        //普通模式
        configPanel.add(loadCommonModePanel());
        //补全模式
        configPanel.add(loadOnceModePanel());

        JPanel southControlPanel = new JPanel(new VFlowLayout());
        JLabel tipLabel = new JLabel("注意：配置修改后, 请点击保存配置按钮以应用配置.");
        tipLabel.setForeground(ModernColors.RED);
        southControlPanel.add(tipLabel);

        //存储当前配置
        JButton saveConfigButton = new JButton("保存并重载配置");
        saveConfigButton.setToolTipText("以控制面板当前的配置应用到服务器, 并保存配置到磁盘.");
        southControlPanel.add(saveConfigButton);
        saveConfigButton.addActionListener(e -> saveConfigurationToFile());

        //重新生成资源文件夹缓存
        JButton regenDirectoryStructureCache = new JButton("重新生成资源文件夹缓存");
        southControlPanel.add(regenDirectoryStructureCache);
        regenDirectoryStructureCache.addActionListener(e -> {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在生成资源缓存, 请稍后再试。", BalloonServer.TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            regenResCache();
        });

        //启动/关闭 服务器
        startOrStop.addActionListener(e -> {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在生成资源缓存, 请稍后再试。", BalloonServer.TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isStarting.get()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在启动服务器, 请稍后再试。", BalloonServer.TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            //如果启动则关闭服务器
            if (!isStarted.get()) {
                saveConfigurationToFile();
                startServer();
            } else {
                stopServer();
            }
        });
        southControlPanel.add(startOrStop);

        JScrollPane configScroll = new JScrollPane(configPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        configScroll.getVerticalScrollBar().setUnitIncrement(15);
        configScroll.setBorder(new TitledBorder("程序配置"));

        //组装控制面板
        controlPanel.add(configScroll);
        controlPanel.add(southControlPanel, BorderLayout.SOUTH);

        return controlPanel;
    }

    protected void loadRequestList() {
        //上传列表
        requestListScrollPane.getVerticalScrollBar().setUnitIncrement(50);
        requestListScrollPane.setBorder(new TitledBorder("上传列表"));

        requestListFrame.add(requestListScrollPane);

        requestListFrame.setTitle(StrUtil.format("{} 的上传列表", serverName));
        requestListFrame.setIconImage(SvgIcons.DEFAULT_SERVER_ICON.getImage());
        requestListFrame.setMinimumSize(new Dimension(400,750));
        requestListFrame.setResizable(false);
        requestListFrame.setLocationRelativeTo(null);
    }

    protected Box loadJksSslBox() {
        //SSL 证书框，仅用于显示。实际操作为右方按钮。
        Box JksSslBox = Box.createHorizontalBox();
        JksSslBox.add(new JLabel("JKS 证书文件:"));
        JksSslTextField.setEditable(false);
        JButton selectJksSslFile = new JButton("...");

        selectJksSslFile.addActionListener(e -> ThreadUtil.execute(() -> {
            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setFileFilter(new FileUtil.SimpleFileFilter(new String[]{"jks"}, null, "JKS 证书 (*.jks)"));

            if (fileChooser.showDialog(littleServerPanel, "载入证书") == JFileChooser.APPROVE_OPTION) {
                File JKS = fileChooser.getSelectedFile();
                if (JKS != null && JKS.exists()) {
                    config.setJksFilePath(JKS.getPath());
                    JksSslTextField.setText(JKS.getName());
                    logger.info("已载入证书 " + JKS.getName());
                }
            }
        }));

        JksSslBox.add(JksSslTextField);
        JksSslBox.add(selectJksSslFile);

        return JksSslBox;
    }
}
