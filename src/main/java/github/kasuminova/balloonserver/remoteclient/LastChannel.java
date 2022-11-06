package github.kasuminova.balloonserver.remoteclient;

import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class LastChannel extends SimpleChannelInboundHandler<Object> {
    private final GUILogger logger;

    public LastChannel(GUILogger logger) {
        this.logger = logger;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        logger.warn("Invalid Message: {}", msg.toString());
    }
}
