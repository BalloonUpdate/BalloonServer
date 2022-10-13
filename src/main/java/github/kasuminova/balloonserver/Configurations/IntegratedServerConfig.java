package github.kasuminova.balloonserver.Configurations;

import com.alibaba.fastjson2.annotation.JSONField;

public class IntegratedServerConfig extends Configuration {
    @JSONField(ordinal = 1)
    private String ip = "127.0.0.1";
    @JSONField(ordinal = 2)
    private int port = 8080;
    @JSONField(ordinal = 3)
    private String mainDirPath = "/res";
    @JSONField(ordinal = 4)
    private boolean fileChangeListener = true;
    @JSONField(ordinal = 5)
    private boolean compatibleMode = false;
    @JSONField(ordinal = 6)
    private String jksFilePath = "";
    @JSONField(ordinal = 7)
    private String jksSslPassword = "";
    @JSONField(ordinal = 8)
    private String[] commonMode = new String[0];
    @JSONField(ordinal = 9)
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

    public boolean isCompatibleMode() {
        return compatibleMode;
    }

    public IntegratedServerConfig setCompatibleMode(boolean compatibleMode) {
        this.compatibleMode = compatibleMode;
        return this;
    }
}
