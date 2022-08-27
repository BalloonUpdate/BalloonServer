package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.ConfigurationManager.LittleServerConfig;
import github.kasuminova.balloonserver.Servers.LittleServer;
import github.kasuminova.balloonserver.Utils.IPUtil.IPAddressUtil;
import github.kasuminova.balloonserver.Utils.LogManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;
import java.net.InetSocketAddress;

import static github.kasuminova.balloonserver.BalloonServer.changeListener;
import static github.kasuminova.balloonserver.BalloonServer.statusProgressBar;
import static github.kasuminova.balloonserver.Servers.LittleServer.*;

public class HttpServer{
    private int port = 8080;
    private String IP = "0.0.0.0";
    private File JKS;
    private char[] JKSPasswd;
    private LogManager logger;
    private LittleServerConfig config;
    private String resJSON;
    public void setResJSON(String resJSON) {
        this.resJSON = resJSON;
    }
    public String getResJSON() {
        return resJSON;
    }
    public void setConfig (LittleServerConfig config) {
        this.config = config;
    }
    public void setLogger(LogManager logger) {
        this.logger = logger;
    }
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture f;

    public void start() {
        //载入配置
        IP = config.getIP();
        if (new File(config.getJKSFilePath()).exists()) {
            JKS = new File(config.getJKSFilePath());
        } else {
            JKS = null;
        }
        JKSPasswd = config.getJKSSSLPassword().toCharArray();
        port = config.getPort();

        ServerBootstrap bootstrap = new ServerBootstrap();
        boss = new NioEventLoopGroup();
        work = new NioEventLoopGroup();
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(JKS, JKSPasswd, LittleServer.config.getMainDirPath(),logger));
        try {
            f = bootstrap.bind(new InetSocketAddress(IP,port)).sync();
            isStarted.set(true);
            startOrStop.setText("关闭服务器");
            if (IPAddressUtil.isIPv6LiteralAddress(IP)) {
                logger.info("服务器已启动，地址：[" + IP + "]:" + port);
            } else {
                logger.info("服务器已启动，地址：" + IP + ":" + port);
            }
        } catch (Exception e) {
            isStarted.set(false);
            logger.error("无法启动服务器", e);
        }

        statusProgressBar.setVisible(false);
        startOrStop.setEnabled(true);
        regenDirectoryStructureCache.setEnabled(true);
        //重置变动监听器
        BalloonServer.statusProgressBar.removeChangeListener(changeListener);
    }

    public void stop() {
        work.shutdownGracefully();
        boss.shutdownGracefully();
        f.channel().close();
        logger.info("服务器已停止.");
    }
}
