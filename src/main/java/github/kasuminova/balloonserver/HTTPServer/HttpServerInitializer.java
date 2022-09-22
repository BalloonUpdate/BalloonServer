package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.Servers.LittleServerInterface;
import github.kasuminova.balloonserver.Utils.GUILogger;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author Kasumi_Nova
 */
public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
    File jks;
    char[] jksPasswd;
    private final boolean useSsl;
    private final GUILogger logger;
    public boolean isUseSsl() {
        return useSsl;
    }

    private final LittleServerInterface serverInterface;
    public HttpServerInitializer(LittleServerInterface serverInterface) {
        this.serverInterface = serverInterface;

        LittleServerConfig config = serverInterface.getConfig();
        this.logger = serverInterface.getLogger();

        if (jks != null) {
            if (jksPasswd != null && jksPasswd.length != 0) {
                this.jks = new File(config.getJksFilePath());
                this.jksPasswd = config.getJksSslPassword().toCharArray();
                useSsl = true;
                logger.info("成功载入 JKS 证书与密码，使用 HTTPS 协议。");
            } else {
                useSsl = false;
                logger.warn("检测到 JKS 证书，但未检测到 JKS 证书密码，使用 HTTP 协议。");
            }
        } else {
            useSsl = false;
            logger.info("未检测到 JKS 证书，使用 HTTP 协议。");
        }
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        //SSL, 使用 JKS 格式证书
        if (useSsl) {
            InputStream jksInputStream = Files.newInputStream(jks.toPath());
            SSLEngine engine = SslContextFactoryOne.getServerContext(jksInputStream, jksPasswd).createSSLEngine();
            //设置服务端模式
            engine.setUseClientMode(false);
            //单向认证
            engine.setNeedClientAuth(false);
            pipeline.addFirst("ssl", new SslHandler(engine));
        }

        //将消息转为单一的 FullHttpRequest或者 FullHttpResponse，因为 http 解码器在每个 http 消息中会生成多个消息对象
        pipeline.addLast("http-aggregator",new HttpObjectAggregator(65535));
        //反向代理适配器
        pipeline.addLast("decoder",new DecodeProxy(logger));
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast("httpAggregator",new HttpObjectAggregator(512 * 1024));
        pipeline.addLast("http-chunked",new ChunkedWriteHandler());
        //gzip 压缩
//        pipeline.addLast("compressor",new HttpContentCompressor());
        //请求处理器
        pipeline.addLast(new HttpRequestHandler(serverInterface));
    }
}