package github.kasuminova.balloonserver.httpserver;

import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.servers.localserver.IntegratedServerInterface;
import github.kasuminova.balloonserver.utils.FileUtil;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.balloonserver.utils.ModernColors;
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
import java.util.concurrent.atomic.AtomicLong;

import static github.kasuminova.balloonserver.BalloonServer.CONFIG;
import static github.kasuminova.balloonserver.utils.MiscUtils.formatTime;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String resJson;
    private final String legacyResJson;
    private final IntegratedServerConfig config;
    private final GUILogger logger;
    private final JPanel requestListPanel;
    private final String indexJsonString;
    private String clientIP = null;
    private String decodedURI = null;

    public HttpRequestHandler(IntegratedServerInterface serverInterface) {
        resJson = serverInterface.getResJson();
        legacyResJson = serverInterface.getLegacyResJson();
        config = serverInterface.getIntegratedServerConfig();
        logger = serverInterface.getLogger();
        requestListPanel = serverInterface.getRequestListPanel();
        indexJsonString = serverInterface.getIndexJson();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        final String uri = req.uri();
        long start = System.currentTimeMillis();

        clientIP = getClientIP(ctx, req);
        //转义后的 URI
        decodedURI = URLDecoder.decode(uri, StandardCharsets.UTF_8);

        if (CONFIG.isDebugMode()) {
            printDebugLog(req.headers().toString(), clientIP, decodedURI, logger);
        }

        //index 请求监听
        if (decodedURI.equals("/index.json")) {
            sendJson(indexJsonString, ctx);
            //打印日志
            printSuccessLog(System.currentTimeMillis() - start, "200", clientIP, decodedURI, logger);
            return;
        }

        //资源结构 JSON 请求监听
        if (decodedURI.equals(config.getMainDirPath() + "_crc32.json")) {
            sendJson(resJson, ctx);
            //打印日志
            printSuccessLog(System.currentTimeMillis() - start, "200", clientIP, decodedURI, logger);
            return;
        }

        if (decodedURI.equals(config.getMainDirPath() + ".json")) {
            //如果服务端未启用旧版兼容模式，且使用了旧版客户端获取新版服务端的缓存文件，则直接 403 请求并提示用户启用旧版兼容模式
            if (!config.isCompatibleMode() || legacyResJson == null) {
                sendError(ctx, logger, HttpResponseStatus.FORBIDDEN, "当前服务端未启用兼容模式.（仅兼容 4.1.15+）\n".repeat(6), clientIP, decodedURI);
                logger.error("警告: 检测到你可能正在使用旧版客户端获取服务端缓存文件.");
                logger.error("如果你想要兼容旧版客户端, 请按照下方描述进行操作:");
                logger.error("打开 “启用旧版兼容”, 然后保存并重载配置, 最后点击 “重新生成资源文件夹缓存”.");
                return;
            }
            sendJson(legacyResJson, ctx);
            //打印日志
            printSuccessLog(System.currentTimeMillis() - start, "200", clientIP, decodedURI, logger);
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
        if (!file.isFile()) {
            sendError(ctx, logger, HttpResponseStatus.BAD_REQUEST, "", clientIP, decodedURI);
            return;
        }

        sendFile(ctx, req, file, start);
    }

    /**
     * 发送文件
     * @param ctx ChannelHandlerContext
     * @param req HTTP 请求
     * @param file 文件
     * @param start 请求开始时间
     * @throws IOException 如果文件读取错误
     */
    private void sendFile(ChannelHandlerContext ctx, FullHttpRequest req, File file, long start) throws IOException {
        RandomAccessFile randomAccessFile;

        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            sendError(ctx, logger, HttpResponseStatus.NOT_FOUND, "", clientIP, decodedURI);
            return;
        }

        //文件大小
        long fileLength = randomAccessFile.length();

        ContentRanges ranges = new ContentRanges(req.headers().get(HttpHeaderNames.RANGE), "bytes", fileLength);

        long contentLength = ranges.getEnd() - ranges.getStart();

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);

        //设置类型为二进制流
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM);
        response.headers().set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);
        response.headers().set(HttpHeaderNames.CONTENT_RANGE, String.format("bytes %s-%s/%s", ranges.getStart(), ranges.getEnd(), fileLength));

        setContentLength(response, contentLength);
        if (isKeepAlive(req)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.write(response);

        //发送文件
        ChannelFuture sendFileFuture = ctx.write(
                new ChunkedFile(randomAccessFile,
                        ranges.getStart(), contentLength, 8192),
                ctx.newProgressivePromise()).addListener(ChannelFutureListener.CLOSE);
        SmoothProgressBar progressBar = createUploadPanel(clientIP, file.getName());

        AtomicLong fileProgress = new AtomicLong(0);
        AtomicLong totalProgress = new AtomicLong(contentLength);

        Timer timer = new Timer(250, e -> {
            progressBar.setString(String.format("%s / %s", FileUtil.formatFileSizeToStr(fileProgress.get()), FileUtil.formatFileSizeToStr(totalProgress.get())));
            progressBar.setValue((int) (fileProgress.get() * progressBar.getMaximum() / totalProgress.get()));
        });
        timer.start();

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture channelProgressiveFuture, long progress, long total) {
                fileProgress.set(progress);
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture channelProgressiveFuture) throws IOException {
                randomAccessFile.close();

                //移除进度条的容器面板
                timer.stop();
                requestListPanel.remove(progressBar.getParent().getParent());
                requestListPanel.updateUI();
                printSuccessLog(System.currentTimeMillis() - start, String.valueOf(response.status().code()), clientIP, decodedURI, logger);
            }
        });
    }

    /**
     * 发送纯 JSON
     * @param jsonString json
     * @param ctx ChannelHandlerContext
     */
    private static void sendJson(String jsonString, ChannelHandlerContext ctx) {
        //因为经过 HttpServerCodec 处理器的处理后消息被封装为 FullHttpRequest 对象
        //创建完整的响应对象
        FullHttpResponse jsonResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(jsonString, CharsetUtil.UTF_8));

        //设置头信息
        jsonResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

        //响应写回给客户端,并在协会后断开这个连接
        ctx.writeAndFlush(jsonResponse).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 打印 200 OK 日志
     *
     * @param usedTime   耗时
     * @param clientIP   客户端 IP
     * @param decodedURI 转义后的 URI
     */
    private static void printSuccessLog(long usedTime, String status ,String clientIP, String decodedURI, GUILogger logger) {
        //格式为 IP, 额外信息, 转义后的 URI, 耗时
        logger.info(
                String.format("%s\t| %s | %s | %s",
                        clientIP,
                        status,
                        formatTime(usedTime),
                        decodedURI
                ),
                ModernColors.GREEN);
    }

    /**
     * 打印非 200 OK 日志
     *
     * @param msg        额外信息
     * @param usedTime   耗时
     * @param clientIP   客户端 IP
     * @param decodedURI 转义后的 URI
     */
    private static void printWarnLog(String msg, long usedTime, String clientIP, String decodedURI, GUILogger logger) {
        //格式为 IP, 额外信息, 转义后的 URI, 耗时
        logger.warn(String.format("%s\t| %s | %s | %s",
                clientIP,
                msg,
                formatTime(usedTime),
                decodedURI));
    }

    private static void printDebugLog(String msg, String clientIP, String decodedURI ,GUILogger logger) {
        logger.debug(String.format("%s\t| %s\n%s",
                clientIP,
                decodedURI,
                msg
                ));
    }

    /**
     * 获取客户端 IP
     */
    private static String getClientIP(ChannelHandlerContext ctx, FullHttpRequest req) {
        //获取客户端 IP
        Attribute<String> channelAttr = ctx.channel().attr(DecodeProxy.key);
        if (channelAttr.get() != null) {
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

    private static void sendError(ChannelHandlerContext ctx, GUILogger logger, HttpResponseStatus status, String message, String clientIP, String decodedURI) {
        long start = System.currentTimeMillis();

        String fullMessage;
        if (message.isEmpty()) {
            fullMessage = String.format("Failure %s\r\n", status);
        } else {
            fullMessage = String.format("Failure %s: %s\r\n", status, message);
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(fullMessage, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        printWarnLog(String.valueOf(status.code()), System.currentTimeMillis() - start, clientIP, decodedURI, logger);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * 创建一个进度条面板记录单独文件的显示
     *
     * @return JProgressBar 进度条
     */
    private SmoothProgressBar createUploadPanel(String IP, String fileName) {
        //单个线程的面板
        JPanel uploadPanel = new JPanel(new VFlowLayout());
        //单个线程的 Box
        Box box = new Box(BoxLayout.LINE_AXIS);
        //文件名 IP
        uploadPanel.setBorder(BorderFactory.createTitledBorder(fileName));
        uploadPanel.add(new JLabel(IP));
        //状态
        box.add(new JLabel("进度: "), BorderLayout.WEST);
        //进度条
        SmoothProgressBar progressBar = new SmoothProgressBar(500, 250);
        progressBar.setStringPainted(true);
        //向 Box 添加进度条
        box.add(progressBar, BorderLayout.EAST);
        uploadPanel.add(box);
        requestListPanel.add(uploadPanel);
        requestListPanel.updateUI();
        return progressBar;
    }
}