package github.kasuminova.balloonserver.remoteclient;

import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class RemoteClient {
    private final GUILogger logger;
    private final RemoteClientInterface serverInterface;
    EventLoopGroup group;
    ChannelFuture future;

    public RemoteClient(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
    }

    public void connect() throws Exception {
        group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new RemoteClientInitializer(logger, serverInterface));

        logger.info("连接中...");
        future = bootstrap.connect("127.0.0.1", 10000).sync();
    }

    public void disconnect() {
        try {
            group.shutdownGracefully();

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
