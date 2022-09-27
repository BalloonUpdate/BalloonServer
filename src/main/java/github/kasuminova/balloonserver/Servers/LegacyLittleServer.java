package github.kasuminova.balloonserver.Servers;

import github.kasuminova.balloonserver.Configurations.ConfigurationManager;
import github.kasuminova.balloonserver.Configurations.LittleServerConfig;
import github.kasuminova.balloonserver.Utils.HashCalculator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * 旧版 LittleServer 面板实例，支持多实例化，兼容 4.x.x - 4.1.14 版本的客户端
 * @author Kasumi_Nova
 */
public class LegacyLittleServer extends AbstractLittleServer {
    public LegacyLittleServer(String serverName, boolean autoStart) {
        super(serverName, autoStart);
        this.resJsonFileExtensionName = "legacy_res-cache";
        this.hashAlgorithm = HashCalculator.SHA1;
    }

    @Override
    protected void saveConfigurationToFile() {
        reloadConfigurationFromGUI();
        try {
            ConfigurationManager.saveConfigurationToFile(config,"./",serverName + ".legacy.lscfg");
            logger.info("已保存配置至磁盘.");
        } catch (IOException ex) {
            logger.error("保存配置文件的时候出现了问题...", ex);
        }
    }

    /**
     * 从文件加载配置文件
     */
    protected void loadConfigurationFromFile() {
        if (!new File("./" + serverName + ".legacy.lscfg.json").exists()) {
            try {
                logger.warn("未找到配置文件，正在尝试在程序当前目录生成配置文件...");
                ConfigurationManager.saveConfigurationToFile(new LittleServerConfig(), "./", String.format("%s.legacy.lscfg", serverName));
                logger.info("配置生成成功.");
                logger.info("目前正在使用程序默认配置.");
            } catch (Exception e) {
                logger.error("生成配置文件的时候出现了问题...", e);
                logger.info("目前正在使用程序默认配置.");
            }
            return;
        }
        try {
            ConfigurationManager.loadLittleServerConfigFromFile("./" + serverName + ".legacy.lscfg.json", config);
        } catch (IOException e) {
            logger.error("加载配置文件的时候出现了问题...", e);
            logger.info("目前正在使用程序默认配置.");
            return;
        }
        //IP
        IPTextField.setText(config.getIp());
        //端口
        portSpinner.setValue(config.getPort());
        //资源文件夹
        mainDirTextField.setText(config.getMainDirPath());
        //实时文件监听器
        fileChangeListener.setSelected(config.isFileChangeListener());
        //Jks 证书
        if (!config.getJksFilePath().isEmpty()) {
            File JksSsl = new File(config.getJksFilePath());
            if (JksSsl.exists()) {
                JksSslTextField.setText(JksSsl.getName());
            } else {
                logger.warn("JKS 证书文件不存在.");
            }
        }
        //Jks 证书密码
        JksSslPassField.setText(config.getJksSslPassword());
        //普通模式
        commonModeList.clear();
        commonModeList.addAll(Arrays.asList(config.getCommonMode()));
        commonMode.setListData(config.getCommonMode());
        //补全模式
        onceModeList.clear();
        onceModeList.addAll(Arrays.asList(config.getOnceMode()));
        onceMode.setListData(config.getOnceMode());

        logger.info("已载入配置文件.");
    }
}
