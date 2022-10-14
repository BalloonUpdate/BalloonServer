package github.kasuminova.balloonserver.Servers;

import github.kasuminova.balloonserver.Configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.GUI.SmoothProgressBar;
import github.kasuminova.balloonserver.Utils.GUILogger;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LittleServer 面板向外开放的接口，大部分内容都在此处交互。
 * TODO 计划添加子状态栏功能
 */
public interface IntegratedServerInterface {
    GUILogger getLogger();
    IntegratedServerConfig getConfig();
    JPanel getRequestListPanel();
    AtomicBoolean isGenerating();
    AtomicBoolean isStarted();
    //获取文件结构 JSON
    String getResJson();
    //获取旧版文件结构 JSON
    String getLegacyResJson();
    //获取 index.json 字符串
    String getIndexJson();
    String getServerName();
    String getResJsonFileExtensionName();
    String getLegacyResJsonFileExtensionName();
    //获取状态栏进度条
    SmoothProgressBar getStatusProgressBar();
    //设置新的文件结构 JSON
    void setResJson(String newResJson);
    //设置新的旧版文件结构 JSON
    void setLegacyResJson(String newLegacyResJson);

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

    void setStatusLabelText(String text, Color fg);

    void resetStatusProgressBar();
}
