package github.kasuminova.balloonserver.servers.remoteserver;

import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.balloonserver.servers.ServerInterface;
import io.netty.channel.ChannelHandlerContext;

public interface RemoteClientInterface extends ServerInterface {
    ChannelHandlerContext getMainChannel();

    RemoteClientConfig getRemoteClientConfig();

    void updateStatus(String memBarText, int value, int runningThreadCount, String clientIP);

    void onDisconnected();

    void onConnected(ChannelHandlerContext ctx);

    void showCommonRuleEditorDialog(JSONArray jsonArray);
    void showOnceRuleEditorDialog(JSONArray jsonArray);
}
