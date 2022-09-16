package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
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

public class HttpServer {
    public AtomicBoolean isFileChanged;
    private final Timer fileChangeListener;
    private GUILogger logger;
    private String resJSON;
    private final LittleServerConfig config;
    private final JPanel requestListPanel;
    private final AtomicBoolean isStarted;
    public void setResJSON(String resJSON) {
        this.resJSON = resJSON;
    }
    public void setLogger(GUILogger logger) {
        this.logger = logger;
    }
    public HttpServer(LittleServerConfig config, JPanel requestListPanel, AtomicBoolean isStarted, AtomicBoolean isFileChanged, Timer fileChangeListener) {
        this.config = config;
        this.requestListPanel = requestListPanel;
        this.isStarted = isStarted;
        this.isFileChanged = isFileChanged;
        this.fileChangeListener = fileChangeListener;
    }
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture f;
    FileMonitor fileMonitor;
    public boolean start() {
        //载入配置
        String IP = config.getIP();
        int port = config.getPort();

        ServerBootstrap bootstrap = new ServerBootstrap();
        boss = new NioEventLoopGroup();
        work = new NioEventLoopGroup();
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(config, logger, resJSON, requestListPanel));
        try {
            f = bootstrap.bind(new InetSocketAddress(IP, port)).sync();
            String addressType = IPAddressUtil.checkAddress(IP);
            assert addressType != null;
            if (addressType.equals("v6")) {
                logger.info("服务器已启动，地址：[" + IP + "]:" + port);
            } else {
                logger.info("服务器已启动，地址：" + IP + ":" + port);
            }
        } catch (Exception e) {
            isStarted.set(false);
            logger.error("无法启动服务器", e);
        }

        //文件监听器
        if (config.isFileChangeListener()) {
            long start = System.currentTimeMillis();
            logger.info("正在启动实时文件监听器.");
            fileMonitor = new FileMonitor(5000);
            fileMonitor.monitor("." + config.getMainDirPath(), new FileListener(logger,isFileChanged));

            new Thread(() -> {
                try {
                    fileMonitor.start();
                    fileChangeListener.start();
                    logger.info("实时文件监听器已启动." + String.format(" (%sms)", System.currentTimeMillis() - start));
                } catch (Exception e) {
                    fileMonitor = null;
                    logger.error("启动文件监听器的时候出现了一些问题...",e);
                }
            }).start();
        }

        return true;
    }

    public void stop() {
        work.shutdownGracefully();
        boss.shutdownGracefully();
        f.channel().close();
        //文件监听器
        if (fileChangeListener.isRunning()) fileChangeListener.stop();
        if (fileMonitor != null) {
            new Thread(() -> {
                try {
                    fileMonitor.stop();
                    logger.info("实时文件监听器已停止.");
                } catch (Exception e) {
                    logger.error("关闭文件监听器的时候出现了一些问题...", e);
                }
                fileMonitor = null;
            });
        }
        logger.info("服务器已停止.");
    }
}
