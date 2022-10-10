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
    public static void loadLittleServerConfigFromFile(String path, IntegratedServerConfig oldConfig) throws IOException {
        IntegratedServerConfig newConfig = JSON.parseObject(Files.newInputStream(Paths.get(path)), IntegratedServerConfig.class);

        oldConfig.setConfigVersion(newConfig.getConfigVersion())
                 .setIp(newConfig.getIp())
                 .setPort(newConfig.getPort())
                 .setMainDirPath(newConfig.getMainDirPath())
                 .setFileChangeListener(newConfig.isFileChangeListener())
                 .setJksFilePath(newConfig.getJksFilePath())
                 .setJksSslPassword(newConfig.getJksSslPassword())
                 .setCommonMode(newConfig.getCommonMode())
                 .setOnceMode(newConfig.getOnceMode());
    }
    public static void loadBalloonServerConfigFromFile(String path, BalloonServerConfig oldConfig) throws IOException {
        BalloonServerConfig config = JSON.parseObject(Files.newInputStream(Paths.get(path)), BalloonServerConfig.class);
        oldConfig.setAutoStartServer(config.isAutoStartServer())
                 .setAutoStartServerOnce(config.isAutoStartServerOnce())
                 .setAutoStartLegacyServer(config.isAutoStartLegacyServer())
                 .setAutoStartLegacyServerOnce(config.isAutoStartLegacyServerOnce())
                 .setDebugMode(config.isDebugMode())
                 .setCloseOperation(config.getCloseOperation())
                 .setAutoCheckUpdates(config.isAutoCheckUpdates())
                 .setAutoUpdate(config.isAutoUpdate());
    }

    public static void saveConfigurationToFile(Configuration configuration, String path, String name) throws IOException {
        FileUtil.createJsonFile(JSONObject.toJSONString(configuration), path, name);
    }
}
