package github.kasuminova.balloonserver.servers.localserver;

import github.kasuminova.balloonserver.servers.GUIServerInterface;
import github.kasuminova.balloonserver.utils.GUILogger;

import javax.swing.*;

/**
 * LittleServer 面板向外开放的接口，大部分内容都在此处交互。
 */
public interface IntegratedServerInterface extends GUIServerInterface {
    GUILogger getLogger();

    JPanel getRequestListPanel();
}
