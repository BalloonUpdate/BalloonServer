package github.kasuminova.balloonserver.Servers;

import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.ConfigurationManager;
import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.GUI.CheckBoxTree.RuleEditor;
import github.kasuminova.balloonserver.GUI.VFlowLayout;
import github.kasuminova.balloonserver.HTTPServer.HttpServer;
import github.kasuminova.balloonserver.HTTPServer.HttpServerInterface;
import github.kasuminova.balloonserver.Utils.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static github.kasuminova.balloonserver.BalloonServer.*;

/**
 * LittleServer 面板实例，支持多实例化
 * @author Kasumi_Nova
 */
public class LittleServer {
    private LittleServerConfig config;
    private HttpServer server;
    private String resJson;
    private static final ExecutorService GLOBAL_THREAD_POOL = Executors.newCachedThreadPool();
    private final List<String> commonModeList = new ArrayList<>();
    private final List<String> onceModeList = new ArrayList<>();
    //服务器启动状态
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    //服务端是否在生成缓存，防止同一时间多个线程生成缓存导致程序混乱
    private final AtomicBoolean isGenerating = new AtomicBoolean(false);
    private final GUILogger logger;
    private final JPanel requestListPanel = new JPanel(new VFlowLayout());
    private final JPanel littleServerPanel = new JPanel(new BorderLayout());
    private final JButton startOrStop = new JButton("重载配置并启动服务器");
    private LittleServerInterface serverInterface;
    private HttpServerInterface httpServerInterface;
    /**
     * 返回当前实例的面板
     * @return 服务器面板
     */
    public JPanel getPanel() {
        return littleServerPanel;
    }
    /**
     * 创建一个服务器面板，并绑定一个新的服务器实例
     * @param configFilePath LittleServerConfig 配置文件路径
     */
    public LittleServer(String configFilePath) {
        long start = System.currentTimeMillis();
        //设置 Logger，主体为 logPanel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("程序日志"));
        JTextPane logPane = new JTextPane();
        logPane.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logPane);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        //初始化 Logger
        logger = new GUILogger("", logPane);

        //控制面板
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("控制面板"));
        controlPanel.setPreferredSize(new Dimension((int) (frame.getWidth() * 0.27), frame.getHeight()));

        //配置窗口
        JPanel configPanel = new JPanel(new VFlowLayout());
        configPanel.setBorder(new TitledBorder("程序配置"));

        //IP 配置
        Box IPPortBox = Box.createHorizontalBox();
        IPPortBox.add(new JLabel("监听 IP："));
        JTextField IPTextField = new JTextField("0.0.0.0");
        IPPortBox.add(IPTextField);
        //端口配置
        SpinnerNumberModel portSpinnerModel = new SpinnerNumberModel(8080,1,65535,1);
        JSpinner portSpinner = new JSpinner(portSpinnerModel);
        JSpinner.NumberEditor portSpinnerEditor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(portSpinnerEditor);
        IPPortBox.add(new JLabel(" 端口："));
        IPPortBox.add(portSpinner);

        configPanel.add(IPPortBox);

        //资源文件夹
        Box mainDirBox = Box.createHorizontalBox();
        JLabel mainDirLabel = new JLabel("资源文件夹：");
        JTextField mainDirTextField = new JTextField("/res");
        mainDirTextField.setToolTipText(
                "仅支持程序当前目录下的文件夹或子文件夹，请勿输入其他文件夹。\n" +
                        "默认为 /res , 也可输入其他文件夹, 如 /resource、/content/res 等.");
        mainDirBox.add(mainDirLabel);
        mainDirBox.add(mainDirTextField);
        configPanel.add(mainDirBox);

        /*
          SSL 证书框，仅用于显示。实际操作为右方按钮。
         */
        Box JksSslBox = Box.createHorizontalBox();
        JksSslBox.add(new JLabel("JKS 证书文件："));
        JTextField JksSslTextField = new JTextField("请选择证书文件");
        JksSslTextField.setEditable(false);
        JButton selectJksSslFile = new JButton("...");

        selectJksSslFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setFileFilter(new FileUtil.SimpleFileFilter(new String[]{"jks"}, "JKS 证书 (*.jks)"));

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

        Box JksSslPassBox = Box.createHorizontalBox();
        JksSslPassBox.add(new JLabel("JKS 证书密码："));
        JPasswordField JksSslPassField = new JPasswordField();
        JksSslPassBox.add(JksSslPassField);
        configPanel.add(JksSslBox);
        configPanel.add(JksSslPassBox);

        //额外功能
        JPanel extraFeaturesPanel = new JPanel(new BorderLayout());
        //实时文件监听
        JCheckBox fileChangeListener = new JCheckBox("启用实时文件监听");
        fileChangeListener.setToolTipText("""
                        开启后，启动服务器的同时会启动文件监听服务.
                        文件监听服务会每隔 5 - 7 秒会监听资源文件夹的变化，如果资源一有变化会立即重新生成资源缓存.
                        注意：不推荐在超大文件夹(10000 文件/文件夹 以上)上使用此功能，可能会造成 I/O 卡顿.""");
        fileChangeListener.setSelected(true);
        extraFeaturesPanel.add(fileChangeListener,BorderLayout.WEST);
        configPanel.add(extraFeaturesPanel);

        /*
          普通更新模式和补全更新模式的 List 变动是实时监听的，无需重载配置文件。
         */
        //普通更新模式
        JPanel common_ModePanel = new JPanel(new VFlowLayout());
        common_ModePanel.setBorder(BorderFactory.createTitledBorder("普通更新模式"));
        JList<String> commonMode = new JList<>();
        commonMode.setVisibleRowCount(6);
        JScrollPane common_ModeScrollPane = new JScrollPane(
                commonMode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        common_ModePanel.add(common_ModeScrollPane);
        configPanel.add(common_ModePanel);

        //菜单
        JPopupMenu commonModeMenu = new JPopupMenu();
        JMenuItem openCommonModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        //普通更新规则编辑器
        openCommonModeRuleEditor.addActionListener(new RuleEditorActionListener(commonMode, commonModeList));
        commonModeMenu.add(openCommonModeRuleEditor);
        //添加更新规则
        JMenuItem addNewCommonRule = new JMenuItem("添加更新规则");
        addNewCommonRule.addActionListener(new AddUpdateRule(commonMode,commonModeList));
        commonModeMenu.add(addNewCommonRule);
        JMenuItem deleteCommonRule = new JMenuItem("删除选定的规则");
        //删除指定规则
        deleteCommonRule.addActionListener(new DeleteUpdateRule(commonMode,commonModeList));
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

        //补全更新模式
        JPanel once_ModePanel = new JPanel(new VFlowLayout());
        once_ModePanel.setBorder(BorderFactory.createTitledBorder("补全更新模式"));
        JList<String> onceMode = new JList<>();
        onceMode.setVisibleRowCount(6);
        JScrollPane once_ModeScrollPane = new JScrollPane(
                onceMode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        once_ModePanel.add(once_ModeScrollPane);

        //菜单
        JPopupMenu onceModeMenu = new JPopupMenu();
        JMenuItem openOnceModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        //补全更新规则编辑器
        openOnceModeRuleEditor.addActionListener(new RuleEditorActionListener(onceMode, onceModeList));
        onceModeMenu.add(openOnceModeRuleEditor);
        //添加更新规则
        JMenuItem addNewOnceRule = new JMenuItem("添加更新规则");
        addNewOnceRule.addActionListener(new AddUpdateRule(onceMode,onceModeList));
        onceModeMenu.add(addNewOnceRule);
        JMenuItem deleteOnceRule = new JMenuItem("删除选定的规则");
        //删除指定规则
        deleteOnceRule.addActionListener(new DeleteUpdateRule(onceMode,onceModeList));
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
        configPanel.add(once_ModePanel);

        //载入配置文件并初始化 HTTP 服务端
        loadConfigurationFromFile(
                configFilePath,
                IPTextField,
                portSpinner,
                JksSslTextField,
                JksSslPassField,
                mainDirTextField,
                commonMode,
                onceMode,
                fileChangeListener);
        loadHttpServer();

        JPanel southControlPanel = new JPanel(new VFlowLayout());
        JLabel tipLabel = new JLabel("上方配置修改后，请点击保存配置按钮来载入配置.");
        tipLabel.setForeground(new Color(255,64,64));
        southControlPanel.add(tipLabel);

        //存储当前配置
        JButton saveConfigButton = new JButton("保存配置");
        saveConfigButton.setToolTipText("以控制面板当前的配置应用到服务器，并保存配置到磁盘.");
        southControlPanel.add(saveConfigButton);
        saveConfigButton.addActionListener(e -> {
            reloadConfigurationFromGUI(
                    IPTextField,
                    portSpinner,
                    JksSslPassField,
                    mainDirTextField,
                    fileChangeListener,
                    commonModeList,
                    onceModeList,
                    config,
                    logger);
            try {
                ConfigurationManager.saveConfigurationToFile(config,"./","littleserver");
                logger.info("已保存配置至磁盘.");
            } catch (IOException ex) {
                logger.error("保存配置文件的时候出现了问题...", ex);
            }
        });

        //重新生成资源文件夹缓存
        JButton regenDirectoryStructureCache = new JButton("重新生成资源文件夹缓存");
        southControlPanel.add(regenDirectoryStructureCache);
        regenDirectoryStructureCache.addActionListener(e -> {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(frame,
                        "当前正在生成资源缓存，请稍后再试。","注意",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            File jsonCache = new File("./res-cache.json");
            CacheUtils cacheUtil = new CacheUtils(serverInterface, httpServerInterface, startOrStop);
            if (jsonCache.exists()) {
                try {
                    String jsonString = FileUtil.readStringFromFile(jsonCache);
                    JSONArray jsonArray = JSONArray.parseArray(jsonString);
                    GLOBAL_THREAD_POOL.execute(() -> cacheUtil.updateDirCache(jsonArray));
                } catch (Exception ex) {
                    logger.error("读取缓存文件的时候出现了一些问题...", ex);
                    logger.warn("缓存文件读取失败, 重新生成缓存...");
                    GLOBAL_THREAD_POOL.execute(() -> cacheUtil.updateDirCache(null));
                }
            } else {
                GLOBAL_THREAD_POOL.execute(() -> cacheUtil.updateDirCache(null));
            }
        });

        //启动服务器
        startOrStop.addActionListener(e -> {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(frame,
                        "当前正在生成资源缓存，请稍后再试。","注意",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            //如果启动则关闭服务器
            if (!isStarted.get()) {
                reloadConfigurationFromGUI(
                        IPTextField,
                        portSpinner,
                        JksSslPassField,
                        mainDirTextField,
                        fileChangeListener,
                        commonModeList,
                        onceModeList,
                        config,
                        logger);
                File jsonCache = new File("./res-cache.json");

                CacheUtils cacheUtil = new CacheUtils(serverInterface, httpServerInterface, startOrStop);
                if (jsonCache.exists()) {
                    try {
                        String jsonString = FileUtil.readStringFromFile(jsonCache);
                        JSONArray jsonArray = JSONArray.parseArray(jsonString);
                        GLOBAL_THREAD_POOL.execute(() -> cacheUtil.genResDirCacheAndStartServer(jsonArray));
                    } catch (Exception ex) {
                        logger.error("读取缓存文件的时候出现了一些问题...", ex);
                        logger.warn("缓存文件读取失败, 重新生成缓存...");
                        GLOBAL_THREAD_POOL.execute(() -> cacheUtil.genResDirCacheAndStartServer(null));
                    }
                } else {
                    GLOBAL_THREAD_POOL.execute(() -> cacheUtil.genResDirCacheAndStartServer(null));
                }
            } else {
                try {
                    server.stop();
                    isStarted.set(false);
                    startOrStop.setText("重载配置并启动服务器");
                } catch (Exception ex) {
                    logger.error("无法正常关闭服务器", ex);
                }
            }
        });
        southControlPanel.add(startOrStop);

        //上传列表
        JScrollPane requestListScrollPane = new JScrollPane(
                requestListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        requestListScrollPane.getVerticalScrollBar().setUnitIncrement(50);
        requestListScrollPane.setBorder(new TitledBorder("上传列表"));
        requestListScrollPane.setPreferredSize(new Dimension((int) (frame.getWidth() * 0.19), frame.getHeight()));

        //组装控制面板
        controlPanel.add(configPanel);
        controlPanel.add(southControlPanel, BorderLayout.SOUTH);
        //服务器窗口组装
        littleServerPanel.add(logPanel, BorderLayout.CENTER);
        //将控制面板和上传列表以水平方向组装
        Box controlPanelBox = Box.createHorizontalBox();
        controlPanelBox.add(controlPanel);
        controlPanelBox.add(requestListScrollPane);

        //组装控制面板和上传列表
        littleServerPanel.add(controlPanelBox, BorderLayout.EAST);

        logger.debug(String.format("载入服务器耗时 %sms.", System.currentTimeMillis() - start));
    }
    private static class AddUpdateRule implements ActionListener {
        JList<String> modeList;
        List<String> rules;

        public AddUpdateRule(JList<String> modeList, List<String> rules) {
            this.modeList = modeList;
            this.rules = rules;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String newRule = JOptionPane.showInputDialog(frame,
                    "请输入更新规则：", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            if (newRule != null && !newRule.isEmpty()) {
                //防止插入相同内容
                if (rules.contains(newRule)) {
                    JOptionPane.showMessageDialog(frame,
                            "重复的更新规则","错误",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    rules.add(newRule);
                    modeList.setListData(rules.toArray(new String[0]));
                }
            }
        }
    }
    private static class DeleteUpdateRule implements ActionListener {
        JList<String> modeList;
        List<String> rules;
        public DeleteUpdateRule(JList<String> modeList, List<String> rules) {
            this.modeList = modeList;
            this.rules = rules;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            List<String> selected = modeList.getSelectedValuesList();

            if (!selected.isEmpty()) {
                rules.removeAll(selected);
                modeList.setListData(rules.toArray(new String[0]));
            } else {
                JOptionPane.showMessageDialog(frame,
                        "请选择一个规则后再删除.", "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //更新规则编辑器类
    private class RuleEditorActionListener implements ActionListener {
        JList<String> ruleList;
        List<String> rules;
        public RuleEditorActionListener(JList<String> ruleList, List<String> rules) {
            this.ruleList = ruleList;
            this.rules = rules;
        }
        private void showRuleEditorDialog(JSONArray jsonArray) {
            //锁定窗口，防止用户误操作
            frame.setEnabled(false);
            RuleEditor editorDialog = new RuleEditor(jsonArray, config.getMainDirPath().replace("/",""));
            editorDialog.setModal(true);

            frame.setEnabled(true);
            editorDialog.setVisible(true);

            if (!editorDialog.getResult().isEmpty()) {
                rules.addAll(editorDialog.getResult());
                ruleList.setListData(rules.toArray(new String[0]));
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isGenerating.get()) {
                JOptionPane.showMessageDialog(frame,
                        "当前正在生成资源缓存，请稍后再试。","注意",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (new File("./res-cache.json").exists()) {
                int selection = JOptionPane.showConfirmDialog(frame,
                        "检测到本地 JSON 缓存，是否以 JSON 缓存启动规则编辑器？",
                        "已检测到本地 JSON 缓存", JOptionPane.YES_NO_OPTION);
                if (selection == JOptionPane.YES_OPTION) {
                    try {
                        String json = FileUtil.readStringFromFile(new File("./res-cache.json"));
                        showRuleEditorDialog(JSONArray.parseArray(json));

                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame,
                                "无法读取本地 JSON 缓存" + ex,"错误",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                int selection = JOptionPane.showConfirmDialog(frame,
                        "未检测到 JSON 缓存，是否立即生成 JSON 缓存并启动规则编辑器？",
                        "未检测到 JSON 缓存", JOptionPane.YES_NO_OPTION);
                if (selection == JOptionPane.YES_OPTION) {
                    GLOBAL_THREAD_POOL.execute(() -> {
                        new CacheUtils(serverInterface,httpServerInterface,startOrStop).updateDirCache(null);
                        try {
                            String json = FileUtil.readStringFromFile(new File("./res-cache.json"));
                            showRuleEditorDialog(JSONArray.parseArray(json));

                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "无法读取本地 JSON 缓存" + ex,"错误",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }
        }
    }

    private void loadHttpServer() {
        serverInterface = new LittleServerInterface() {
            @Override
            public GUILogger getLogger() {
                return logger;
            }

            @Override
            public LittleServerConfig getConfig() {
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
            public void regenCache() {
                regenResCache();
            }
        };
        server = new HttpServer(serverInterface);
        httpServerInterface = () -> server.start();
    }

    /**
     * 重新生成缓存
     */
    private void regenResCache() {
        File jsonCache = new File("./res-cache.json");
        CacheUtils cacheUtil = new CacheUtils(serverInterface,httpServerInterface,startOrStop);
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
    }

    /**
     * 从文件加载配置文件
     * @param IPTextField IP 输入框
     * @param portSpinner 端口输入框
     * @param JksSslTextField JKS 证书
     * @param JksSslPassField JKS 证书密码
     * @param mainDirTextField 资源文件夹路径
     * @param commonMode 普通模式
     * @param onceMode 补全模式
     * @param fileChangeListener 实时文件监听
     */
    private void loadConfigurationFromFile(
            String configFilePath,
            JTextField IPTextField,
            JSpinner portSpinner,
            JTextField JksSslTextField,
            JPasswordField JksSslPassField,
            JTextField mainDirTextField,
            JList<String> commonMode,
            JList<String> onceMode,
            JCheckBox fileChangeListener) {
        if (new File(configFilePath).exists()) {
            try {
                config = ConfigurationManager.loadLittleServerConfigFromFile(configFilePath);
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
            } catch (Exception e) {
                logger.error("加载配置文件的时候出现了问题...", e);
                config = new LittleServerConfig();
                logger.info("目前正在使用程序默认配置.");
            }
        } else {
            try {
                logger.warn("未找到配置文件，正在尝试在程序当前目录生成配置文件...");
                ConfigurationManager.saveConfigurationToFile(new LittleServerConfig(), "./", "littleserver");
                logger.info("配置生成成功.");
                logger.info("目前正在使用程序默认配置.");
            } catch (Exception e) {
                logger.error("生成配置文件的时候出现了问题...", e);
                config = new LittleServerConfig();
                logger.info("目前正在使用程序默认配置.");
            }
        }
    }

    /**
     * 以面板当前配置重载配置
     * @param IPTextField IP 输入框
     * @param portSpinner 端口输入框
     * @param JksSslPassField Jks 证书密码
     * @param mainDirTextField 资源文件夹
     * @param fileChangeListener 实时文件监听器
     * @param commonModeList 普通模式
     * @param onceModeList 补全模式
     * @param config 配置文件
     * @param logger Logger
     */
    private static void reloadConfigurationFromGUI(
            JTextField IPTextField,
            JSpinner portSpinner,
            JPasswordField JksSslPassField,
            JTextField mainDirTextField,
            JCheckBox fileChangeListener,
            List<String> commonModeList,
            List<String> onceModeList,
            LittleServerConfig config,
            GUILogger logger) {
        //设置端口
        config.setPort((Integer) portSpinner.getValue());
        //设置 IP
        String IP = IPTextField.getText();
        String IPType = IPAddressUtil.checkAddress(IP);
        if (IPType != null) {
            if (IP.contains("0.0.0.0")) {
                config.setIp("0.0.0.0");
            } else if ("v4".equals(IPType) || "v6".equals(IPType)) {
                config.setIp(IP);
            }
        } else {
            config.setIp("0.0.0.0");
            logger.warn("配置中的 IP 格式错误，使用默认 IP 地址 0.0.0.0");
        }
        //设置资源目录
        config.setMainDirPath(mainDirTextField.getText());
        //设置实时文件监听器
        config.setFileChangeListener(fileChangeListener.isSelected());
        //设置 Jks 证书密码
        config.setJksSslPassword(String.valueOf(JksSslPassField.getPassword()));
        //设置普通模式
        config.setCommonMode(commonModeList.toArray(new String[0]));
        //设置补全模式
        config.setOnceMode(onceModeList.toArray(new String[0]));
        logger.info("已加载配置.");
    }
}
