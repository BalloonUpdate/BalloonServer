package github.kasuminova.balloonserver.HTTPServer;

import github.kasuminova.balloonserver.Configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.GUI.LayoutManager.VFlowLayout;
import github.kasuminova.balloonserver.Servers.IntegratedServerInterface;
import github.kasuminova.balloonserver.Utils.FileUtil;
import github.kasuminova.balloonserver.Utils.GUILogger;
import github.kasuminova.balloonserver.Utils.HashCalculator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.Attribute;
import io.netty.util.CharsetUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String resJson;
    private final IntegratedServerConfig config;
    private final GUILogger logger;
    private final JPanel requestListPanel;
    private final String hashAlgorithm;
    private final String indexJsonString;
    public HttpRequestHandler(IntegratedServerInterface serverInterface) {
        this.resJson = serverInterface.getResJson();
        this.config = serverInterface.getConfig();
        this.logger = serverInterface.getLogger();
        this.requestListPanel = serverInterface.getRequestListPanel();
        this.hashAlgorithm = serverInterface.getHashAlgorithm();
        this.indexJsonString = serverInterface.getIndexJson();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        final String uri = req.uri();
        long start = System.currentTimeMillis();

        String clientIP = getClientIP(ctx,req);
        //转义后的 URI
        String decodedURI = URLDecoder.decode(uri, StandardCharsets.UTF_8);

        //index 请求监听
        if (decodedURI.equals("/index.json")) {
            //因为经过 HttpServerCodec 处理器的处理后消息被封装为 FullHttpRequest 对象
            //创建完整的响应对象
            FullHttpResponse jsonResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(indexJsonString, CharsetUtil.UTF_8));

            //设置头信息
            jsonResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

            //响应写回给客户端,并在协会后断开这个连接
            ctx.writeAndFlush(jsonResponse).addListener(ChannelFutureListener.CLOSE);

            //打印日志
            printLog(String.valueOf(HttpResponseStatus.OK.code()), start, clientIP, decodedURI, logger);
            return;
        }

        //如果使用旧版客户端获取新版服务端的缓存文件，则直接 403 请求并提示用户使用旧版服务端
        if (hashAlgorithm.equals(HashCalculator.CRC32) && decodedURI.equals(config.getMainDirPath() + ".json")) {
            sendError(ctx, logger, HttpResponseStatus.FORBIDDEN, "当前服务端版本不兼容此版本客户端，请使用旧版服务端.（仅兼容 4.1.14+）\n".repeat(6), clientIP, decodedURI);
            logger.error("检测到你可能正在使用旧版客户端获取新版服务端缓存文件，请使用旧版服务端。");
            return;
        }

        //资源结构 JSON 请求监听
        if (decodedURI.equals(config.getMainDirPath() + ".json") || decodedURI.equals(config.getMainDirPath() + "_crc32.json") && hashAlgorithm.contains(HashCalculator.CRC32)) {
            // 经过 HttpServerCodec 处理器的处理后消息被封装为 FullHttpRequest 对象
            // 创建完整的响应对象
            FullHttpResponse jsonResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(resJson, CharsetUtil.UTF_8));

            //设置头信息
            jsonResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

            // 响应写回给客户端,并在协会后断开这个连接
            ctx.writeAndFlush(jsonResponse).addListener(ChannelFutureListener.CLOSE);

            //打印日志
            printLog(String.valueOf(jsonResponse.status().code()), start, clientIP, decodedURI, logger);
            return;
        }

        //安全性检查
        if (!decodedURI.startsWith(config.getMainDirPath())) {
            sendError(ctx, logger, HttpResponseStatus.FORBIDDEN, "", clientIP, decodedURI);
            return;
        }

        //文件请求监听
        File file = new File("." + decodedURI);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, logger, HttpResponseStatus.NOT_FOUND, "", clientIP, decodedURI);
            return;
        }
        if (!file.isFile()){
            sendError(ctx, logger, HttpResponseStatus.BAD_REQUEST, "", clientIP, decodedURI);
            return;
        }

        RandomAccessFile randomAccessFile;

        try {
            randomAccessFile = new RandomAccessFile(file,"r");  //只读模式
        } catch (FileNotFoundException e) {
            sendError(ctx, logger, HttpResponseStatus.NOT_FOUND, "", clientIP, decodedURI);
            return;
        }
        long fileLength = randomAccessFile.length();    //文件大小
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
        setContentLength(response,fileLength);
        if (isKeepAlive(req)){
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.write(response);

        //发送文件
        ChannelFuture sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), ctx.newProgressivePromise());
        JProgressBar progressBar = createUploadPanel(getClientIP(ctx,req), file.getName());

        final long[] fileProgress = {0};

        Timer timer = new Timer(100, e -> {
            progressBar.setString(String.format("%s / %s", FileUtil.formatFileSizeToStr(fileProgress[0]), FileUtil.formatFileSizeToStr(fileLength)));
            progressBar.setValue((int) (fileProgress[0] * 200 / fileLength) );
        });
        timer.start();

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture channelProgressiveFuture, long progress, long total) {
                fileProgress[0] = progress;
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture channelProgressiveFuture) {
                timer.stop();
                //移除进度条的容器面板
                requestListPanel.remove(progressBar.getParent().getParent());
                requestListPanel.updateUI();
                printLog(String.valueOf(HttpResponseStatus.OK.code()), start, clientIP, decodedURI, logger);
            }
        });

        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 创建一个进度条面板记录单独文件的显示
     * @return JProgressBar 进度条
     */
    public JProgressBar createUploadPanel(String IP, String fileName) {
        //单个线程的面板
        JPanel uploadPanel = new JPanel(new VFlowLayout());
        //单个线程的 Box
        Box box = new Box(BoxLayout.LINE_AXIS);
        //文件名 IP
        uploadPanel.setBorder(BorderFactory.createTitledBorder(fileName));
        uploadPanel.add(new JLabel(IP));
        //状态
        box.add(new JLabel("进度："), BorderLayout.WEST);
        //进度条
        JProgressBar progressBar = new JProgressBar(0,200);
        progressBar.setStringPainted(true);
        //向 Box 添加进度条
        box.add(progressBar, BorderLayout.EAST);
        uploadPanel.add(box);
        requestListPanel.add(uploadPanel);
        requestListPanel.updateUI();
        return progressBar;
    }

    /**
     * 打印日志
     * @param msg 额外信息
     * @param startTime 耗时
     * @param clientIP 客户端 IP
     * @param decodedURI 转义后的 URI
     */
    private static void printLog(String msg, long startTime, String clientIP, String decodedURI, GUILogger logger) {
        //格式为 IP, 额外信息, 转义后的 URI, 耗时
        logger.info(String.format("%s %s URI: %s (%sms)", clientIP, msg, decodedURI, System.currentTimeMillis() - startTime));
    }

    /**
     * 获取客户端 IP
     */
    private static String getClientIP(ChannelHandlerContext ctx, FullHttpRequest req) {
        //获取客户端 IP
        Attribute<String> channelAttr = ctx.channel().attr(DecodeProxy.key);
        if (channelAttr.get() != null){
            return channelAttr.get();
        } else {
            String clientIP = req.headers().get("X-Forwarded-For");
            if (clientIP == null) {
                InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
                clientIP = socket.getAddress().getHostAddress();
            }
            return clientIP;
        }
    }

    private static void sendError(ChannelHandlerContext ctx, GUILogger logger, HttpResponseStatus status, String message, String clientIP, String decodedURI){
        long start = System.currentTimeMillis();
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,status,Unpooled.copiedBuffer(String.format("Failure %s: %s \r\n", status, message), CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE,HttpHeaderValues.TEXT_PLAIN);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        printLog(String.valueOf(status.code()), start, clientIP, decodedURI, logger);
    }
}