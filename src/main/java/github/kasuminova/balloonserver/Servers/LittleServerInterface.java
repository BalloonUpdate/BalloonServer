package github.kasuminova.balloonserver.Servers;

import github.kasuminova.balloonserver.Configurations.LittleServerConfig;
import github.kasuminova.balloonserver.Utils.GUILogger;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LittleServer 面板向外开放的接口，大部分内容都在此处交互。
 * TODO 计划添加子状态栏功能
 */
public interface LittleServerInterface {
    GUILogger getLogger();
    LittleServerConfig getConfig();
    JPanel getRequestListPanel();
    AtomicBoolean isGenerating();
    AtomicBoolean isStarted();
    //获取文件结构 JSON
    String getResJson();
    String getServerName();
    String getResJsonFileExtensionName();
    //设置新的文件结构 JSON
    void setResJson(String newResJson);

    /**
     * 重新生成缓存
     */
    void regenCache();

    /**
     * 关闭服务器
     * @return 是否关闭成功
     */
    boolean stopServer();

    /**
     * 保存配置
     */
    void saveConfig();

    /**
     * 获取 Hash 算法
     */
    String getHashAlgorithm();
}
