package github.kasuminova.messages.processor;

import github.kasuminova.messages.RequestMessage;
import io.netty.channel.ChannelHandlerContext;

public class MessageProcessor {
    /**
     * 处理 RequestMessage 内容
     * @param ctx 如果请求有返回值或出现问题, 则使用此通道发送消息
     * @param message 消息
     */
    public static void processRequestMessage(ChannelHandlerContext ctx, RequestMessage message) {
        String requestType = message.getRequestType();

        switch (requestType) {

        }
    }
}
