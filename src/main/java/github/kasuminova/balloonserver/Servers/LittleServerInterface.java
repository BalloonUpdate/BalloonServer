package github.kasuminova.balloonserver.Servers;

import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.Utils.GUILogger;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public interface LittleServerInterface {
    GUILogger getLogger();
    LittleServerConfig getConfig();
    JPanel getRequestListPanel();
    AtomicBoolean isGenerating();
    AtomicBoolean isStarted();
    /**
     * 重新生成缓存
     */
    void regenCache();
}
