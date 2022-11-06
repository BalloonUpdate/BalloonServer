package github.kasuminova.balloonserver.servers;

import github.kasuminova.balloonserver.utils.GUILogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractServer {
    protected final long start = System.currentTimeMillis();
    protected final List<String> commonModeList = new ArrayList<>(0);
    protected final List<String> onceModeList = new ArrayList<>(0);
    //服务器启动状态
    protected final AtomicBoolean isStarted = new AtomicBoolean(false);
    protected final AtomicBoolean isStarting = new AtomicBoolean(false);
    //服务端是否在生成缓存，防止同一时间多个线程生成缓存导致程序混乱
    protected final AtomicBoolean isGenerating = new AtomicBoolean(false);
    protected final GUILogger logger;
    protected final String serverName;

    protected AbstractServer(String serverName) {
        this.serverName = serverName;

        //设置 Logger，主体为 logPanel
        logger = new GUILogger(serverName);
    }

    protected abstract ServerInterface getServerInterface();
}
