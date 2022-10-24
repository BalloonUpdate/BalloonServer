package github.kasuminova.balloonserver.remoteclient;

import github.kasuminova.balloonserver.gui.fileobjectbrowser.FileObjectBrowser;
import github.kasuminova.balloonserver.utils.fileobject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoteClientFileObjectChannel extends SimpleChannelInboundHandler<Object> {
    private final GUILogger logger;
    private final RemoteClientInterface serverInterface;
    public RemoteClientFileObjectChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        serverInterface.onDisconnected();
        logger.warn("出现问题, 已断开连接: {}", cause);
        ctx.close();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof SimpleDirectoryObject directoryObject) {
            showFileObjectBrowser(directoryObject);
        }
    }

    private static void showFileObjectBrowser(SimpleDirectoryObject directoryObject) {
        FileObjectBrowser objectBrowser = new FileObjectBrowser(directoryObject);
        objectBrowser.setVisible(true);
    }
}
