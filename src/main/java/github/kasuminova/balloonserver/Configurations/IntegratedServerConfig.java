package github.kasuminova.balloonserver.Configurations;

public class IntegratedServerConfig extends Configuration {
    private String ip = "127.0.0.1";
    private int port = 8080;
    private String mainDirPath = "/res";
    private boolean fileChangeListener = true;
    private String jksFilePath = "";
    private String jksSslPassword = "";
    private String[] commonMode = new String[0];
    private String[] onceMode = new String[0];
    public IntegratedServerConfig() {
        configVersion = 1;
    }

    /**
     * 重置配置文件
     */
    public IntegratedServerConfig reset() {
        configVersion = 1;
        ip = "127.0.0.1";
        port = 8080;
        mainDirPath = "/res";
        fileChangeListener = true;
        jksFilePath = "";
        jksSslPassword = "";
        commonMode = new String[0];
        onceMode = new String[0];
        return this;
    }

    @Override
    public IntegratedServerConfig setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public IntegratedServerConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getJksFilePath() {
        return jksFilePath;
    }

    public IntegratedServerConfig setJksFilePath(String jksFilePath) {
        this.jksFilePath = jksFilePath;
        return this;
    }

    public String getJksSslPassword() {
        return jksSslPassword;
    }

    public IntegratedServerConfig setJksSslPassword(String jksSslPassword) {
        this.jksSslPassword = jksSslPassword;
        return this;
    }

    public String[] getCommonMode() {
        return commonMode;
    }

    public IntegratedServerConfig setCommonMode(String[] commonMode) {
        this.commonMode = commonMode;
        return this;
    }

    public String[] getOnceMode() {
        return onceMode;
    }

    public IntegratedServerConfig setOnceMode(String[] onceMode) {
        this.onceMode = onceMode;
        return this;
    }

    public IntegratedServerConfig setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public String getMainDirPath() {
        return mainDirPath;
    }

    public IntegratedServerConfig setMainDirPath(String mainDirPath) {
        this.mainDirPath = mainDirPath;
        return this;
    }

    public boolean isFileChangeListener() {
        return fileChangeListener;
    }

    public IntegratedServerConfig setFileChangeListener(boolean fileChangeListener) {
        this.fileChangeListener = fileChangeListener;
        return this;
    }
}
