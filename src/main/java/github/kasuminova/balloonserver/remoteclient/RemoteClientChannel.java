package github.kasuminova.balloonserver.remoteclient;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.gui.fileobjectbrowser.FileObjectBrowser;
import github.kasuminova.balloonserver.utils.fileobject.SimpleDirectoryObject;
import github.kasuminova.messages.*;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;

import java.util.Timer;
import java.util.TimerTask;

public class RemoteClientChannel extends AbstractRemoteClientChannel {
    private final Timer timeOutListener = new Timer();

    public RemoteClientChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        super(logger, serverInterface);
    }

    @Override
    public void onRegisterMessages() {
        //认证消息
        registerMessage(AuthSuccessMessage.class, (MessageProcessor<AuthSuccessMessage>) this::onAuthSuccess);
        //日志消息
        registerMessage(LogMessage.class, (MessageProcessor<LogMessage>) this::processLogMsg);
        //状态消息
        registerMessage(StatusMessage.class, (MessageProcessor<StatusMessage>) this::updateStatus);
        //文件夹消息
        registerMessage(SimpleDirectoryObject.class, (MessageProcessor<SimpleDirectoryObject>) RemoteClientChannel::showFileObjectBrowser);
    }

    @Override
    protected void channelActive0() {
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
    }

    private void onAuthSuccess(AuthSuccessMessage message) {
        serverInterface.onConnected(message.getConfig());
        timeOutListener.cancel();
    }

    private void processLogMsg(LogMessage message) {
        switch (message.getLevel()) {
            case "INFO" -> logger.info(message.getMessage());
            case "WARN" -> logger.warn(message.getMessage());
            case "ERROR" -> logger.error(message.getMessage());
            case "DEBUG" -> logger.debug(message.getMessage());
        }
    }

    private void updateStatus(StatusMessage statusMessage) {
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
