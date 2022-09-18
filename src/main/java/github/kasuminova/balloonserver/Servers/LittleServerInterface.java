package github.kasuminova.balloonserver.Servers;

import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.Utils.GUILogger;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LittleServer 面板向外开放的接口，大部分内容都在此处交互。
 */
public interface LittleServerInterface {
    GUILogger getLogger();
    LittleServerConfig getConfig();
    JPanel getRequestListPanel();
    AtomicBoolean isGenerating();
    AtomicBoolean isStarted();
    //获取文件结构 JSON
    String getResJson();
    //设置新的文件结构 JSON
    void setResJson(String newResJson);
    /**
     * 重新生成缓存
     */
    void regenCache();
}
