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
        LittleServerConfig newConfig = JSON.parseObject(Files.newInputStream(Paths.get(path)), LittleServerConfig.class);

        oldConfig.setConfigVersion(newConfig.getConfigVersion());
        oldConfig.setIp(newConfig.getIp());
        oldConfig.setPort(newConfig.getPort());
        oldConfig.setMainDirPath(newConfig.getMainDirPath());
        oldConfig.setFileChangeListener(newConfig.isFileChangeListener());
        oldConfig.setJksFilePath(newConfig.getJksFilePath());
        oldConfig.setJksSslPassword(newConfig.getJksSslPassword());
        oldConfig.setCommonMode(newConfig.getCommonMode());
        oldConfig.setOnceMode(newConfig.getOnceMode());
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
