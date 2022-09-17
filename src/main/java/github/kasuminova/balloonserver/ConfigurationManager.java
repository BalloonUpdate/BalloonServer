package github.kasuminova.balloonserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.Utils.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Kasumi_Nova
 */
public class ConfigurationManager {

    public static LittleServerConfig loadLittleServerConfigFromFile(String path) throws IOException {
        LittleServerConfig config = JSON.parseObject(Files.newInputStream(Paths.get(path)), LittleServerConfig.class);

        //配置文件版本验证
        if (config.getConfigVersion() == 0) {
            LittleServerConfig newConfig = new LittleServerConfig();
            newConfig.setIp(config.getIp());
            newConfig.setFileChangeListener(config.fileChangeListener);
            newConfig.setPort(config.getPort());
            newConfig.setMainDirPath(config.getMainDirPath());
            newConfig.setJksFilePath(config.getJksFilePath());
            newConfig.setJksSslPassword(config.getJksSslPassword());

            return newConfig;
        }

        return config;
    }

    public static void saveConfigurationToFile(LittleServerConfig configuration, String path, String name) throws IOException {
        FileUtil.createJsonFile(JSONObject.toJSONString(configuration), path, name);
    }

    public static class LittleServerConfig {
        private int configVersion = 1;
        private String ip = "0.0.0.0";
        private int port = 8080;
        private String mainDirPath = "/res";
        private boolean fileChangeListener = true;
        private String jksFilePath = "";
        private String jksSslPassword = "";
        private String[] commonMode = new String[0];
        private String[] onceMode = new String[0];

        public int getConfigVersion() {
            return configVersion;
        }
        public void setConfigVersion(int configVersion) {
            this.configVersion = configVersion;
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
}
