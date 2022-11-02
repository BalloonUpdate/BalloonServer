package github.kasuminova.balloonserver.configurations;

import cn.hutool.core.io.IORuntimeException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import github.kasuminova.balloonserver.utils.FileUtil;

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
                .setCompatibleMode(newConfig.isCompatibleMode())
                .setJksFilePath(newConfig.getJksFilePath())
                .setJksSslPassword(newConfig.getJksSslPassword())
                .setCommonMode(newConfig.getCommonMode())
                .setOnceMode(newConfig.getOnceMode());
    }

    public static void loadBalloonServerConfigFromFile(String path, BalloonServerConfig oldConfig) throws IOException {
        BalloonServerConfig config = JSON.parseObject(Files.newInputStream(Paths.get(path)), BalloonServerConfig.class);
        oldConfig.setAutoStartServer(config.isAutoStartServer())
                .setAutoStartServerOnce(config.isAutoStartServerOnce())
                .setDebugMode(config.isDebugMode())
                .setCloseOperation(config.getCloseOperation())
                .setAutoCheckUpdates(config.isAutoCheckUpdates())
                .setAutoUpdate(config.isAutoUpdate())
                .setLowIOPerformanceMode(config.isLowIOPerformanceMode())
                .setFileThreadPoolSize(config.getFileThreadPoolSize());
    }

    public static void loadRemoteClientConfigFromFile(String path, RemoteClientConfig oldConfig) throws IOException {
        RemoteClientConfig config = JSON.parseObject(Files.newInputStream(Paths.get(path)), RemoteClientConfig.class);
        oldConfig.setToken(config.getToken())
                .setIp(config.getIp())
                .setPort(config.getPort());
    }

    public static void saveConfigurationToFile(Configuration configuration, String path, String name) throws IORuntimeException {
        FileUtil.createJsonFile(JSONObject.toJSONString(configuration, JSONWriter.Feature.PrettyFormat), path, name);
    }
}
