package github.kasuminova.balloonserver.remoteclient;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.configurations.RemoteClientConfig;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.messages.MessageProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRemoteClientChannel extends SimpleChannelInboundHandler<Object> {
    protected static final int TIMEOUT = 5000;
    protected final RemoteClientInterface serverInterface;
    protected final GUILogger logger;
    protected final RemoteClientConfig config;
    protected final Map<Class<?>, MessageProcessor<?>> messageProcessors = new HashMap<>(4);
    protected ChannelHandlerContext ctx;

    public AbstractRemoteClientChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
        config = serverInterface.getRemoteClientConfig();
    }

    /**
     * 处理接收到的消息
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        MessageProcessor<Object> processor = (MessageProcessor<Object>) messageProcessors.get(msg.getClass());
        if (processor != null) {
            processor.process(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("出现问题, 已断开连接: {}", cause);
        ctx.close();
        ctx.fireChannelInactive();
    }

    /**
     * 注册消息和消息对应的处理器
     * @param clazz 消息类型
     * @param processor 消息处理函数
     */
    public void registerMessage(Class<?> clazz, MessageProcessor<?> processor) {
        messageProcessors.put(clazz, processor);
        if (BalloonServer.CONFIG.isDebugMode()) {
            logger.debug("Registered Message {}", clazz.getName());
        }
    }
}
