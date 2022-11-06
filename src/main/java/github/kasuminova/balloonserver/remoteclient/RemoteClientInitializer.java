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
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        //编码器
        pipeline.addFirst(new ObjectEncoder());
        //解码器
        pipeline.addFirst(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingResolver(null)));

        pipeline.addLast("mainChannel", new RemoteClientChannel(logger, serverInterface));
        pipeline.addLast("fileObjectChannel", new RemoteClientFileChannel(logger, serverInterface));
        pipeline.addLast("lastChannel", new LastChannel(logger));
    }
}
