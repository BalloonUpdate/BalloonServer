package github.kasuminova.balloonserver.servers.remoteserver;

import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import io.netty.channel.ChannelHandlerContext;

public interface RemoteClientInterface {
    ChannelHandlerContext getMainChannel();
    RemoteClientConfig getConfig();

    void updateStatus(String memBarText, int value, int runningThreadCount, String clientIP);

    void onDisconnected();
    void onConnected(ChannelHandlerContext ctx);
}
