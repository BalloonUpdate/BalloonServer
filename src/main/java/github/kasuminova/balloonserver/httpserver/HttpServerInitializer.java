package github.kasuminova.balloonserver.httpserver;

import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.servers.IntegratedServerInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
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
    private final boolean useSsl;
    private final GUILogger logger;
    private final IntegratedServerInterface serverInterface;
    File jks;
    char[] jksPasswd;

    public HttpServerInitializer(IntegratedServerInterface serverInterface) {
        this.serverInterface = serverInterface;

        IntegratedServerConfig config = serverInterface.getConfig();
        this.logger = serverInterface.getLogger();

        if (config.getJksFilePath().isEmpty() || config.getJksSslPassword().isEmpty()) {
            useSsl = false;
            return;
        }

        this.jks = new File(config.getJksFilePath());
        this.jksPasswd = config.getJksSslPassword().toCharArray();

        if (jks.exists()) {
            if (jksPasswd != null && jksPasswd.length != 0) {
                useSsl = true;
                logger.info("成功载入 JKS 证书与密码, 使用 HTTPS 协议。");
            } else {
                useSsl = false;
                logger.warn("检测到 JKS 证书, 但是 JKS 证书密码为空, 使用 HTTP 协议。");
            }
        } else {
            useSsl = false;
            logger.info("未检测到 JKS 证书, 使用 HTTP 协议。");
        }
    }

    public boolean isUseSsl() {
        return useSsl;
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

        //反向代理适配器
        pipeline.addLast("decoder", new DecodeProxy(logger));

        pipeline.addLast("http-decoder", new HttpRequestDecoder());
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("http-encoder", new HttpResponseEncoder());
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());

        //gzip 压缩
//        pipeline.addLast("compressor",new HttpContentCompressor());
        //请求处理器
        pipeline.addLast("http-handler", new HttpRequestHandler(serverInterface));
//        pipeline.addLast("http-handler", new TestHttpRequestHandler("/"));
    }
}