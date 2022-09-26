package github.kasuminova.balloonserver.Configurations;

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
    public static void loadLittleServerConfigFromFile(String path, LittleServerConfig oldConfig) throws IOException {
        LittleServerConfig config = JSON.parseObject(Files.newInputStream(Paths.get(path)), LittleServerConfig.class);

        //配置文件版本验证
        if (config.getConfigVersion() == 0) {
            LittleServerConfig newConfig = new LittleServerConfig();
            newConfig.setIp(config.getIp());
            newConfig.setFileChangeListener(config.isFileChangeListener());
            newConfig.setPort(config.getPort());
            newConfig.setMainDirPath(config.getMainDirPath());
            newConfig.setJksFilePath(config.getJksFilePath());
            newConfig.setJksSslPassword(config.getJksSslPassword());
            return;
        }

        oldConfig.setConfigVersion(config.getConfigVersion());
        oldConfig.setIp(config.getIp());
        oldConfig.setPort(config.getPort());
        oldConfig.setMainDirPath(config.getMainDirPath());
        oldConfig.setFileChangeListener(config.isFileChangeListener());
        oldConfig.setJksFilePath(config.getJksFilePath());
        oldConfig.setJksSslPassword(config.getJksSslPassword());
        oldConfig.setCommonMode(config.getCommonMode());
        oldConfig.setOnceMode(config.getOnceMode());
    }
    public static void loadBalloonServerConfigFromFile(String path, BalloonServerConfig oldConfig) throws IOException {
        BalloonServerConfig config = JSON.parseObject(Files.newInputStream(Paths.get(path)), BalloonServerConfig.class);
        oldConfig.setAutoStartServer(config.isAutoStartServer());
        oldConfig.setAutoStartServerOnce(config.isAutoStartServerOnce());
        oldConfig.setDebugMode(config.isDebugMode());
        oldConfig.setCloseOperation(config.getCloseOperation());
        oldConfig.setAutoCheckUpdates(config.isAutoCheckUpdates());
        oldConfig.setAutoUpdate(config.isAutoUpdate());
    }

    public static void saveConfigurationToFile(Configuration configuration, String path, String name) throws IOException {
        FileUtil.createJsonFile(JSONObject.toJSONString(configuration), path, name);
    }
}
