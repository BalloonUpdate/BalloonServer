package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.Configurations.LittleServerConfig;
import github.kasuminova.balloonserver.Servers.LittleServerInterface;
import github.kasuminova.balloonserver.Utils.FileListener;
import github.kasuminova.balloonserver.Utils.FileListener.FileMonitor;
import github.kasuminova.balloonserver.Utils.GUILogger;
import github.kasuminova.balloonserver.Utils.IPAddressUtil;
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

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_THREAD_POOL;

/**
 * @author Kasumi_Nova
 * LittleServer 所绑定的服务器
 */
public class HttpServer {
    private Timer fileChangeListener;
    private final GUILogger logger;
    private final AtomicBoolean isGenerating;
    private final AtomicBoolean isFileChanged = new AtomicBoolean(false);
    private final LittleServerConfig config;
    private final AtomicBoolean isStarted;
    private final LittleServerInterface serverInterface;
    /**
     * 新建一个 HTTP 服务器实例
     * @param serverInterface 服务器实例接口
     */
    public HttpServer(LittleServerInterface serverInterface) {
        this.serverInterface = serverInterface;

        this.logger = serverInterface.getLogger();
        this.config = serverInterface.getConfig();
        this.isStarted = serverInterface.isStarted();
        this.isGenerating = serverInterface.isGenerating();
    }
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture future;
    FileMonitor fileMonitor;
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

            if ("v6".equals(addressType)) {
                if (httpServerInitializer.isUseSsl()) {
                    logger.info(String.format("服务器已启动 (%sms)，API 地址：http://%s:%s/index.json",
                            System.currentTimeMillis() - start,
                            httpServerInitializer.jks.getName().replace(".jks",""),
                            port));
                    logger.info("注意：已启用 HTTPS 协议，你只能通过域名来访问 API 地址，如果上方输出的域名不正确，请自行将 IP 更换为域名。");
                } else {
                    logger.info(String.format("服务器已启动 (%sms)，API 地址：https://[%s]:%s/index.json",
                            System.currentTimeMillis() - start,
                            ip,
                            port));
                }
            } else {
                if (httpServerInitializer.isUseSsl()) {
                    logger.info(String.format("服务器已启动 (%sms)，API 地址：http://%s:%s/index.json",
                            System.currentTimeMillis() - start,
                            httpServerInitializer.jks.getName().replace(".jks",""),
                            port));
                    logger.info("注意：已启用 HTTPS 协议，你只能通过域名来访问 API 地址。");
                } else {
                    logger.info(String.format("服务器已启动 (%sms)，API 地址：http://%s:%s/index.json",
                            System.currentTimeMillis() - start,
                            ip,
                            port));
                }
            }
        } catch (Exception e) {
            isStarted.set(false);
            logger.error("无法启动服务器", e);
            return false;
        }

        //文件监听器
        if (config.isFileChangeListener()) {
            long start1 = System.currentTimeMillis();
            logger.info("正在启动实时文件监听器.");
            fileMonitor = new FileMonitor(5000);
            fileMonitor.monitor("." + config.getMainDirPath(), new FileListener(logger,isFileChanged));

            GLOBAL_THREAD_POOL.execute(() -> {
                try {
                    fileMonitor.start();
                    fileChangeListener = new Timer(2000, e -> {
                        if (isFileChanged.get() && !isGenerating.get()) {
                            logger.info("检测到文件变动, 开始更新资源文件夹缓存...");
                            serverInterface.regenCache();
                            isFileChanged.set(false);
                        }
                    });
                    fileChangeListener.start();
                    logger.info("实时文件监听器已启动." + String.format(" (%sms)", System.currentTimeMillis() - start1));
                } catch (Exception e) {
                    fileMonitor = null;
                    logger.error("启动文件监听器的时候出现了一些问题...",e);
                }
            });
        }
        return true;
    }

    public void stop() {
        work.shutdownGracefully();
        boss.shutdownGracefully();
        future.channel().close();
        //关闭文件监听器
        if (fileChangeListener != null && fileChangeListener.isRunning()) {
            fileChangeListener.stop();
            fileChangeListener = null;
        }

        if (fileMonitor != null) {
            GLOBAL_THREAD_POOL.execute(() -> {
                try {
                    fileMonitor.stop();
                    logger.info("实时文件监听器已停止.");
                } catch (Exception e) {
                    logger.error("关闭文件监听器的时候出现了一些问题...", e);
                }
                fileMonitor = null;
            });
        }

        System.gc();
        logger.info("内存已完成回收.");
        logger.info("服务器已停止.");
    }
}
