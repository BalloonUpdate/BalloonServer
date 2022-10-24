package github.kasuminova.balloonserver.servers.remoteserver;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.configurations.ConfigurationManager;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.utils.IPAddressUtil;
import github.kasuminova.balloonserver.utils.ModernColors;
import github.kasuminova.messages.RequestMessage;
import github.kasuminova.messages.StringMessage;
import github.kasuminova.balloonserver.remoteclient.RemoteClient;
import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.channel.ChannelHandlerContext;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

import static github.kasuminova.balloonserver.BalloonServer.MAIN_FRAME;

/**
 * RemoteIntegratedClient 远程服务器客户端实例
 */
public class RemoteIntegratedServerClient {
    protected final RemoteClientConfig config = new RemoteClientConfig();
    protected final JPanel remoteClientPanel = new JPanel(new BorderLayout());
    protected final JPanel remotePanel = new JPanel(new BorderLayout());
    protected final JPanel controlPanel = new JPanel(new VFlowLayout());
    protected final JPanel connectPanel = new JPanel(new VFlowLayout());
    protected final JTextField IPTextField = new JTextField("0.0.0.0");
    protected final JSpinner portSpinner = new JSpinner();
    protected final JTextField tokenTextField = new JTextField("");
    protected final JPanel statusPanel = new JPanel(new BorderLayout());
    protected final SmoothProgressBar memBar = new SmoothProgressBar(500,250);
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

        loadControlPanel();
        controlPanel.setVisible(false);

        loadConnectPanel();
        loadStatusBar();

        remotePanel.add(controlPanel, BorderLayout.EAST);
        remotePanel.add(connectPanel, BorderLayout.WEST);
        remoteClientPanel.add(remotePanel, BorderLayout.EAST);
        remoteClientPanel.add(statusPanel, BorderLayout.SOUTH);
    }

    private void loadControlPanel() {
        controlPanel.setPreferredSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.3), 0));
        controlPanel.setBorder(new TitledBorder("远程控制面板"));

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
        controlPanel.add(msgPanel);

        JButton disconnect = new JButton("断开连接");
        disconnect.addActionListener(e -> {
            client.disconnect();
            serverInterface.onDisconnected();
        });
        controlPanel.add(disconnect);

        JButton openFileList = new JButton("打开服务端文件夹列表");
        openFileList.addActionListener(e -> remoteChannel.writeAndFlush(
                new RequestMessage("GetFileList",
                        List.of(new String[]{"./"}))));
        controlPanel.add(openFileList);
    }

    protected void loadStatusBar() {
        statusPanel.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(5, 4, 5, 4)));

        Box leftBox = Box.createHorizontalBox();
        pingLabel.setBorder(new EmptyBorder(0,0,0,20));
        leftBox.add(pingLabel);
        leftBox.add(runningThreadCount);

        Box memBarBox = Box.createHorizontalBox();
        memBar.setBorder(new EmptyBorder(0,0,0,5));
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
    }

    private void loadConnectPanel() {
        connectPanel.setPreferredSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.3), 0));
        connectPanel.setBorder(new TitledBorder("连接服务器"));

        //IP 配置
        Box IPPortBox = Box.createHorizontalBox();
        IPPortBox.add(new JLabel("远程服务器 IP:"));
        IPPortBox.add(IPTextField);

        //端口配置
        IPPortBox.add(new JLabel(" 远程端口:"), BorderLayout.WEST);
        SpinnerNumberModel portSpinnerModel = new SpinnerNumberModel(10000, 1, 65535, 1);
        portSpinner.setModel(portSpinnerModel);
        JSpinner.NumberEditor portSpinnerEditor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(portSpinnerEditor);

        IPPortBox.add(portSpinner);

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
    }

    private void loadClientInterface() {
        serverInterface = new RemoteClientInterface() {
            long lastStatusUpdated;
            @Override
            public ChannelHandlerContext getMainChannel() {
                return remoteChannel;
            }

            @Override
            public RemoteClientConfig getConfig() {
                return config;
            }

            @Override
            public void updateStatus(String memBarText, int value, int activeThreads, String clientIP) {
                memBar.setString(memBarText);
                memBar.setValue(value);
                runningThreadCount.setText(StrUtil.format("远程服务器运行的线程数: {}", activeThreads));

                int ping = (int) (System.currentTimeMillis() - lastStatusUpdated - 1000);
                pingLabel.setText(StrUtil.format("延迟: {}ms", Math.max(ping, 0)));

                if (ping < 75) {
                    pingLabel.setForeground(ModernColors.GREEN);
                } else if (ping < 150) {
                    pingLabel.setForeground(ModernColors.BLUE);
                } else if (ping < 275) {
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

        IPTextField.setText(config.getIp());
        portSpinner.setValue(config.getPort());
        tokenTextField.setText(config.getToken());
    }

    protected void reloadConfigurationFromGUI() {
        //IP 检查
        String IP = IPTextField.getText();
        String IPType = IPAddressUtil.checkAddress(IP);
        if (IPType == null) {
            IP = "0.0.0.0";
            config.setIp("0.0.0.0");
            logger.warn("配置中的 IP 格式错误，使用默认 IP 地址 0.0.0.0");
        }

        config.setPort((Integer) portSpinner.getValue())
                .setIp(IP)
                .setToken(tokenTextField.getText());

        saveConfiguration();
        logger.info("已加载配置.");
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
}
