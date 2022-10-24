package github.kasuminova.balloonserver.remoteclient;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.messages.*;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoteClientChannel extends SimpleChannelInboundHandler<Object> {
    private final GUILogger logger;
    private final RemoteClientInterface serverInterface;
    private final RemoteClientConfig config;
    public RemoteClientChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
        config = serverInterface.getRemoteClientConfig();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("已连接至服务器, 认证中...");
        ctx.writeAndFlush(new TokenMessage(config.getToken(), BalloonServer.VERSION));
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
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof StringMessage strMsg) {
            logger.info(strMsg.getMessage());
        } else if (msg instanceof ErrorMessage errMsg) {
            printErrLog(errMsg, logger);
        } else if (msg instanceof StatusMessage statusMessage) {
            updateStatus(statusMessage, serverInterface);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private static void printErrLog(ErrorMessage errMsg, GUILogger logger) {
        if (errMsg.getStackTrace().isEmpty()) {
            logger.error(errMsg.getMessage());
        } else {
            logger.error("{}\n{}", errMsg.getMessage(), errMsg.getStackTrace());
        }
    }

    private static void updateStatus(StatusMessage statusMessage, RemoteClientInterface serverInterface) {
        serverInterface.updateStatus(
                StrUtil.format("{} M / {} M - Max: {} M",
                        statusMessage.getUsed(), statusMessage.getTotal(), statusMessage.getMax()),
                (int) ((double) (statusMessage.getUsed() * 500) / statusMessage.getTotal()),
                statusMessage.getRunningThreadCount(),
                statusMessage.getClientIP());
    }
}
