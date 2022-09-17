package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.Utils.GUILogger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * &#064;Description nginx代理 netty tcp服务端负载均衡，nginx stream要打开 proxy_protocol on; 配置
 */
public class DecodeProxy extends ByteToMessageDecoder {
    /**
     * 保存客户端IP
     */
    public static AttributeKey<String> key = AttributeKey.valueOf("IP");
    GUILogger logger;
    public DecodeProxy(GUILogger logger) {
        this.logger = logger;
    }
    /**
     * decode() 会根据接收的数据，被调用多次，直到确定没有新的元素添加到list,
     * 或者是 ByteBuf 没有更多的可读字节为止。
     * 如果 list 不为空，就会将 list 的内容传递给下一个 handler
     * @param ctx 上下文对象
     * @param byteBuf 入站后的 ByteBuf
     * @param out 将解码后的数据传递给下一个 handler
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        byte[] bytes = printSz(byteBuf);
        String message = new String(bytes, StandardCharsets.UTF_8);

        if (bytes.length > 0){
            //判断是否有代理
            if (message.contains("PROXY")){
                logger.info("PROXY MSG: " + message.substring(0,message.length()-2));
                if (message.contains("\n")){
                    String[] str =  message.split("\n")[0].split(" ");
                    logger.info("Real Client IP: " + str[2]);
                    Attribute<String> channelAttr = ctx.channel().attr(key);
                    //基于channel的属性
                    if(null == channelAttr.get()){
                        channelAttr.set(str[2]);
                    }
                }

                //清空数据，重要不能省略
                byteBuf.clear();
            }

            if (byteBuf.readableBytes() > 0){
                out.add(byteBuf.readBytes(byteBuf.readableBytes()));
            }
        }
    }

    /**
     * 打印 byte 数组
     */
    public byte[] printSz(ByteBuf newBuf){
        ByteBuf copy = newBuf.copy();
        byte[] bytes = new byte[copy.readableBytes()];
        copy.readBytes(bytes);
        return bytes;
    }
}