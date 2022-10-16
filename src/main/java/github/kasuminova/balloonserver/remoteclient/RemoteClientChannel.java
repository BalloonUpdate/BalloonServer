package github.kasuminova.balloonserver.remoteclient;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.balloonserver.gui.fileobjectbrowser.FileObjectBrowser;
import github.kasuminova.messages.*;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.messages.fileobject.LiteDirectoryObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoteClientChannel extends SimpleChannelInboundHandler<AbstractMessage> {
    private final GUILogger logger;
    private final RemoteClientInterface serverInterface;
    private final RemoteClientConfig config;
    public RemoteClientChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
        config = serverInterface.getConfig();
    }

    String str;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("认证中...");
        ctx.writeAndFlush(new TokenMessage(config.getToken(), BalloonServer.VERSION.toString()));
        serverInterface.onConnected(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        serverInterface.onDisconnected();
        logger.warn("出现问题, 已断开连接: {}", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        serverInterface.onDisconnected();
        logger.info("已从服务器断开连接.");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) {
        if (msg instanceof StringMessage strMsg) {
            str = strMsg.getMessage();
            logger.info(str);
        } else if (msg instanceof ErrorMessage errMsg) {
            str = errMsg.getMessage();
            if (errMsg.getStackTrace().isEmpty()) {
                logger.error(str);
            } else {
                logger.error("{}\n{}", str, errMsg.getStackTrace());
            }
        } else if (msg instanceof StatusMessage statusMessage) {
            serverInterface.updateStatus(
                    StrUtil.format("{} M / {} M - Max: {} M",
                            statusMessage.getUsed(), statusMessage.getTotal(), statusMessage.getMax()),
                    (int) ((double) (statusMessage.getUsed() * 500) / statusMessage.getTotal()),
                    statusMessage.getRunningThreadCount(),
                    statusMessage.getClientIP());
        } else if (msg instanceof LiteDirectoryObject directoryObject) {
            FileObjectBrowser objectBrowser = new FileObjectBrowser(directoryObject);
            objectBrowser.setVisible(true);
        } else {
            str = msg.getMessageType();
            logger.info("从服务端接收到未知消息类型: {}", str);
        }
    }
}
