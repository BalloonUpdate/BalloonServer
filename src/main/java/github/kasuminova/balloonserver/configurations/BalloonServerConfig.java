package github.kasuminova.balloonserver.configurations;

import com.alibaba.fastjson2.annotation.JSONField;

public class BalloonServerConfig extends Configuration {
    public static final CloseOperations QUERY = new CloseOperations(0, "每次询问");
    public static final CloseOperations HIDE_ON_CLOSE = new CloseOperations(1, "最小化至托盘");
    public static final CloseOperations EXIT_ON_CLOSE = new CloseOperations(2, "退出程序");
    //自动启动服务器
    @JSONField(ordinal = 1)
    private boolean autoStartServer = false;
    //自动启动服务器（仅一次）
    @JSONField(ordinal = 2)
    private boolean autoStartServerOnce = false;
    //自动检查更新
    @JSONField(ordinal = 3)
    private boolean autoCheckUpdates = false;
    //自动更新
    @JSONField(ordinal = 4)
    private boolean autoUpdate = false;
    //关闭窗口的操作
    @JSONField(ordinal = 5)
    private int closeOperation = 0;
    //DEBUG 模式
    @JSONField(ordinal = 6)
    private boolean debugMode = false;

    public BalloonServerConfig() {
        configVersion = 0;
    }

    @Override
    public BalloonServerConfig setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
        return this;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public BalloonServerConfig setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    public boolean isAutoStartServer() {
        return autoStartServer;
    }

    public BalloonServerConfig setAutoStartServer(boolean autoStartServer) {
        this.autoStartServer = autoStartServer;
        return this;
    }

    public boolean isAutoStartServerOnce() {
        return autoStartServerOnce;
    }

    public BalloonServerConfig setAutoStartServerOnce(boolean autoStartServerOnce) {
        this.autoStartServerOnce = autoStartServerOnce;
        return this;
    }

    public int getCloseOperation() {
        return closeOperation;
    }

    public BalloonServerConfig setCloseOperation(int closeOperation) {
        this.closeOperation = closeOperation;
        return this;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public BalloonServerConfig setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        return this;
    }

    public boolean isAutoCheckUpdates() {
        return autoCheckUpdates;
    }

    public BalloonServerConfig setAutoCheckUpdates(boolean autoCheckUpdates) {
        this.autoCheckUpdates = autoCheckUpdates;
        return this;
    }
}
