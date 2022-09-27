package github.kasuminova.balloonserver.Configurations;

public class LittleServerConfig extends Configuration {
    private String ip = "127.0.0.1";
    private int port = 8080;
    private String mainDirPath = "/res";
    private boolean fileChangeListener = true;
    private String jksFilePath = "";
    private String jksSslPassword = "";
    private String[] commonMode = new String[0];
    private String[] onceMode = new String[0];
    public LittleServerConfig() {
        configVersion = 1;
    }

    /**
     * 重置配置文件
     */
    public void reset() {
        configVersion = 1;
        ip = "127.0.0.1";
        port = 8080;
        mainDirPath = "/res";
        fileChangeListener = true;
        jksFilePath = "";
        jksSslPassword = "";
        commonMode = new String[0];
        onceMode = new String[0];
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getJksFilePath() {
        return jksFilePath;
    }

    public void setJksFilePath(String jksFilePath) {
        this.jksFilePath = jksFilePath;
    }

    public String getJksSslPassword() {
        return jksSslPassword;
    }

    public void setJksSslPassword(String jksSslPassword) {
        this.jksSslPassword = jksSslPassword;
    }

    public String[] getCommonMode() {
        return commonMode;
    }

    public void setCommonMode(String[] commonMode) {
        this.commonMode = commonMode;
    }

    public String[] getOnceMode() {
        return onceMode;
    }

    public void setOnceMode(String[] onceMode) {
        this.onceMode = onceMode;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMainDirPath() {
        return mainDirPath;
    }

    public void setMainDirPath(String mainDirPath) {
        this.mainDirPath = mainDirPath;
    }

    public boolean isFileChangeListener() {
        return fileChangeListener;
    }

    public void setFileChangeListener(boolean fileChangeListener) {
        this.fileChangeListener = fileChangeListener;
    }
}
