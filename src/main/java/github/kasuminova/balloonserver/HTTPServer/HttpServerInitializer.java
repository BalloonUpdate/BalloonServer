package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.Utils.GUILogger;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLEngine;
import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
    File JKS;
    String resPath;
    String resJSON;
    char[] JKSPasswd;
    boolean useSSL;
    GUILogger logger;
    LittleServerConfig config;
    JPanel requestListPanel;
    public HttpServerInitializer(LittleServerConfig config, GUILogger logger, String resJSON, JPanel requestListPanel) {
        this.resPath = config.getMainDirPath();
        this.config = config;
        this.logger = logger;
        this.resJSON = resJSON;
        this.requestListPanel = requestListPanel;
        if (JKS != null) {
            if (JKSPasswd != null && JKSPasswd.length != 0) {
                this.JKS = new File(config.getJKSFilePath());
                this.JKSPasswd = config.getJKSSSLPassword().toCharArray();
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
        pipeline.addLast("decoder",new DecodeProxy(logger)); //反向代理适配器
        pipeline.addLast(new HttpServerCodec());// http 编解码
        pipeline.addLast("httpAggregator",new HttpObjectAggregator(512 * 1024)); // http 消息聚合器 512*1024 为接收的最大 contentLength
        pipeline.addLast("http-chunked",new ChunkedWriteHandler());
        pipeline.addLast(new HttpRequestHandler(resJSON, config, logger, requestListPanel));// 请求处理器
    }
}