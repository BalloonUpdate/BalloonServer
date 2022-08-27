package github.kasuminova.balloonserver.Servers;

import com.alibaba.fastjson2.JSONArray;
import github.balloonupdate.littleserver.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.ConfigurationManager;
import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.GUI.VFlowLayout;
import github.kasuminova.balloonserver.HTTPServer.HttpServer;
import github.kasuminova.balloonserver.Utils.FileListUtils;
import github.kasuminova.balloonserver.Utils.FileUtil;
import github.kasuminova.balloonserver.Utils.IPUtil.IPAddressUtil;
import github.kasuminova.balloonserver.Utils.LogManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static github.kasuminova.balloonserver.BalloonServer.*;

public class LittleServer {
    //服务器启动状态
    public static AtomicBoolean isStarted = new AtomicBoolean(false);
    public static HttpServer server = new HttpServer();
    public static LogManager logger;
    public static LittleServerConfig config;
    public static JButton startOrStop = new JButton("重载配置并启动服务器");
    public static JButton regenDirectoryStructureCache = new JButton("重新生成资源文件夹缓存");
    public static JPanel requestListPanel = new JPanel(new VFlowLayout());
    static List<String> common_ModeList = new ArrayList<>();
    static List<String> once_ModeList = new ArrayList<>();
    public static JPanel createPanel() {
        //主面板
        JPanel littleServerPanel = new JPanel(new BorderLayout());

        //设置 Logger，主体为 logPanel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("程序日志"));
        JTextPane logPane = new JTextPane();
        logPane.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logPane);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        logger = new LogManager("", logPane);
        server.setLogger(logger);

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

        //高性能模式
        Box mainDirBox = Box.createHorizontalBox();
        JLabel mainDirLabel = new JLabel("资源文件夹：");
        JTextField mainDirTextField = new JTextField("/res");
        mainDirTextField.setToolTipText(
                "仅支持程序当前目录下的文件夹或子文件夹，请勿输入其他文件夹。\n" +
                "默认为 /res , 也可输入其他文件夹, 如 /resource、/content/res 等.");
        mainDirBox.add(mainDirLabel);
        mainDirBox.add(mainDirTextField);

        JCheckBox highPerformanceMode = new JCheckBox("高性能模式");
        highPerformanceMode.setToolTipText(
                "高性能模式，平时在线人数超过 50 建议开启，否则建议关闭\n" +
                "开启后，修改更新规则和变动 res 目录下的文件都需要在关闭单文件服务端的情况下进行，否则不会生效\n" +
                "关闭时，仅修改更新规则需要重启，修改 res 目录下的文件则不需要");
        mainDirBox.add(highPerformanceMode);
        configPanel.add(mainDirBox);

        //SSL 证书
        Box JksSslBox = Box.createHorizontalBox();
        JksSslBox.add(new JLabel("JKS 证书文件："));
        JTextField JksSslTextField = new JTextField("请选择证书文件");
        JksSslTextField.setEditable(false);
        JButton selectJksSslFile = new JButton("选择");

        selectJksSslFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setFileFilter(new FileUtil.SimpleFileFilter(new String[]{"jks"}, "JKS 证书 (*.jks)"));

            if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(littleServerPanel, "载入证书")) {
                File JKS = fileChooser.getSelectedFile();
                if (JKS != null && JKS.exists()) {
                    config.setJKSFilePath(JKS.getPath());
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

        //普通更新模式
        JPanel common_ModePanel = new JPanel(new VFlowLayout());
        common_ModePanel.setBorder(BorderFactory.createTitledBorder("普通更新模式"));
        JList<String> common_Mode = new JList<>();
        common_Mode.setVisibleRowCount(4);
        JScrollPane common_ModeScrollPane = new JScrollPane(common_Mode,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        common_ModePanel.add(common_ModeScrollPane);
        configPanel.add(common_ModePanel);

        //菜单
        JPopupMenu common_ModeMenu = new JPopupMenu();
        JMenuItem addNewCommonRule = new JMenuItem("添加更新规则");
        addNewCommonRule.addActionListener(e -> {
            String newRule = JOptionPane.showInputDialog(frame, "请输入更新规则：", "提示", JOptionPane.INFORMATION_MESSAGE);
            if (!newRule.isEmpty()) {
                common_ModeList.add(newRule);
                //去重
                common_ModeList = common_ModeList.stream().distinct().collect(Collectors.toList());
                //设置
                common_Mode.setListData(common_ModeList.toArray(new String[common_ModeList.size()]));
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
                common_Mode.setListData(listTmp1.toArray(new String[listTmp1.size()]));
            } else {
                JOptionPane.showMessageDialog(frame,"请选择一个规则后再删除.", "错误", JOptionPane.ERROR_MESSAGE);
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
        JScrollPane once_ModeScrollPane = new JScrollPane(once_Mode,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        once_ModePanel.add(once_ModeScrollPane);

        //菜单
        JPopupMenu once_ModeMenu = new JPopupMenu();
        JMenuItem addNewOnceRule = new JMenuItem("添加更新规则");
        addNewOnceRule.addActionListener(e -> {
            String newRule = JOptionPane.showInputDialog(frame, "请输入更新规则：", "提示", JOptionPane.INFORMATION_MESSAGE);
            if (!newRule.isEmpty()) {
                once_ModeList.add(newRule);
                //去重
                once_ModeList = once_ModeList.stream().distinct().collect(Collectors.toList());
                //设置
                once_Mode.setListData(once_ModeList.toArray(new String[once_ModeList.size()]));
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
                once_Mode.setListData(listTmp1.toArray(new String[listTmp1.size()]));
            } else {
                JOptionPane.showMessageDialog(frame,"请选择一个规则后再删除.", "错误", JOptionPane.ERROR_MESSAGE);
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

        //载入配置文件
        loadConfigurationFromFile(IPTextField,portSpinner,highPerformanceMode,JksSslTextField,JksSslPassField,mainDirTextField,common_Mode,once_Mode);

        JPanel southControlPanel = new JPanel(new VFlowLayout());
        southControlPanel.add(new JLabel("上方配置修改后，请点击重载配置按钮来载入配置."));

        //重载配置
        JButton reloadButton = new JButton("重载配置");
        reloadButton.setToolTipText("以控制面板当前的配置应用到程序配置，但是不保存配置到磁盘.");
        reloadButton.addActionListener(e -> reloadConfigurationFromGUI(IPTextField,portSpinner,highPerformanceMode,JksSslPassField,mainDirTextField));
        southControlPanel.add(reloadButton);

        //存储当前配置
        JButton saveConfigButton = new JButton("保存当前配置并重载");
        saveConfigButton.setToolTipText("以控制面板当前的配置应用到程序配置，并保存配置到磁盘.");
        southControlPanel.add(saveConfigButton);
        saveConfigButton.addActionListener(e -> {
            reloadConfigurationFromGUI(IPTextField,portSpinner,highPerformanceMode,JksSslPassField,mainDirTextField);
            try {
                ConfigurationManager.saveConfigurationToFile(config,"./","littleserver");
                logger.info("已保存配置至磁盘.");
            } catch (IOException ex) {
                logger.error("保存配置文件的时候出现了问题...", ex);
            }
        });

        //重新生成资源文件夹缓存
        southControlPanel.add(regenDirectoryStructureCache);
        regenDirectoryStructureCache.addActionListener(e -> genDirCache());

        //启动服务器
        startOrStop.addActionListener(e -> {
            //如果启动则关闭服务器
            if (!isStarted.get()) {
                reloadConfigurationFromGUI(IPTextField,portSpinner,highPerformanceMode,JksSslPassField,mainDirTextField);
                genDirCacheAndStartServer();
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
        JScrollPane requestListScrollPane = new JScrollPane(requestListPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        return littleServerPanel;
    }

    /**
     * 以多线程方式生成资源缓存结构
     * 使用配置文件中的资源文件夹路径
     * WARN: 这段代码稍微有点屎山，慎改
     */
    private static void genDirCache() {
        ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();

        File dir = new File("." + config.getMainDirPath());
        if (!dir.exists()) {
            logger.warn("设定中的资源目录：" + dir.getPath() + " 不存在, 使用默认路径.");
            dir = new File("./res");
            if (!dir.exists()) {
                logger.warn("默认资源目录不存在：" + dir.getPath() + " 正在创建文件夹.");
                dir.mkdir();
            }
        } else if (dir.listFiles().length == 1) {
            dir = dir.listFiles()[0];
        }

        //计算资源目录大小, 用于进度条计算
        logger.info("正在计算资源目录大小...");
        long[] dirSize = FileListUtils.getDirSize(dir);
        String totalSize = FileUtil.formatFileSizeToStr(dirSize[0]);
        if (dirSize[0] != 0) {
            regenDirectoryStructureCache.setEnabled(false);
            startOrStop.setEnabled(false);
            logger.info("文件夹大小：" + totalSize + ", 文件数量：" + dirSize[1]);

            //新线程, 计算资源目录缓存
            logger.info("正在生成资源目录缓存...");
            statusProgressBar.setMaximum((int) dirSize[1]);
            Thread thread = new Thread(new FileListUtils.DirCounterThread(fileObjList, dir));
            thread.start();
            //轮询线程, 读取进度
            Timer timer = new Timer(100, e -> {
                long completedBytes = FileListUtils.completedBytes.get();
                int completedFiles = FileListUtils.completedFiles.get();
                String completedSize = FileUtil.formatFileSizeToStr(completedBytes);
                statusProgressBar.setValue(completedFiles);
                statusProgressBar.setString("生成缓存中：" +
                        completedSize + " / " + totalSize +
                        " - " +
                        completedFiles + " 文件 / " + dirSize[1] +
                        " 文件");
            });
            //启动轮询
            timer.start();
            statusProgressBar.setVisible(true);
            //隐藏状态栏进度条
            changeListener = e -> {
                if (FileListUtils.completedFiles.get() >= dirSize[1]) {
                    timer.stop();
                    logger.info("资源目录缓存生成完毕, 正在向磁盘生成 JSON 缓存.");
                    //输出 JSON
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.addAll(fileObjList);
                    String resJSONStr = jsonArray.toJSONString();
                    server.setResJSON(resJSONStr);
                    try {
                        FileUtil.createJsonFile(resJSONStr, "./", "res-cache");
                        logger.info("JSON 缓存生成完毕.");
                    } catch (IOException ex) {
                        logger.error("生成 JSON 缓存的时候出现了问题...", ex);
                    }

                    FileListUtils.completedBytes.set(0);
                    FileListUtils.completedFiles.set(0);
                    statusProgressBar.setVisible(false);
                    regenDirectoryStructureCache.setEnabled(true);
                    startOrStop.setEnabled(true);
                }
            };
            statusProgressBar.addChangeListener(changeListener);
        } else {
            logger.warn("资源目录为空, 跳过缓存生成.");
        }
    }

    /**
     * 以多线程方式生成资源缓存结构并启动服务器
     * 使用配置文件中的资源文件夹路径
     * WARN: 这段代码稍微有点屎山，慎改
     */
    private static void genDirCacheAndStartServer() {
        regenDirectoryStructureCache.setEnabled(false);
        startOrStop.setEnabled(false);
        ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();

        File dir = new File("." + config.getMainDirPath());

        if (!dir.exists()) {
            logger.warn("设定中的资源目录：" + dir.getPath() + " 不存在, 使用默认路径.");
            dir = new File("./res");
            if (!dir.exists()) {
                logger.warn("默认资源目录不存在：" + dir.getPath() + " 正在创建文件夹.");
                dir.mkdir();
            }
        } else {
            if (dir.listFiles().length == 1) {
                dir = dir.listFiles()[0];
            }
        }

        //计算资源目录大小, 用于进度条计算
        logger.info("正在计算资源目录大小...");
        long[] dirSize = FileListUtils.getDirSize(dir);
        String totalSize = FileUtil.formatFileSizeToStr(dirSize[0]);
        if (dirSize[0] != 0) {
            logger.info("文件夹大小：" + totalSize + ", 文件数量：" + dirSize[1]);

            //新线程, 计算资源目录缓存
            logger.info("正在生成资源目录缓存...");
            statusProgressBar.setMaximum((int) dirSize[1]);
            Thread thread = new Thread(new FileListUtils.DirCounterThread(fileObjList, dir));
            thread.start();
            //轮询线程, 读取进度
            Timer timer = new Timer(100, e -> {
                long completedBytes = FileListUtils.completedBytes.get();
                long completedFiles = FileListUtils.completedFiles.get();
                String completedSize = FileUtil.formatFileSizeToStr(completedBytes);
                statusProgressBar.setValue((int) completedFiles);
                statusProgressBar.setString("生成缓存中：" +
                        completedSize + " / " + totalSize +
                        " - " +
                        completedFiles + " 文件 / " + dirSize[1] +
                        " 文件");
            });
            //启动轮询
            timer.start();
            statusProgressBar.setVisible(true);
            //隐藏状态栏进度条
            changeListener = e -> {
                if (FileListUtils.completedFiles.get() >= dirSize[1]) {
                    timer.stop();
                    logger.info("资源目录缓存生成完毕, 正在向磁盘生成 JSON 缓存.");
                    //输出 JSON
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.addAll(fileObjList);
                    String resJSONStr = jsonArray.toJSONString();
                    server.setResJSON(resJSONStr);
                    try {
                        FileUtil.createJsonFile(resJSONStr, "./", "res-cache");
                        logger.info("JSON 缓存生成完毕.");
                    } catch (IOException ex) {
                        logger.error("生成 JSON 缓存的时候出现了问题...", ex);
                    }
                    server.start();

                    FileListUtils.completedBytes.set(0);
                    FileListUtils.completedFiles.set(0);
                }
            };
            statusProgressBar.addChangeListener(changeListener);
        } else {
            logger.warn("资源目录为空, 跳过缓存生成.");
            server.start();

            startOrStop.setEnabled(true);
            startOrStop.setText("关闭服务器");
        }
    }

    private static void loadConfigurationFromFile(
            JTextField IPTextField,
            JSpinner portSpinner,
            JCheckBox highPerformanceMode,
            JTextField JksSslTextField,
            JPasswordField JksSslPassField,
            JTextField mainDirTextField,
            JList<String> common_Mode,
            JList<String> once_Mode) {
        if (new File("./littleserver.json").exists()) {
            try {
                config = ConfigurationManager.loadLittleServerConfigFromFile("./littleserver.json");
                //IP
                IPTextField.setText(config.getIP());
                //端口
                portSpinner.setValue(config.getPort());
                //资源文件夹
                mainDirTextField.setText(config.getMainDirPath());
                //高性能模式
                highPerformanceMode.setSelected(config.isHighPerformanceMode());
                //Jks 证书
                if (!config.getJKSFilePath().isEmpty()) {
                    File JksSsl = new File(config.getJKSFilePath());
                    if (JksSsl.exists()) {
                        JksSslTextField.setText(JksSsl.getName());
                    } else {
                        logger.warn("Jks 证书文件不存在.");
                    }
                }
                //Jks 证书密码
                JksSslPassField.setText(config.getJKSSSLPassword());
                //普通模式
                common_ModeList = new ArrayList<>(Arrays.asList(config.getCommon_mode()));
                common_Mode.setListData(config.getCommon_mode());
                //补全模式
                once_ModeList = new ArrayList<>(Arrays.asList(config.getOnce_mode()));
                once_Mode.setListData(config.getOnce_mode());

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
    private static void reloadConfigurationFromGUI(
            JTextField IPTextField,
            JSpinner portSpinner,
            JCheckBox highPerformanceMode,
            JPasswordField JksSslPassField,
            JTextField mainDirTextField) {
        //设置端口
        config.setPort((Integer) portSpinner.getValue());
        //设置 IP
        String IP = IPTextField.getText();
        if (IP.contains("0.0.0.0")) {
            config.setIP("0.0.0.0");
        } else if (IPAddressUtil.isIPv4LiteralAddress(IP) || IPAddressUtil.isIPv6LiteralAddress(IP)) {
            config.setIP(IP);
        } else {
            config.setIP("0.0.0.0");
            logger.warn("配置中的 IP 格式错误，使用默认 IP 地址 0.0.0.0");
        }
        //设置资源目录
        config.setMainDirPath(mainDirTextField.getText());
        //设置高性能模式
        config.setHighPerformanceMode(highPerformanceMode.isSelected());
        //设置 Jks 证书密码
        config.setJKSSSLPassword(String.valueOf(JksSslPassField.getPassword()));
        //设置普通模式
        config.setCommon_mode(common_ModeList.toArray(new String[common_ModeList.size()]));
        config.setOnce_mode(once_ModeList.toArray(new String[once_ModeList.size()]));
        //设置补全模式
        //设置 HTTP 服务器配置
        server.setConfig(config);
        logger.info("已加载配置.");
    }
}
