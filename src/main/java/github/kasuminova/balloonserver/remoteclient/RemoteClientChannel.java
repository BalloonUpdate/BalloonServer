package github.kasuminova.balloonserver.remoteclient;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.balloonserver.gui.fileobjectbrowser.FileObjectBrowser;
import github.kasuminova.balloonserver.utils.fileobject.SimpleDirectoryObject;
import github.kasuminova.messages.*;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Timer;
import java.util.TimerTask;

public class RemoteClientChannel extends SimpleChannelInboundHandler<Object> {
    private static final int TIMEOUT = 5000;
    private final GUILogger logger;
    private final RemoteClientInterface serverInterface;
    private final RemoteClientConfig config;
    private final Timer timeOutListener = new Timer();
    public RemoteClientChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
        config = serverInterface.getRemoteClientConfig();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("已连接至服务器, 认证中...");
        timeOutListener.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.warn("认证超时.");
                timeOutListener.cancel();
                ctx.close();
            }
        }, TIMEOUT, TIMEOUT);
        ctx.writeAndFlush(new TokenMessage(config.getToken(), BalloonServer.VERSION));
        ctx.fireChannelActive();
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

        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof AuthSuccessMessage authSuccessMessage) {
            serverInterface.onConnected(ctx, authSuccessMessage.getConfig());
            timeOutListener.cancel();
        } else if (msg instanceof LogMessage logMsg) {
            switch (logMsg.getLevel()) {
                case "INFO" -> logger.info(logMsg.getMessage());
                case "WARN" -> logger.warn(logMsg.getMessage());
                case "ERROR" -> logger.error(logMsg.getMessage());
                case "DEBUG" -> logger.debug(logMsg.getMessage());
            }
        } else if (msg instanceof StatusMessage statusMessage) {
            updateStatus(statusMessage, serverInterface);
        } else if (msg instanceof SimpleDirectoryObject directoryObject) {
            showFileObjectBrowser(directoryObject);
        } else {
            ctx.fireChannelRead(msg);
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

    private static void showFileObjectBrowser(SimpleDirectoryObject directoryObject) {
        FileObjectBrowser objectBrowser = new FileObjectBrowser(directoryObject);
        objectBrowser.setVisible(true);
    }
}
