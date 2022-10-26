package github.kasuminova.balloonserver.servers.remoteserver;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.configurations.ConfigurationManager;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.remoteclient.RemoteClient;
import github.kasuminova.balloonserver.servers.AbstractServer;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.balloonserver.utils.IPAddressUtil;
import github.kasuminova.balloonserver.utils.ModernColors;
import github.kasuminova.messages.RequestMessage;
import github.kasuminova.messages.StringMessage;
import github.kasuminova.messages.filemessages.FileRequestMsg;
import io.netty.channel.ChannelHandlerContext;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static github.kasuminova.balloonserver.BalloonServer.MAIN_FRAME;

/**
 * RemoteIntegratedClient 远程服务器客户端实例
 */
public class RemoteIntegratedServerClient extends AbstractServer {
    protected final RemoteClientConfig config = new RemoteClientConfig();
    protected final JPanel remoteClientPanel = new JPanel(new BorderLayout());
    protected final JPanel remotePanel = new JPanel(new BorderLayout());
    protected final JPanel connectPanel = new JPanel(new VFlowLayout());
    protected final JTextField remoteIPTextField = new JTextField("0.0.0.0");
    protected final JSpinner remotePortSpinner = new JSpinner();
    protected final JTextField tokenTextField = new JTextField("");
    protected final JPanel statusPanel = new JPanel(new BorderLayout());
    protected final SmoothProgressBar memBar = new SmoothProgressBar(500, 250);
    protected final JLabel runningThreadCount = new JLabel("远程服务器运行的线程数: 未连接至服务器");
    protected final JLabel pingLabel = new JLabel("延迟: 未连接至服务器");
    protected final GUILogger logger;
    protected final String serverName;
    protected String clientIP = null;
    protected boolean isConnected = false;
    protected boolean isConnecting = false;
    protected ChannelHandlerContext remoteChannel;
    protected RemoteClientInterface serverInterface;
    protected RemoteClient client;

    public RemoteIntegratedServerClient(String serverName) {
        this.serverName = serverName;

        //设置 Logger，主体为 logPanel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setMinimumSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.5), 0));

        logPanel.setBorder(new TitledBorder("远程服务器实例日志"));
        JTextPane logPane = new JTextPane();
        logPane.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logPane);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        remoteClientPanel.add(logPanel);

        //初始化 Logger
        logger = new GUILogger(serverName, logPane);

        loadClientInterface();

        controlPanel.setVisible(false);

        remotePanel.add(loadControlPanel(), BorderLayout.EAST);
        remotePanel.add(loadConnectPanel(), BorderLayout.WEST);
        remoteClientPanel.add(remotePanel, BorderLayout.EAST);

        remoteClientPanel.add(loadStatusBar(), BorderLayout.SOUTH);
    }

    protected JPanel loadControlPanel() {
        controlPanel.setPreferredSize(new Dimension(350, 0));
        controlPanel.setBorder(new TitledBorder("远程控制面板"));

        //配置窗口
        JPanel configPanel = new JPanel(new VFlowLayout());

        //IP 端口
        configPanel.add(loadIPPortBox());
        //资源文件夹
        configPanel.add(loadMainDirBox());
        //SSL 证书框，仅用于显示。实际操作为右方按钮。
//        configPanel.add(loadJksSslBox());
        //Jks 证书密码框
        configPanel.add(loadJksSslPassBox());
        //额外功能
        configPanel.add(loadExtraFeaturesPanel());
        //普通模式
        configPanel.add(loadCommonModePanel());
        //补全模式
        configPanel.add(loadOnceModePanel());

        JScrollPane configScroll = new JScrollPane(configPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        configScroll.getVerticalScrollBar().setUnitIncrement(15);
        configScroll.setBorder(new TitledBorder("远程配置"));


        JPanel southControlPanel = new JPanel(new VFlowLayout());

        JPanel msgPanel = new JPanel(new BorderLayout());
        msgPanel.add(new JLabel("输入消息:"), BorderLayout.WEST);
        JTextField textField = new JTextField();

        msgPanel.add(textField, BorderLayout.CENTER);

        JButton sendMessage = new JButton("发送消息");
        sendMessage.addActionListener(e -> {
            ChannelHandlerContext mainChannel = serverInterface.getMainChannel();
            mainChannel.writeAndFlush(new StringMessage(textField.getText()));
            textField.setText("");
        });

        msgPanel.add(sendMessage, BorderLayout.EAST);
        southControlPanel.add(msgPanel);

        JPanel fileRequestPanel = new JPanel(new BorderLayout());
        fileRequestPanel.add(new JLabel("请求文件:"), BorderLayout.WEST);
        JTextField fileRequestTextField = new JTextField();

        fileRequestPanel.add(fileRequestTextField, BorderLayout.CENTER);

        JButton requestFile = new JButton("请求文件");
        requestFile.addActionListener(e -> {
            ChannelHandlerContext mainChannel = serverInterface.getMainChannel();
            mainChannel.writeAndFlush(new FileRequestMsg("./", fileRequestTextField.getText()));
            textField.setText("");
        });

        fileRequestPanel.add(requestFile, BorderLayout.EAST);
        southControlPanel.add(fileRequestPanel);

        JButton disconnect = new JButton("断开连接");
        disconnect.addActionListener(e -> {
            client.disconnect();
            serverInterface.onDisconnected();
        });
        southControlPanel.add(disconnect);

        JButton openFileList = new JButton("打开服务端文件夹列表");
        openFileList.addActionListener(e -> remoteChannel.writeAndFlush(
                new RequestMessage("GetFileList",
                        List.of(new String[]{"./"}))));
        southControlPanel.add(openFileList);

        controlPanel.add(configScroll);
        controlPanel.add(southControlPanel, BorderLayout.SOUTH);

        return controlPanel;
    }

    protected JPanel loadStatusBar() {
        statusPanel.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(5, 4, 5, 4)));

        Box leftBox = Box.createHorizontalBox();
        pingLabel.setBorder(new EmptyBorder(0, 0, 0, 20));
        leftBox.add(pingLabel);
        leftBox.add(runningThreadCount);

        Box memBarBox = Box.createHorizontalBox();
        memBar.setBorder(new EmptyBorder(0, 0, 0, 5));
        memBar.setPreferredSize(new Dimension(250, memBar.getHeight()));
        memBar.setStringPainted(true);
        memBar.setString("未连接至服务器");
        memBarBox.add(new JLabel("远程服务器 JVM 内存使用情况: "));
        memBarBox.add(memBar);
        JButton GC = new JButton("清理");
        GC.setPreferredSize(new Dimension(65, 21));
        GC.addActionListener(e -> {
            if (remoteChannel != null) remoteChannel.writeAndFlush(
                    new RequestMessage("MemoryGC"));
        });
        memBarBox.add(GC);

        statusPanel.add(leftBox, BorderLayout.WEST);
        statusPanel.add(memBarBox, BorderLayout.EAST);

        return statusPanel;
    }

    private JPanel loadConnectPanel() {
        connectPanel.setPreferredSize(new Dimension(350, 0));
        connectPanel.setBorder(new TitledBorder("连接服务器"));

        //IP 配置
        Box IPPortBox = Box.createHorizontalBox();
        IPPortBox.add(new JLabel("远程服务器 IP:"));
        IPPortBox.add(remoteIPTextField);

        //端口配置
        IPPortBox.add(new JLabel(" 远程端口:"), BorderLayout.WEST);
        SpinnerNumberModel portSpinnerModel = new SpinnerNumberModel(10000, 1, 65535, 1);
        remotePortSpinner.setModel(portSpinnerModel);
        JSpinner.NumberEditor portSpinnerEditor = new JSpinner.NumberEditor(remotePortSpinner, "#");
        remotePortSpinner.setEditor(portSpinnerEditor);

        IPPortBox.add(remotePortSpinner);

        //token 配置
        Box tokenBox = Box.createHorizontalBox();
        tokenBox.add(new JLabel("Token:"));
        tokenBox.add(tokenTextField);

        loadRemoteServerConfigFromFile();

        JButton connect = new JButton("连接");
        connect.addActionListener(e -> {
            if (isConnecting) {
                JOptionPane.showMessageDialog(MAIN_FRAME,
                        "当前正在连接服务器, 请稍后再试.",
                        BalloonServer.TITLE, JOptionPane.ERROR_MESSAGE);
                return;
            }
            ThreadUtil.execute(() -> {
                isConnecting = true;
                reloadConfigurationFromGUI();
                connect.setText("连接中...");
                try {
                    client.connect();
                } catch (Exception ex) {
                    logger.error("无法连接至服务器: {}", ex);
                    isConnecting = false;
                }
                connect.setText("连接");
            });
        });

        connectPanel.add(IPPortBox);
        connectPanel.add(tokenBox);
        connectPanel.add(connect);

        return connectPanel;
    }

    private void loadClientInterface() {
        serverInterface = new RemoteClientInterface() {
            long lastStatusUpdated;

            @Override
            public IntegratedServerConfig getIntegratedServerConfig() {
                return null;
            }

            @Override
            public String getServerName() {
                return serverName;
            }

            @Override
            public void regenCache() {

            }

            @Override
            public boolean stopServer() {
                return false;
            }

            @Override
            public void saveConfig() {

            }

            @Override
            public String getIndexJson() {
                return null;
            }

            @Override
            public String getResJsonFileExtensionName() {
                return null;
            }

            @Override
            public String getLegacyResJsonFileExtensionName() {
                return null;
            }

            @Override
            public String getLegacyResJson() {
                return null;
            }

            @Override
            public void setLegacyResJson(String newLegacyResJson) {

            }

            @Override
            public SmoothProgressBar getStatusProgressBar() {
                return null;
            }

            @Override
            public void setStatusLabelText(String text, Color fg) {

            }

            @Override
            public void resetStatusProgressBar() {

            }

            @Override
            public AtomicBoolean isGenerating() {
                return null;
            }

            @Override
            public AtomicBoolean isStarted() {
                return null;
            }

            @Override
            public String getResJson() {
                return null;
            }

            @Override
            public void setResJson(String newResJson) {

            }

            @Override
            public ChannelHandlerContext getMainChannel() {
                return remoteChannel;
            }

            @Override
            public RemoteClientConfig getRemoteClientConfig() {
                return config;
            }

            @Override
            public void updateStatus(String memBarText, int value, int activeThreads, String clientIP) {
                memBar.setString(memBarText);
                memBar.setValue(value);
                runningThreadCount.setText(StrUtil.format("远程服务器运行的线程数: {}", activeThreads));

                int ping = (int) (System.currentTimeMillis() - lastStatusUpdated - 1000);
                pingLabel.setText(StrUtil.format("延迟: {}ms", Math.max(ping, 0)));

                if (ping < 100) {
                    pingLabel.setForeground(ModernColors.GREEN);
                } else if (ping < 200) {
                    pingLabel.setForeground(ModernColors.BLUE);
                } else if (ping < 400) {
                    pingLabel.setForeground(ModernColors.YELLOW);
                } else {
                    pingLabel.setForeground(ModernColors.RED);
                }

                lastStatusUpdated = System.currentTimeMillis();
                RemoteIntegratedServerClient.this.clientIP = clientIP;
            }

            @Override
            public void onDisconnected() {
                remoteChannel = null;
                clientIP = null;

                isConnecting = false;
                isConnected = false;

                pingLabel.setText("延迟: 未连接至服务器");
                runningThreadCount.setText("远程服务器运行的线程数: 未连接至服务器");
                memBar.setValue(0);
                memBar.setString("未连接至服务器");
                controlPanel.setVisible(false);
                connectPanel.setVisible(true);
            }

            @Override
            public void onConnected(ChannelHandlerContext ctx) {
                remoteChannel = ctx;

                isConnecting = false;
                isConnected = true;

                controlPanel.setVisible(true);
                connectPanel.setVisible(false);

                lastStatusUpdated = System.currentTimeMillis();
            }

            @Override
            public void updateConfig(IntegratedServerConfig config) {
                ThreadUtil.execute(() -> updateGUIConfig(config));
            }

            @Override
            public void showCommonRuleEditorDialog(JSONArray jsonArray) {

            }

            @Override
            public void showOnceRuleEditorDialog(JSONArray jsonArray) {

            }
        };
        client = new RemoteClient(logger, serverInterface);
    }

    protected void loadRemoteServerConfigFromFile() {
        String path = StrUtil.format("{}.hscfg", serverName);
        try {
            if (new File(path + ".json").exists()) {
                ConfigurationManager.loadRemoteClientConfigFromFile(path + ".json", config);
                logger.info("成功载入远程服务器客户端配置文件.");
            } else {
                logger.info("远程服务器客户端文件不存在, 正在创建文件...");
                ConfigurationManager.saveConfigurationToFile(config, "./", path);
                logger.info("成功生成远程服务器客户端配置文件.");
            }
        } catch (Exception e) {
            logger.error("远程服务器客户端配置文件加载失败！", e);
        }

        remoteIPTextField.setText(config.getIp());
        remotePortSpinner.setValue(config.getPort());
        tokenTextField.setText(config.getToken());
    }

    protected void reloadConfigurationFromGUI() {
        //IP 检查
        String IP = remoteIPTextField.getText();
        String IPType = IPAddressUtil.checkAddress(IP);
        if (IPType == null) {
            IP = "0.0.0.0";
            config.setIp("0.0.0.0");
            logger.warn("配置中的 IP 格式错误，使用默认 IP 地址 0.0.0.0");
        }

        config.setPort((Integer) remotePortSpinner.getValue())
                .setIp(IP)
                .setToken(tokenTextField.getText());

        saveConfiguration();
        logger.info("已加载配置.");
    }

    private void updateGUIConfig(IntegratedServerConfig config) {
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
        JksSslTextField.setText(config.getJksFilePath());
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

        logger.info("已载入 API 服务器配置文件.");
    }

    protected void saveConfiguration() {
        try {
            ConfigurationManager.saveConfigurationToFile(config, "./", serverName + ".hscfg");
            logger.info("已保存配置文件至磁盘.");
        } catch (IORuntimeException e) {
            logger.error("保存配置文件的时候出现了一些问题...", e);
        }
    }

    /**
     * 返回当前实例的面板
     *
     * @return 服务器面板
     */
    public JPanel getPanel() {
        return remoteClientPanel;
    }

    @Override
    protected Box loadJksSslBox() {
        return null;
    }

    @Override
    protected RemoteClientInterface getServerInterface() {
        return null;
    }
}
