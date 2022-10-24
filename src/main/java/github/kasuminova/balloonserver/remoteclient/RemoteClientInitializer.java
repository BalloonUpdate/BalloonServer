package github.kasuminova.balloonserver.remoteclient;

import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class RemoteClientInitializer extends ChannelInitializer<SocketChannel> {
    private final GUILogger logger;
    private final RemoteClientInterface serverInterface;
    public RemoteClientInitializer(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        //编码器
        pipeline.addLast(new ObjectEncoder());
        //解码器
        pipeline.addLast(new ObjectDecoder(ClassResolvers.weakCachingResolver(Object.class.getClassLoader())));

        pipeline.addLast("mainChannel", new RemoteClientChannel(logger, serverInterface));
        pipeline.addLast("fileObjectChannel", new RemoteClientFileObjectChannel(logger, serverInterface));
    }
}
