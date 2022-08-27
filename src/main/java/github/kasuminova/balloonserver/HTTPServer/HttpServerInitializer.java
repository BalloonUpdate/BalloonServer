package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.Utils.LogManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
    File JKS;
    String resPath;
    char[] JKSPasswd;
    public static boolean useSSL = false;
    public HttpServerInitializer(@Nullable File JKS, @Nullable char[] JKSPasswd,String resPath,LogManager logger) {
        this.resPath = resPath;
        if (JKS != null) {
            if (JKSPasswd != null && JKSPasswd.length != 0) {
                this.JKS = JKS;
                this.JKSPasswd = JKSPasswd;
                useSSL = true;
                logger.info("成功载入 JKS 证书与密码，使用 HTTPS 协议。");
            } else {
                useSSL = false;
                logger.warn("检测到 JKS 证书，但未检测到 JKS 证书密码，使用 HTTP 协议。");
            }
        } else {
            useSSL = false;
            logger.info("未检测到 JKS 证书，使用 HTTP 协议。");
        }
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        //SSL, 使用 JKS 格式证书
        if (useSSL) {
            InputStream JKSInputStream = Files.newInputStream(JKS.toPath());
            SSLEngine engine = SslContextFactoryOne.getServerContext(JKSInputStream, JKSPasswd).createSSLEngine();
            //设置服务端模式
            engine.setUseClientMode(false);
            //单向认证
            engine.setNeedClientAuth(false);
            pipeline.addFirst("ssl", new SslHandler(engine));
        }

        //将消息转为单一的 FullHttpRequest或者 FullHttpResponse，因为 http 解码器在每个 http 消息中会生成多个消息对象
        pipeline.addLast("http-aggregator",new HttpObjectAggregator(65535));
        pipeline.addLast("decoder",new DecodeProxy()); //IP 获取
        pipeline.addLast(new HttpServerCodec());// http 编解码
        pipeline.addLast("httpAggregator",new HttpObjectAggregator(512 * 1024)); // http 消息聚合器 512*1024 为接收的最大 contentlength
        pipeline.addLast("http-chunked",new ChunkedWriteHandler());
        pipeline.addLast(new HttpRequestHandler());// 请求处理器
    }
}