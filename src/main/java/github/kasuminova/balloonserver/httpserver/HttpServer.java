package github.kasuminova.balloonserver.httpserver;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.servers.localserver.IntegratedServerInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.balloonserver.utils.IPAddressUtil;
import github.kasuminova.balloonserver.utils.ModernColors;
import github.kasuminova.balloonserver.utils.NextFileListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Kasumi_Nova
 * LittleServer 所绑定的服务器
 */
public class HttpServer {
    private static final int FILE_CHANGE_LISTENER_DELAY = 5000;
    private final GUILogger logger;
    private final AtomicBoolean isGenerating;
    private final AtomicBoolean isFileChanged = new AtomicBoolean(false);
    private final IntegratedServerConfig config;
    private final AtomicBoolean isStarted;
    private final IntegratedServerInterface serverInterface;
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture future;
    NextFileListener fileListener;
    private Timer fileChangeListener;
    private String apiAddress;

    /**
     * 新建一个 HTTP 服务器实例
     *
     * @param serverInterface 服务器实例接口
     */
    public HttpServer(IntegratedServerInterface serverInterface) {
        this.serverInterface = serverInterface;

        this.logger = serverInterface.getLogger();
        this.config = serverInterface.getIntegratedServerConfig();
        this.isStarted = serverInterface.isStarted();
        this.isGenerating = serverInterface.isGenerating();
    }

    public String getAPIAddress() {
        return apiAddress;
    }

    public boolean start() {
        long start = System.currentTimeMillis();
        //载入配置
        String ip = config.getIp();
        int port = config.getPort();

        ServerBootstrap bootstrap = new ServerBootstrap();
        boss = new NioEventLoopGroup();
        work = new NioEventLoopGroup();

        HttpServerInitializer httpServerInitializer = new HttpServerInitializer(serverInterface);
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(httpServerInitializer);

        try {
            future = bootstrap.bind(new InetSocketAddress(ip, port)).sync();
            String addressType = IPAddressUtil.checkAddress(ip);
            assert addressType != null;

            logger.info(String.format("服务器已启动! (%sms)", System.currentTimeMillis() - start), ModernColors.GREEN);

            if (addressType.equals("v6")) {
                if (httpServerInitializer.isUseSsl()) {
                    apiAddress = String.format("https://%s:%s/index.json",
                            StrUtil.removeSuffix(httpServerInitializer.jks.getName(), ".jks"),
                            port);
                } else {
                    apiAddress = String.format("https://[%s]:%s/index.json",
                            ip,
                            port);
                }
            } else {
                if (httpServerInitializer.isUseSsl()) {
                    apiAddress = String.format("https://%s:%s/index.json",
                            StrUtil.removeSuffix(httpServerInitializer.jks.getName(), ".jks"),
                            port);
                } else if (ip.equals("0.0.0.0")) {
                    apiAddress = String.format("http://localhost:%s/index.json",
                            port);
                } else {
                    apiAddress = String.format("http://%s:%s/index.json",
                            ip,
                            port);
                }
            }

            if (httpServerInitializer.isUseSsl()) {
                logger.info("注意: 已启用 HTTPS 协议, 你只能通过域名来访问 API 地址。如果下方输出的域名不正确, 请自行更换域名。");
            }
            logger.info(String.format("API 地址: %s", apiAddress), ModernColors.GREEN);
            serverInterface.setStatusLabelText("已启动", ModernColors.GREEN);
        } catch (Exception e) {
            isStarted.set(false);
            logger.error("无法启动服务器", e);
            return false;
        }

        //文件监听器
        if (config.isFileChangeListener()) {
            fileListener = new NextFileListener(String.format(".%s", config.getMainDirPath()), isFileChanged, logger, 10);
            fileListener.start();
            fileChangeListener = new Timer(FILE_CHANGE_LISTENER_DELAY, e -> {
                if (isFileChanged.get() && !isGenerating.get()) {
                    logger.info("检测到文件变动, 开始更新资源文件夹缓存...");
                    serverInterface.regenCache();
                    isFileChanged.set(false);
                }
            });
            fileChangeListener.start();
            logger.info("实时文件监听器已启动.");
        }
        return true;
    }

    public void stop() {
        work.shutdownGracefully();
        boss.shutdownGracefully();
        future.channel().close();

        apiAddress = null;

        //关闭文件监听器
        if (fileChangeListener != null && fileChangeListener.isRunning()) {
            fileChangeListener.stop();
            fileChangeListener = null;
        }

        if (fileListener != null) {
            fileListener.stop();
            logger.info("实时文件监听器已停止.");
            fileListener = null;
        }

        serverInterface.setStatusLabelText("就绪", ModernColors.BLUE);

        System.gc();
        logger.info("内存已完成回收.");
        logger.info("服务器已停止.");
    }
}
