package github.kasuminova.balloonserver.Servers;

import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.ConfigurationManager;
import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.GUI.VFlowLayout;
import github.kasuminova.balloonserver.HTTPServer.HttpServer;
import github.kasuminova.balloonserver.Utils.*;
import github.kasuminova.balloonserver.Utils.FileObject.AbstractSimpleFileObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
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
import java.util.stream.Collectors;

import static github.kasuminova.balloonserver.BalloonServer.*;

/**
 * LittleServer 面板实例，支持多实例化
 * @author Kasumi_Nova
 */
public class LittleServer {
    private LittleServerConfig config;
    private List<String> common_ModeList = new ArrayList<>();
    private List<String> once_ModeList = new ArrayList<>();
    private HttpServer server;
    //线程池
    private final ExecutorService GLOBAL_THREAD_POOL = Executors.newCachedThreadPool();
    //服务器启动状态
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    //服务端是否在生成缓存，防止同一时间多个线程生成缓存导致程序混乱
    private final AtomicBoolean isGenerating = new AtomicBoolean(false);
    private final GUILogger logger;
    private final JButton startOrStop = new JButton("重载配置并启动服务器");
    private final JButton regenDirectoryStructureCache = new JButton("重新生成资源文件夹缓存");
    private final JPanel requestListPanel = new JPanel(new VFlowLayout());
    private final cacheUtils cacheUtil = new cacheUtils();
    private final JPanel littleServerPanel = new JPanel(new BorderLayout());

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
        JButton selectJksSslFile = new JButton("选择");

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
        JList<String> common_Mode = new JList<>();
        common_Mode.setVisibleRowCount(4);
        JScrollPane common_ModeScrollPane = new JScrollPane(
                common_Mode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        common_ModePanel.add(common_ModeScrollPane);
        configPanel.add(common_ModePanel);

        //菜单
        JPopupMenu common_ModeMenu = new JPopupMenu();
        JMenuItem addNewCommonRule = new JMenuItem("添加更新规则");
        addNewCommonRule.addActionListener(e -> {
            String newRule = JOptionPane.showInputDialog(frame,
                    "请输入更新规则：", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            if (newRule != null && !newRule.isEmpty()) {
                common_ModeList.add(newRule);
                //去重
                common_ModeList = common_ModeList.stream().distinct().collect(Collectors.toList());
                //设置
                common_Mode.setListData(common_ModeList.toArray(new String[0]));
            }
        });
        common_ModeMenu.add(addNewCommonRule);
        JMenuItem deleteCommonRule = new JMenuItem("删除选定的规则");
        //删除指定规则
        deleteCommonRule.addActionListener(e -> {
            if (common_Mode.getSelectedIndex() != -1) {
                ListModel<String> common_ModeListModel = common_Mode.getModel();
                List<String> listTmp1 = new ArrayList<>();
                for (int i = 0; i < common_ModeListModel.getSize(); i++) {
                    listTmp1.add(common_ModeListModel.getElementAt(i));
                }
                listTmp1.remove(common_Mode.getSelectedIndex());
                common_ModeList = listTmp1;
                common_Mode.setListData(listTmp1.toArray(new String[0]));
            } else {
                JOptionPane.showMessageDialog(frame,
                        "请选择一个规则后再删除.", "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        common_ModeMenu.add(deleteCommonRule);
        //鼠标监听
        common_Mode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()){
                    common_ModeMenu.show(common_Mode,e.getX(),e.getY());
                }
            }
        });

        //补全更新模式
        JPanel once_ModePanel = new JPanel(new VFlowLayout());
        once_ModePanel.setBorder(BorderFactory.createTitledBorder("补全更新模式"));
        JList<String> once_Mode = new JList<>();
        once_Mode.setVisibleRowCount(4);
        JScrollPane once_ModeScrollPane = new JScrollPane(
                once_Mode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        once_ModePanel.add(once_ModeScrollPane);

        //菜单
        JPopupMenu once_ModeMenu = new JPopupMenu();
        JMenuItem addNewOnceRule = new JMenuItem("添加更新规则");
        addNewOnceRule.addActionListener(e -> {
            String newRule = JOptionPane.showInputDialog(frame,
                    "请输入更新规则：", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            if (newRule != null && !newRule.isEmpty()) {
                once_ModeList.add(newRule);
                //去重
                once_ModeList = once_ModeList.stream().distinct().collect(Collectors.toList());
                //设置
                once_Mode.setListData(once_ModeList.toArray(new String[0]));
            }
        });
        once_ModeMenu.add(addNewOnceRule);
        JMenuItem deleteOnceRule = new JMenuItem("删除选定的规则");
        //删除指定规则
        deleteOnceRule.addActionListener(e -> {
            if (once_Mode.getSelectedIndex() != -1) {
                ListModel<String> once_ModeListModel = once_Mode.getModel();
                List<String> listTmp1 = new ArrayList<>();
                for (int i = 0; i < once_ModeListModel.getSize(); i++) {
                    listTmp1.add(once_ModeListModel.getElementAt(i));
                }
                listTmp1.remove(once_Mode.getSelectedIndex());
                once_ModeList = listTmp1;
                once_Mode.setListData(listTmp1.toArray(new String[0]));
            } else {
                JOptionPane.showMessageDialog(frame,
                        "请选择一个规则后再删除.", "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        //鼠标监听
        once_Mode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()){
                    once_ModeMenu.show(once_Mode,e.getX(),e.getY());
                }
            }
        });
        once_ModeMenu.add(deleteOnceRule);
        configPanel.add(once_ModePanel);

        //载入配置文件并初始化 HTTP 服务端
        loadConfigurationFromFile(
                configFilePath,
                IPTextField,
                portSpinner,
                JksSslTextField,
                JksSslPassField,
                mainDirTextField,
                common_Mode,
                once_Mode,
                fileChangeListener);
        loadHttpServer();

        JPanel southControlPanel = new JPanel(new VFlowLayout());
        southControlPanel.add(new JLabel("上方配置修改后，请点击保存配置按钮来载入配置."));

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
                    fileChangeListener);
            try {
                ConfigurationManager.saveConfigurationToFile(config,"./","littleserver");
                logger.info("已保存配置至磁盘.");
            } catch (IOException ex) {
                logger.error("保存配置文件的时候出现了问题...", ex);
            }
        });

        //重新生成资源文件夹缓存
        southControlPanel.add(regenDirectoryStructureCache);
        regenDirectoryStructureCache.addActionListener(e -> {
            File jsonCache = new File("./res-cache.json");
            if (jsonCache.exists()) {
                try {
                    String jsonString = FileUtil.readStringFromFile(jsonCache);
                    JSONArray jsonArray = JSONArray.parseArray(jsonString);
                    new cacheUtils().updateDirCache(jsonArray);
                } catch (Exception ex) {
                    logger.error("读取缓存文件的时候出现了一些问题...", ex);
                    logger.warn("缓存文件读取失败, 重新生成缓存...");
                    cacheUtil.updateDirCache(null);
                }
            } else {
                cacheUtil.updateDirCache(null);
            }
        });

        //启动服务器
        startOrStop.addActionListener(e -> {
            //如果启动则关闭服务器
            if (!isStarted.get()) {
                reloadConfigurationFromGUI(
                        IPTextField,
                        portSpinner,
                        JksSslPassField,
                        mainDirTextField,
                        fileChangeListener);
                File jsonCache = new File("./res-cache.json");
                if (jsonCache.exists()) {
                    try {
                        String jsonString = FileUtil.readStringFromFile(jsonCache);
                        JSONArray jsonArray = JSONArray.parseArray(jsonString);
                        cacheUtil.genResDirCacheAndStartServer(jsonArray);
                    } catch (Exception ex) {
                        logger.error("读取缓存文件的时候出现了一些问题...", ex);
                        logger.warn("缓存文件读取失败, 重新生成缓存...");
                        cacheUtil.genResDirCacheAndStartServer(null);
                    }
                } else {
                    cacheUtil.genResDirCacheAndStartServer(null);
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

        logger.debug(String.format("载入服务器耗时 %sms", System.currentTimeMillis() - start));
    }
    private void loadHttpServer() {
        server = new HttpServer(new LittleServerInterface() {
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
            public void regenCache() {
                regenResCache();
            }
        });
    }

    /**
     * 重新生成缓存
     */
    private void regenResCache() {
        File jsonCache = new File("./res-cache.json");
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
     * 一个内部类, 用于 生成/更新/检查 资源文件夹的缓存
     * <p>
     * 本端含金量最高的类（雾）
     * </p>
     */
    private class cacheUtils {
        //公用定时器
        Timer timer;
        //fileObjList，用于序列化 JSON
        ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();

        //jsonArray 转化为资源文件夹缓存必要的变量
        JSONArray jsonArray;
        NextFileListUtils fileListUtils;
        Thread counterThread;
        /**
         * 更新资源缓存结构
         * 传入的参数如果为 null，则完整生成一次缓存
         */
        private void updateDirCache(JSONArray jsonCache) {
            if (jsonCache != null) {
                if (!genDirCache(jsonCache)) {
                    regenDirectoryStructureCache.setEnabled(true);
                    startOrStop.setEnabled(true);
                    return;
                }
                GLOBAL_THREAD_POOL.execute(new Thread(() -> {
                    try {
                        //等待线程结束
                        counterThread.join();
                        System.gc();
                        logger.info("内存已完成回收.");

                        timer.stop();
                        logger.info("资源变化计算完毕, 正在向磁盘生成 JSON 缓存.");

                        //输出并向服务器设置 JSON
                        generateJsonToDiskAndSetServerJson(jsonArray);

                        //隐藏状态栏进度条
                        statusProgressBar.setVisible(false);
                        regenDirectoryStructureCache.setEnabled(true);
                        startOrStop.setEnabled(true);
                        //重置变量
                        jsonArray = null;
                        isGenerating.set(false);
                    } catch (InterruptedException e) {
                        logger.error("计算资源缓存的时候出现了问题...", e);
                    }
                }));
            } else if (genDirCache(null)) {
                GLOBAL_THREAD_POOL.execute(new Thread(() -> {
                    try {
                        //等待线程结束
                        counterThread.join();
                        System.gc();
                        logger.info("内存已完成回收.");

                        timer.stop();
                        logger.info("资源目录缓存生成完毕, 正在向磁盘生成 JSON 缓存.");

                        //输出并向服务器设置 JSON
                        jsonArray = new JSONArray();
                        jsonArray.addAll(fileObjList);
                        generateJsonToDiskAndSetServerJson(jsonArray);

                        //隐藏状态栏进度条
                        statusProgressBar.setVisible(false);
                        regenDirectoryStructureCache.setEnabled(true);
                        startOrStop.setEnabled(true);
                        //重置变量
                        isGenerating.set(false);
                        jsonArray = null;
                    } catch (InterruptedException e) {
                        logger.error("计算资源缓存的时候出现了问题...", e);
                    }
                }));
            } else {
                regenDirectoryStructureCache.setEnabled(true);
                startOrStop.setEnabled(true);
            }
        }

        /**
         * 更新资源缓存结构并启动服务器
         * 传入的参数如果为 null，则完整生成一次缓存
         */
        private void genResDirCacheAndStartServer(JSONArray jsonCache) {
            if (jsonCache != null) {
                if (!genDirCache(jsonCache)) {
                    regenDirectoryStructureCache.setEnabled(true);
                    startOrStop.setEnabled(true);
                    return;
                }
                GLOBAL_THREAD_POOL.execute(new Thread(() -> {
                    try {
                        //等待线程结束
                        counterThread.join();
                        System.gc();
                        logger.info("内存已完成回收.");

                        timer.stop();
                        logger.info("资源变化计算完毕, 正在向磁盘生成 JSON 缓存.");
                        //输出并向服务器设置 JSON
                        generateJsonToDiskAndSetServerJson(jsonArray);

                        //启动服务器
                        if (server.start()) {
                            isStarted.set(true);
                            startOrStop.setText("关闭服务器");
                        }

                        //隐藏状态栏进度条
                        statusProgressBar.setVisible(false);
                        regenDirectoryStructureCache.setEnabled(true);
                        startOrStop.setEnabled(true);

                        //重置变量
                        jsonArray = null;
                        isGenerating.set(false);
                    } catch (InterruptedException e) {
                        logger.error("计算资源缓存的时候出现了问题...", e);
                    }
                }));
            } else if (!genDirCache(null)) {
                GLOBAL_THREAD_POOL.execute(new Thread(() -> {
                    try {
                        //等待线程结束
                        counterThread.join();
                        System.gc();
                        logger.info("内存已完成回收.");

                        timer.stop();
                        logger.info("资源目录缓存生成完毕, 正在向磁盘生成 JSON 缓存.");
                        //输出并向服务器设置 JSON
                        jsonArray = new JSONArray();
                        jsonArray.addAll(fileObjList);
                        generateJsonToDiskAndSetServerJson(jsonArray);

                        //启动服务器
                        if (server.start()) {
                            isStarted.set(true);
                            startOrStop.setText("关闭服务器");
                        }

                        //隐藏状态栏进度条
                        statusProgressBar.setVisible(false);
                        regenDirectoryStructureCache.setEnabled(true);
                        startOrStop.setEnabled(true);

                        //重置变量
                        isGenerating.set(false);
                        jsonArray = null;
                    } catch (InterruptedException e) {
                        logger.error("计算资源缓存的时候出现了问题...", e);
                    }
                }));
            } else {
                regenDirectoryStructureCache.setEnabled(true);
                startOrStop.setEnabled(true);
                //启动服务器
                if (server.start()) {
                    isStarted.set(true);
                    startOrStop.setText("关闭服务器");
                }
            }
        }

        /**
         * 向磁盘生成 JSON 缓存，并设置服务端 JSON
         * @param jsonArray JSON 缓存
         */
        private void generateJsonToDiskAndSetServerJson(JSONArray jsonArray) {
            String resJSONStr = jsonArray.toJSONString();
            server.setResJson(resJSONStr);
            GLOBAL_THREAD_POOL.execute(new Thread(() -> {
                try {
                    FileUtil.createJsonFile(resJSONStr, "./", "res-cache");
                    logger.info("JSON 缓存生成完毕.");
                } catch (IOException ex) {
                    logger.error("生成 JSON 缓存的时候出现了问题...", ex);
                }
            }));
        }

        /**
         * 以多线程方式生成资源缓存结构
         * <p>
         * 使用配置文件中的资源文件夹路径
         * </p>
         * WARN: 这段代码稍微有点屎山，慎改
         * @return 如果资源文件夹为空返回 false, 否则返回 true
         */
        private boolean genDirCache(JSONArray jsonCache) {
            isGenerating.set(true);
            regenDirectoryStructureCache.setEnabled(false);
            startOrStop.setEnabled(false);

            //创建新的计算实例
            fileListUtils = new NextFileListUtils();

            File dir = new File("." + config.getMainDirPath());
            if (!dir.exists()) {
                logger.warn("设定中的资源目录：" + dir.getPath() + " 不存在, 使用默认路径.");
                dir = new File("./res");
                if (!dir.exists()) {
                    logger.warn("默认资源目录不存在：" + dir.getPath() + " 正在创建文件夹.");
                    if (dir.mkdir()) {
                        logger.error("默认资源目录创建失败！请检查你的资源目录文件夹是否被占用或非文件夹。");
                    }
                    logger.warn("资源目录为空, 跳过缓存生成.");
                    return false;
                }
            }

            logger.info("正在计算资源目录大小...");
            File[] fileList = dir.listFiles();
            //检查文件夹是否为空
            if (fileList == null) {
                logger.warn("资源目录为空, 跳过缓存生成.");
                return false;
            }

            //计算文件夹内的文件和总大小（文件夹不计入），用于进度条显示
            //TODO: 计划放到新线程中计算
            long[] dirSize = NextFileListUtils.getDirSize(dir);

            String totalSize = FileUtil.formatFileSizeToStr(dirSize[0]);
            if (dirSize[0] != 0) {
                FileCacheCalculator fileCacheCalculator = new FileCacheCalculator(logger);
                logger.info("文件夹大小：" + totalSize + ", 文件数量：" + dirSize[1]);
                if (jsonCache != null) {
                    logger.info("检测到已缓存的 JSON, 正在检查变化...");

                    //创建新线程实例并执行
                    File finalDir = dir;
                    counterThread = new Thread(() -> jsonArray = fileCacheCalculator.scanDir(jsonCache, finalDir));
                    counterThread.start();

                    statusProgressBar = addNewStatusProgressBar();
                    statusProgressBar.setString("检查变化中：" + 0 + " 文件 / " + dirSize[1] + " 文件");
                    timer = new Timer(25, e -> {
                        int progress = fileCacheCalculator.progress.get();
                        statusProgressBar.setValue((int) ((double) progress * 1000 / dirSize[1]));
                        statusProgressBar.setString("检查变化中：" + progress + " 文件 / " + dirSize[1] + " 文件");
                    });
                } else {
                    logger.info("正在生成资源目录缓存...");
                    File finalDir = dir;

                    //创建新线程实例并执行
                    counterThread = new Thread(() -> fileObjList = fileListUtils.scanDir(finalDir, logger));
                    counterThread.start();

                    statusProgressBar = addNewStatusProgressBar();
                    //轮询线程, 读取进度
                    timer = new Timer(25, e -> {
                        long completedBytes = fileListUtils.completedBytes.get();
                        long completedFiles = fileListUtils.completedFiles.get();
                        String completedSize = FileUtil.formatFileSizeToStr(completedBytes);
                        statusProgressBar.setValue((int) ((double) completedBytes * 1000 / dirSize[0]));
                        statusProgressBar.setString("生成缓存中：" +
                                completedSize + " / " + totalSize +
                                " - " +
                                completedFiles + " 文件 / " + dirSize[1] +
                                " 文件");
                    });
                }
                //启动轮询
                timer.start();
                statusProgressBar.setVisible(true);
            }
            return true;
        }
    }

    /**
     * 从文件加载配置文件
     * @param IPTextField IP 输入框
     * @param portSpinner 端口输入框
     * @param JksSslTextField JKS 证书
     * @param JksSslPassField JKS 证书密码
     * @param mainDirTextField 资源文件夹路径
     * @param common_Mode 普通模式
     * @param once_Mode 补全模式
     * @param fileChangeListener 实时文件监听
     */
    private void loadConfigurationFromFile(
            String configFilePath,
            JTextField IPTextField,
            JSpinner portSpinner,
            JTextField JksSslTextField,
            JPasswordField JksSslPassField,
            JTextField mainDirTextField,
            JList<String> common_Mode,
            JList<String> once_Mode,
            JCheckBox fileChangeListener) {
        if (new File(configFilePath).exists()) {
            try {
                config = ConfigurationManager.loadLittleServerConfigFromFile("./littleserver.json");
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
                common_ModeList = new ArrayList<>(Arrays.asList(config.getCommonMode()));
                common_Mode.setListData(config.getCommonMode());
                //补全模式
                once_ModeList = new ArrayList<>(Arrays.asList(config.getOnceMode()));
                once_Mode.setListData(config.getOnceMode());

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
            } catch (Exception e) {
                logger.error("生成配置文件的时候出现了问题...", e);
            }
            config = new LittleServerConfig();
            logger.info("目前正在使用程序默认配置.");
        }
    }

    /**
     * 以 GUI 当前的配置重载配置文件
     * @param IPTextField IP 输入框
     * @param portSpinner 端口输入框
     * @param JksSslPassField JKS 证书密码
     * @param mainDirTextField 资源文件夹路径
     * @param fileChangeListener 实时文件监听
     */
    private void reloadConfigurationFromGUI(
            JTextField IPTextField,
            JSpinner portSpinner,
            JPasswordField JksSslPassField,
            JTextField mainDirTextField,
            JCheckBox fileChangeListener) {
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
        config.setCommonMode(common_ModeList.toArray(new String[0]));
        //设置补全模式
        config.setOnceMode(once_ModeList.toArray(new String[0]));
        logger.info("已加载配置.");
    }
}
