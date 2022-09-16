package github.kasuminova.balloonserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.Utils.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigurationManager {

    public static LittleServerConfig loadLittleServerConfigFromFile(String path) {
        try {
            return JSON.parseObject(Files.newInputStream(Paths.get(path)), LittleServerConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new LittleServerConfig();
        }
    }

    public static void saveConfigurationToFile(LittleServerConfig configuration, String path, String name) throws IOException {
        FileUtil.createJsonFile(JSONObject.toJSONString(configuration), path, name);
    }

    public static class LittleServerConfig {
        private int configVersion = 0;
        private String IP = "0.0.0.0";
        private int port = 8080;
        private String mainDirPath = "/res";
        private boolean miniSizeUpdateMode = true;
        private boolean fileChangeListener = true;
        private String JKSFilePath = "";
        private String JKSSSLPassword = "";
        private String[] common_mode = new String[0];
        private String[] once_mode = new String[0];

        public int getConfigVersion() {
            return configVersion;
        }
        public void setConfigVersion(int configVersion) {
            this.configVersion = configVersion;
        }
        public String getIP() {
            return IP;
        }
        public int getPort() {
            return port;
        }
        public void setPort(int port) {
            this.port = port;
        }
        public boolean isMiniSizeUpdateMode() {
            return miniSizeUpdateMode;
        }
        public void setMiniSizeUpdateMode(boolean miniSizeUpdateMode) {
            this.miniSizeUpdateMode = miniSizeUpdateMode;
        }
        public String getJKSFilePath() {
            return JKSFilePath;
        }
        public void setJKSFilePath(String JKSFilePath) {
            this.JKSFilePath = JKSFilePath;
        }
        public String getJKSSSLPassword() {
            return JKSSSLPassword;
        }
        public void setJKSSSLPassword(String JKSSSLPassword) {
            this.JKSSSLPassword = JKSSSLPassword;
        }
        public String[] getCommon_mode() {
            return common_mode;
        }
        public void setCommon_mode(String[] common_mode) {
            this.common_mode = common_mode;
        }
        public String[] getOnce_mode() {
            return once_mode;
        }
        public void setOnce_mode(String[] once_mode) {
            this.once_mode = once_mode;
        }
        public void setIP(String IP) {
            this.IP = IP;
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
