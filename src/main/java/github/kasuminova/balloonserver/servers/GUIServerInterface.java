package github.kasuminova.balloonserver.servers;

import github.kasuminova.balloonserver.gui.SmoothProgressBar;

import java.awt.*;

public interface GUIServerInterface extends ServerInterface {
    String getResJsonFileExtensionName();

    String getLegacyResJsonFileExtensionName();

    //获取状态栏进度条
    SmoothProgressBar getStatusProgressBar();

    void setStatusLabelText(String text, Color fg);

    void resetStatusProgressBar();
}