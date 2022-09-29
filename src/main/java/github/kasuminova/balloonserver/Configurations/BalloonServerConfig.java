package github.kasuminova.balloonserver.Configurations;

public class BalloonServerConfig extends Configuration {
    public static final CloseOperations QUERY = new CloseOperations(0, "每次询问");
    public static final CloseOperations HIDE_ON_CLOSE = new CloseOperations(1, "最小化至托盘");
    public static final CloseOperations EXIT_ON_CLOSE = new CloseOperations(2, "退出程序");
    public BalloonServerConfig() {
        configVersion = 0;
    }
    //DEBUG 模式
    private boolean debugMode = false;
    public boolean isDebugMode() {
        return debugMode;
    }
    public BalloonServerConfig setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }
    //自动启动服务器
    private boolean autoStartServer = false;
    public boolean isAutoStartServer() {
        return autoStartServer;
    }
    public BalloonServerConfig setAutoStartServer(boolean autoStartServer) {
        this.autoStartServer = autoStartServer;
        return this;
    }
    //自动启动服务器（仅一次）
    private boolean autoStartServerOnce = false;
    public boolean isAutoStartServerOnce() {
        return autoStartServerOnce;
    }
    public BalloonServerConfig setAutoStartServerOnce(boolean autoStartServerOnce) {
        this.autoStartServerOnce = autoStartServerOnce;
        return this;
    }
    //关闭窗口的操作
    private int closeOperation = 0;
    public int getCloseOperation() {
        return closeOperation;
    }
    public BalloonServerConfig setCloseOperation(int closeOperation) {
        this.closeOperation = closeOperation;
        return this;
    }
    //自动更新
    private boolean autoUpdate = false;
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
    public BalloonServerConfig setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        return this;
    }
    //自动检查更新
    private boolean autoCheckUpdates = false;
    public boolean isAutoCheckUpdates() {
        return autoCheckUpdates;
    }
    public BalloonServerConfig setAutoCheckUpdates(boolean autoCheckUpdates) {
        this.autoCheckUpdates = autoCheckUpdates;
        return this;
    }
}
