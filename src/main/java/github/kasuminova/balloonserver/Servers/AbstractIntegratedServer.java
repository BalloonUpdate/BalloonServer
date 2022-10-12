package github.kasuminova.balloonserver.Servers;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.Configurations.ConfigurationManager;
import github.kasuminova.balloonserver.Configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.GUI.LayoutManager.VFlowLayout;
import github.kasuminova.balloonserver.GUI.RuleEditor;
import github.kasuminova.balloonserver.HTTPServer.HttpServer;
import github.kasuminova.balloonserver.HTTPServer.HttpServerInterface;
import github.kasuminova.balloonserver.Utils.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static github.kasuminova.balloonserver.BalloonServer.*;
import static github.kasuminova.balloonserver.Utils.SvgIcons.*;

/**
 * 可继承 IntegratedServer 面板实例
 */
public abstract class AbstractIntegratedServer {
    protected HttpServer server;
    protected String resJson;
    protected String resJsonFileExtensionName = "res-cache";
    protected String hashAlgorithm;
    protected String indexJson = null;
    protected String configFileSuffix;
    protected final JLabel statusLabel = new JLabel("状态：就绪", JLabel.LEFT);
    protected final JProgressBar statusProgressBar = new JProgressBar(0, 1000);
    protected final JButton copyAddressButton = new JButton("复制 API 地址");
    protected final JButton openAddressButton = new JButton("在浏览器中打开 API 地址");
    protected final String serverName;
    protected final GUILogger logger;
    protected final IntegratedServerConfig config = new IntegratedServerConfig();
    protected final List<String> commonModeList = new ArrayList<>();
    protected final List<String> onceModeList = new ArrayList<>();
    //服务器启动状态
    protected final AtomicBoolean isStarted = new AtomicBoolean(false);
    protected final AtomicBoolean isStarting = new AtomicBoolean(false);
    //服务端是否在生成缓存，防止同一时间多个线程生成缓存导致程序混乱
    protected final AtomicBoolean isGenerating = new AtomicBoolean(false);
    protected final JPanel requestListPanel = new JPanel(new VFlowLayout());
    protected final JScrollPane requestListScrollPane = new JScrollPane(
            requestListPanel,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    protected final JPanel littleServerPanel = new JPanel(new BorderLayout());
    protected final JPanel controlPanel = new JPanel(new BorderLayout());
    protected final JButton startOrStop = new JButton("保存配置并启动服务器");
    //IP 输入框
    protected final JTextField IPTextField = new JTextField("0.0.0.0");
    //端口输入框
    protected final JSpinner portSpinner = new JSpinner();
    //Jks 证书密码
    protected final JPasswordField JksSslPassField = new JPasswordField();
    //资源文件夹输入框
    protected final JTextField mainDirTextField = new JTextField("/res");
    //实时文件监听
    protected final JCheckBox fileChangeListener = new JCheckBox("启用实时文件监听");
    //证书文件名（不可编辑）
    protected final JTextField JksSslTextField = new JTextField("请选择证书文件");
    //普通模式
    protected final JList<String> commonMode = new JList<>();
    //补全模式
    protected final JList<String> onceMode = new JList<>();
    protected IntegratedServerInterface serverInterface;
    protected HttpServerInterface httpServerInterface;

    /**
     * 返回当前实例的面板
     * @return 服务器面板
     */
    public JPanel getPanel() {
        return littleServerPanel;
    }

    /**
     * 返回当前服务器实例的接口
     * @return LittleServerInterface
     */
    public IntegratedServerInterface getServerInterface() {
        return serverInterface;
    }

    /**
     * 返回当前服务器实例的 HTTP 服务器接口
     * @return HttpServerInterface
     */
    public HttpServerInterface getHttpServerInterface() {
        return httpServerInterface;
    }

    /**
     * 创建一个服务器面板，并绑定一个新的服务器实例
     * @param serverName LittleServerConfig 配置文件路径
     * @param autoStart 是否在创建对象后自动启动服务器
     */
    public AbstractIntegratedServer(String serverName, boolean autoStart, String configFileSuffix) {
        this.serverName = serverName;
        this.configFileSuffix = configFileSuffix;

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
                logPane.getDocument().remove(0,logPane.getDocument().getLength());
                logger.info("已清空当前服务器实例日志.");
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

        //控制面板
        controlPanel.setBorder(new TitledBorder("控制面板"));
        controlPanel.setPreferredSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.25), 0));

        //配置窗口
        JPanel configPanel = new JPanel(new VFlowLayout());
        configPanel.setBorder(new TitledBorder("程序配置"));

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

        //载入配置文件并初始化 HTTP 服务端
        loadConfigurationFromFile();
        loadHttpServer();

        JPanel southControlPanel = new JPanel(new VFlowLayout());
        JLabel tipLabel = new JLabel("上方配置修改后，请点击保存配置按钮来载入配置.");
        tipLabel.setForeground(new Color(255,75,75));
        southControlPanel.add(tipLabel);

        //存储当前配置
        JButton saveConfigButton = new JButton("保存并重载配置");
        saveConfigButton.setToolTipText("以控制面板当前的配置应用到服务器，并保存配置到磁盘.");
        southControlPanel.add(saveConfigButton);
        saveConfigButton.addActionListener(e -> saveConfigurationToFile());

        //重新生成资源文件夹缓存
        JButton regenDirectoryStructureCache = new JButton("重新生成资源文件夹缓存");
        southControlPanel.add(regenDirectoryStructureCache);
        regenDirectoryStructureCache.addActionListener(e -> {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在生成资源缓存，请稍后再试。", BalloonServer.TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            regenResCache();
        });

        //启动/关闭 服务器
        startOrStop.addActionListener(e -> {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在生成资源缓存，请稍后再试。", BalloonServer.TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isStarting.get()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在启动服务器，请稍后再试。", BalloonServer.TITLE,
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

        //上传列表
        requestListScrollPane.getVerticalScrollBar().setUnitIncrement(50);
        requestListScrollPane.setBorder(new TitledBorder("上传列表"));
        requestListScrollPane.setPreferredSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.225), littleServerPanel.getHeight()));
        //组装控制面板
        controlPanel.add(configPanel);
        controlPanel.add(southControlPanel, BorderLayout.SOUTH);

        //服务器窗口组装
        //控制面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(controlPanel, BorderLayout.WEST);
        //上传列表
        rightPanel.add(requestListScrollPane, BorderLayout.EAST);

        //组装主面板
        littleServerPanel.add(logPanel, BorderLayout.CENTER);
        littleServerPanel.add(rightPanel, BorderLayout.EAST);
        //载入实例状态栏
        littleServerPanel.add(loadStatusBar(), BorderLayout.SOUTH);
        logger.debug(String.format("载入服务器耗时 %sms.", System.currentTimeMillis() - start));

        if (autoStart) {
            logger.info("检测到自动启动服务器选项已开启，正在启动服务器...");
            startServer();
        }
    }

    //更新规则编辑器类
    protected class RuleEditorActionListener implements ActionListener {
        protected final JList<String> ruleList;
        protected final List<String> rules;
        public RuleEditorActionListener(JList<String> ruleList, List<String> rules) {
            this.ruleList = ruleList;
            this.rules = rules;
        }
        protected void showRuleEditorDialog(JSONArray jsonArray) {
            //锁定窗口，防止用户误操作
            MAIN_FRAME.setEnabled(false);
            RuleEditor editorDialog = new RuleEditor(jsonArray, rules);
            editorDialog.setModal(true);

            MAIN_FRAME.setEnabled(true);
            editorDialog.setVisible(true);

            ruleList.setListData(rules.toArray(new String[0]));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在生成资源缓存，请稍后再试。",
                        BalloonServer.TITLE,
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (new File(String.format("./%s.%s.json", serverName, resJsonFileExtensionName)).exists()) {
                int selection = JOptionPane.showConfirmDialog(MAIN_FRAME,
                        "检测到本地 JSON 缓存，是否以 JSON 缓存启动规则编辑器？",
                        BalloonServer.TITLE, JOptionPane.YES_NO_OPTION);
                if (!(selection == JOptionPane.YES_OPTION)) return;

                try {
                    String json = FileUtil.readStringFromFile(new File(String.format("./%s.%s.json", serverName, resJsonFileExtensionName)));
                    showRuleEditorDialog(JSONArray.parseArray(json));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(MAIN_FRAME,
                            "无法读取本地 JSON 缓存" + ex, BalloonServer.TITLE,
                            JOptionPane.ERROR_MESSAGE);
                }
                return;
            }

            int selection = JOptionPane.showConfirmDialog(MAIN_FRAME,
                    "未检测到 JSON 缓存，是否立即生成 JSON 缓存并启动规则编辑器？",
                    BalloonServer.TITLE, JOptionPane.YES_NO_OPTION);
            if (!(selection == JOptionPane.YES_OPTION)) return;

            GLOBAL_THREAD_POOL.execute(() -> {
                new CacheUtils(serverInterface,httpServerInterface,startOrStop).updateDirCache(null);
                if (new File(String.format("./%s.%s.json", serverName, resJsonFileExtensionName)).exists()) {
                    try {
                        String json = FileUtil.readStringFromFile(new File(String.format("./%s.%s.json", serverName, resJsonFileExtensionName)));
                        showRuleEditorDialog(JSONArray.parseArray(json));

                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(MAIN_FRAME,
                                "无法读取本地 JSON 缓存\n" + ex, BalloonServer.TITLE,
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
    }

    /**
     * 启动服务器
     */
    protected void startServer() {
        GLOBAL_THREAD_POOL.execute(() -> {
            isStarting.set(true);
            serverInterface.setStatusLabelText("状态：启动中", ModernColors.YELLOW);

            File jsonCache = new File(String.format("./%s.%s.json", serverName, resJsonFileExtensionName));

            CacheUtils cacheUtil = new CacheUtils(serverInterface, httpServerInterface, startOrStop);
            JSONArray jsonArray = new JSONArray();

            if (jsonCache.exists()) {
                try {
                    String jsonString = FileUtil.readStringFromFile(jsonCache);
                    jsonArray.addAll(JSONArray.parseArray(jsonString));
                } catch (Exception e) {
                    logger.error("读取缓存文件的时候出现了一些问题...", e);
                    logger.warn("缓存文件读取失败, 重新生成缓存...");
                }
            }

            if (jsonArray.isEmpty()) {
                cacheUtil.updateDirCacheAndStartServer(null);
            } else {
                cacheUtil.updateDirCacheAndStartServer(jsonArray);
            }

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
            public IntegratedServerConfig getConfig() {
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
            public String getServerName() {
                return serverName;
            }

            @Override
            public String getResJsonFileExtensionName() {
                return resJsonFileExtensionName;
            }

            @Override
            public JProgressBar getStatusProgressBar() {
                return statusProgressBar;
            }

            @Override
            public String getIndexJson() {
                return indexJson;
            }

            @Override
            public void setResJson(String newResJson) {
                resJson = newResJson;
            }

            @Override
            public void regenCache() {
                regenResCache();
            }

            @Override
            public boolean stopServer() {
                return AbstractIntegratedServer.this.stopServer();
            }

            @Override
            public void saveConfig() {
                saveConfigurationToFile();
            }

            @Override
            public String getHashAlgorithm() {
                return hashAlgorithm;
            }

            @Override
            public void setStatusLabelText(String text, Color fg) {
                statusLabel.setText(text);
                statusLabel.setForeground(fg);
            }

            @Override
            public void resetStatusProgressBar() {
                AbstractIntegratedServer.this.resetStatusProgressBar();
            }
        };
        server = new HttpServer(serverInterface);
        httpServerInterface = () -> server.start();
    }

    /**
     * 重新生成缓存
     */
    protected void regenResCache() {
        GLOBAL_THREAD_POOL.execute(() -> {
            File jsonCache = new File(String.format("./%s.%s.json", serverName, resJsonFileExtensionName));
            CacheUtils cacheUtil = new CacheUtils(serverInterface, httpServerInterface, startOrStop);
            if (jsonCache.exists()) {
                try {
                    String jsonString = FileUtil.readStringFromFile(jsonCache);
                    JSONArray jsonArray = JSONArray.parseArray(jsonString);
                    cacheUtil.updateDirCache(jsonArray);
                } catch (Exception ex) {
                    logger.error("读取缓存文件的时候出现了一些问题...", ex);
                    logger.warn("缓存文件读取失败, 重新生成缓存...");
                    cacheUtil.updateDirCache(null);
                }
            } else {
                cacheUtil.updateDirCache(null);
            }
        });
    }

    /**
     * 关闭服务器
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
            ConfigurationManager.saveConfigurationToFile(config,"./",serverName + configFileSuffix.replace(".json", ""));
            logger.info("已保存配置至磁盘.");
        } catch (IOException ex) {
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

        logger.info("已载入配置文件.");
    }

    protected final JSONObject index = new JSONObject();
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
                .setJksSslPassword(String.valueOf(JksSslPassField.getPassword())) //设置 Jks 证书密码
                .setCommonMode(commonModeList.toArray(new String[0])) //设置普通模式
                .setOnceMode(onceModeList.toArray(new String[0])) //设置补全模式
                .setIp(IP);

        index.clear();
        index.put("update", config.getMainDirPath().replace("/", "").intern());
        index.put("hash_algorithm", hashAlgorithm.intern());
        index.put("common_mode", config.getCommonMode());
        index.put("once_mode", config.getOnceMode());

        //重新构建 index.json 字符串缓存
        indexJson = index.toJSONString(JSONWriter.Feature.PrettyFormat);

        logger.info("已加载配置.");
    }

    protected JPanel loadStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(2, 4, 2, 4)));

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
        JButton showOrHideRequestListPanel = new JButton("隐藏上传列表");
        showOrHideRequestListPanel.addActionListener(new ShowOrHideComponentActionListener(
                requestListScrollPane, "上传列表", showOrHideRequestListPanel
        ));

        //复制 API 地址
        copyAddressButton.setVisible(false);
        copyAddressButton.addActionListener(e -> {
            MiscUtils.setClipboardString(server.getAPIAddress());
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

    /**
     * 重置主窗口状态栏进度条
     */
    public void resetStatusProgressBar() {
        statusProgressBar.setVisible(false);
        statusProgressBar.setIndeterminate(false);
        statusProgressBar.setValue(0);
    }

    protected Box loadIPPortBox() {
        //IP 配置
        Box IPPortBox = Box.createHorizontalBox();
        IPPortBox.add(new JLabel("监听 IP："));
        IPTextField.putClientProperty("JTextField.showClearButton", true);
        IPPortBox.add(IPTextField);
        //端口配置
        SpinnerNumberModel portSpinnerModel = new SpinnerNumberModel(8080,1,65535,1);
        portSpinner.setModel(portSpinnerModel);
        JSpinner.NumberEditor portSpinnerEditor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(portSpinnerEditor);
        IPPortBox.add(new JLabel(" 端口："));
        IPPortBox.add(portSpinner);

        return IPPortBox;
    }

    protected Box loadMainDirBox() {
        //资源文件夹
        Box mainDirBox = Box.createHorizontalBox();
        JLabel mainDirLabel = new JLabel("资源文件夹：");
        mainDirTextField.putClientProperty("JTextField.showClearButton", true);
        mainDirTextField.setToolTipText("""
                        仅支持程序当前目录下的文件夹或子文件夹，请勿输入其他文件夹。
                        默认为 /res , 也可输入其他文件夹, 如 /resources、/content、/.minecraft 等.""");
        mainDirBox.add(mainDirLabel);
        mainDirBox.add(mainDirTextField);

        return mainDirBox;
    }

    protected Box loadJksSslBox() {
        //SSL 证书框，仅用于显示。实际操作为右方按钮。
        Box JksSslBox = Box.createHorizontalBox();
        JksSslBox.add(new JLabel("JKS 证书文件："));
        JksSslTextField.setEditable(false);
        JButton selectJksSslFile = new JButton("...");

        selectJksSslFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setFileFilter(new FileUtil.SimpleFileFilter(new String[]{"jks"}, null,"JKS 证书 (*.jks)"));

            if (fileChooser.showDialog(littleServerPanel, "载入证书") == JFileChooser.APPROVE_OPTION) {
                File JKS = fileChooser.getSelectedFile();
                if (JKS != null && JKS.exists()) {
                    config.setJksFilePath(JKS.getPath());
                    JksSslTextField.setText(JKS.getName());
                    logger.info("已载入证书 " + JKS.getName());
                }
            }
        });

        JksSslBox.add(JksSslTextField);
        JksSslBox.add(selectJksSslFile);

        return JksSslBox;
    }

    protected Box loadJksSslPassBox() {
        Box JksSslPassBox = Box.createHorizontalBox();
        JksSslPassBox.add(new JLabel("JKS 证书密码："));
        JksSslPassBox.add(JksSslPassField);
        return JksSslPassBox;
    }

    protected JPanel loadCommonModePanel() {
        //普通更新模式
        JPanel commonModePanel = new JPanel(new VFlowLayout());
        commonModePanel.setBorder(new TitledBorder("普通更新模式"));
        commonMode.setVisibleRowCount(6);
        JScrollPane common_ModeScrollPane = new JScrollPane(
                commonMode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        commonModePanel.add(common_ModeScrollPane);

        //菜单
        JPopupMenu commonModeMenu = new JPopupMenu();
        JMenuItem openCommonModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        openCommonModeRuleEditor.setIcon(EDIT_ICON);
        //普通更新规则编辑器
        openCommonModeRuleEditor.addActionListener(new RuleEditorActionListener(commonMode, commonModeList));
        commonModeMenu.add(openCommonModeRuleEditor);
        commonModeMenu.addSeparator();
        //添加更新规则
        JMenuItem addNewCommonRule = new JMenuItem("添加更新规则");
        addNewCommonRule.setIcon(PLUS_ICON);
        addNewCommonRule.addActionListener(new AddUpdateRule(commonMode, commonModeList, MAIN_FRAME));
        commonModeMenu.add(addNewCommonRule);
        //删除指定规则
        JMenuItem deleteCommonRule = new JMenuItem("删除选定的规则");
        deleteCommonRule.setIcon(REMOVE_ICON);
        deleteCommonRule.addActionListener(new DeleteUpdateRule(commonMode, commonModeList, MAIN_FRAME));
        commonModeMenu.add(deleteCommonRule);
        //鼠标监听
        commonMode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()){
                    commonModeMenu.show(commonMode,e.getX(),e.getY());
                }
            }
        });

        return commonModePanel;
    }

    protected JPanel loadOnceModePanel() {
        //补全更新模式
        JPanel onceModePanel = new JPanel(new VFlowLayout());
        onceModePanel.setBorder(new TitledBorder("补全更新模式"));
        onceMode.setVisibleRowCount(6);
        JScrollPane once_ModeScrollPane = new JScrollPane(
                onceMode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        onceModePanel.add(once_ModeScrollPane);

        //菜单
        JPopupMenu onceModeMenu = new JPopupMenu();
        JMenuItem openOnceModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        openOnceModeRuleEditor.setIcon(EDIT_ICON);
        //补全更新规则编辑器
        openOnceModeRuleEditor.addActionListener(new RuleEditorActionListener(onceMode, onceModeList));
        onceModeMenu.add(openOnceModeRuleEditor);
        onceModeMenu.addSeparator();
        //添加更新规则
        JMenuItem addNewOnceRule = new JMenuItem("添加更新规则");
        addNewOnceRule.setIcon(PLUS_ICON);
        addNewOnceRule.addActionListener(new AddUpdateRule(onceMode,onceModeList, MAIN_FRAME));
        onceModeMenu.add(addNewOnceRule);
        //删除指定规则
        JMenuItem deleteOnceRule = new JMenuItem("删除选定的规则");
        deleteOnceRule.setIcon(REMOVE_ICON);
        deleteOnceRule.addActionListener(new DeleteUpdateRule(onceMode,onceModeList, MAIN_FRAME));
        //鼠标监听
        onceMode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()){
                    onceModeMenu.show(onceMode,e.getX(),e.getY());
                }
            }
        });
        onceModeMenu.add(deleteOnceRule);

        return onceModePanel;
    }

    protected JPanel loadExtraFeaturesPanel() {
        JPanel extraFeaturesPanel = new JPanel(new BorderLayout());
        //实时文件监听
        fileChangeListener.setToolTipText("""
                        开启后，启动服务器的同时会启动文件监听服务.
                        文件监听服务会每隔 5 - 7 秒会监听资源文件夹的变化，如果资源一有变化会立即重新生成资源缓存.
                        注意：不推荐在超大文件夹(10000 文件/文件夹 以上)上使用此功能，可能会造成 I/O 卡顿.""");
        fileChangeListener.setSelected(true);
        extraFeaturesPanel.add(fileChangeListener,BorderLayout.WEST);

        return extraFeaturesPanel;
    }
}
