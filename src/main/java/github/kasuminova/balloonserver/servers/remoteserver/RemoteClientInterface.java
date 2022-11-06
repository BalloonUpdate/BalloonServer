package github.kasuminova.balloonserver.servers.remoteserver;

import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.balloonserver.servers.ServerInterface;
import io.netty.channel.ChannelHandlerContext;

public interface RemoteClientInterface extends ServerInterface {
    ChannelHandlerContext getChannel();
    void setChannel(ChannelHandlerContext ctx);

    RemoteClientConfig getRemoteClientConfig();

    void updateStatus(String memBarText, int value, int runningThreadCount, String clientIP);

    void onDisconnected();

    void onConnected(IntegratedServerConfig config);

    void showCommonRuleEditorDialog(JSONArray jsonArray);
    void showOnceRuleEditorDialog(JSONArray jsonArray);
}
