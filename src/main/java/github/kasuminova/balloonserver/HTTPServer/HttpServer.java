package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.Servers.LittleServer;
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
import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static github.kasuminova.balloonserver.BalloonServer.changeListener;
import static github.kasuminova.balloonserver.BalloonServer.statusProgressBar;
import static github.kasuminova.balloonserver.Servers.LittleServer.*;

public class HttpServer {
    public AtomicBoolean isFileChanged = new AtomicBoolean(false);
    private Timer fileChangeListener;
    private GUILogger logger;
    private String resJSON;
    public void setResJSON(String resJSON) {
        this.resJSON = resJSON;
    }
    public String getResJSON() {
        return resJSON;
    }
    public void setLogger(GUILogger logger) {
        this.logger = logger;
    }
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture f;
    FileMonitor fileMonitor;
    public void start() {
        //载入配置
        String IP = config.getIP();
        File JKS;
        if (new File(config.getJKSFilePath()).exists()) {
            JKS = new File(config.getJKSFilePath());
        } else {
            JKS = null;
        }
        char[] JKSPasswd = config.getJKSSSLPassword().toCharArray();
        int port = config.getPort();

        ServerBootstrap bootstrap = new ServerBootstrap();
        boss = new NioEventLoopGroup();
        work = new NioEventLoopGroup();
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(JKS, JKSPasswd, LittleServer.config.getMainDirPath(),logger));
        try {
            f = bootstrap.bind(new InetSocketAddress(IP, port)).sync();
            startOrStop.setText("关闭服务器");
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
            fileMonitor = new FileMonitor(5000);
            fileMonitor.monitor("." + config.getMainDirPath(), new FileListener());
            threadPool.execute(() -> {
                try {
                    fileMonitor.start();
                    fileChangeListener = new Timer(2000, e -> {
                        if (isFileChanged.get() && !isGenerating) {
                            logger.info("检测到文件变动, 开始更新资源文件夹缓存...");
                            LittleServer.regenCache();
                            isFileChanged.set(false);
                        }
                    });
                    fileChangeListener.start();
                } catch (Exception e) {
                    fileMonitor = null;
                    logger.error("启动文件监听器的时候出现了一些问题...",e);
                }
                logger.info("实时文件监听器已启动.");
            });
        }

        isStarted.set(true);
        statusProgressBar.setVisible(false);
        startOrStop.setEnabled(true);
        regenDirectoryStructureCache.setEnabled(true);
        //重置变动监听器
        BalloonServer.statusProgressBar.removeChangeListener(changeListener);
    }

    public void stop() throws Exception {
        work.shutdownGracefully();
        boss.shutdownGracefully();
        f.channel().close();
        //文件监听器
        if (fileChangeListener.isRunning()) fileChangeListener.stop();
        if (fileMonitor != null) {
            fileMonitor.stop();
            fileMonitor = null;
            logger.info("实时文件监听器已停止.");
        }
        logger.info("服务器已停止.");
    }
}
