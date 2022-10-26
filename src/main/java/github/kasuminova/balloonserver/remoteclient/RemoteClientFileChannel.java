package github.kasuminova.balloonserver.remoteclient;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.FileUtil;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.balloonserver.utils.MiscUtils;
import github.kasuminova.messages.filemessages.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteClientFileChannel extends SimpleChannelInboundHandler<Object> {
    private final GUILogger logger;
    private final RemoteClientInterface serverInterface;
    private final Map<String, NetworkFile> networkFiles = new ConcurrentHashMap<>();
    private Timer fileDaemon;

    public RemoteClientFileChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        this.logger = logger;
        this.serverInterface = serverInterface;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        serverInterface.onDisconnected();
        logger.warn("出现问题, 已断开连接: {}", MiscUtils.stackTraceToString(cause));
        ctx.close();
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileMessage fileMessage) {
            String path = StrUtil.format("{}/{}", fileMessage.getFilePath(), fileMessage.getFileName());
            File file = new File(path);

            if (msg instanceof FileInfoMsg fileInfoMsg) {
                logger.info("文件信息: {}/{}, 大小: {}",
                        fileInfoMsg.getFileName(), fileInfoMsg.getFilePath(),
                        FileUtil.formatFileSizeToStr(fileInfoMsg.getSize()));
            } else if (msg instanceof FileObjMessage fileObjMsg) {
                try {
                    receiveFile(ctx, path, file, fileObjMsg);
                } catch (IOException e) {
                    logger.error(e);
                    ctx.writeAndFlush(new FileTransStopMsg(fileMessage.getFilePath(), fileMessage.getFileName()));
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void receiveFile(ChannelHandlerContext ctx, String path, File file, FileObjMessage fileObjMsg) throws IOException {
        NetworkFile networkFile = networkFiles.get(path);
        if (networkFile == null) {
            if (!file.exists()) {
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    throw new IOException("Failed To Create Directory " + file.getParentFile().getPath());
                }
                if (!file.createNewFile()) {
                    throw new IOException("Failed To Create File " + file.getPath());
                }
            }
            networkFile = new NetworkFile(System.currentTimeMillis(), new RandomAccessFile(file, "rw"));
            networkFiles.put(path, networkFile);
        }

        RandomAccessFile randomAccessFile = networkFile.randomAccessFile;

        randomAccessFile.seek(fileObjMsg.getOffset());
        randomAccessFile.write(fileObjMsg.getData());
        networkFile.lastUpdateTime = System.currentTimeMillis();
        networkFile.completedBytes = networkFile.completedBytes + fileObjMsg.getLength();

        if (networkFile.completedBytes >= fileObjMsg.getTotal()) {
            networkFiles.get(path).randomAccessFile.close();
            networkFiles.remove(path);
            logger.info("Successfully To Received File {}", path);
        } else {
            long len = fileObjMsg.getTotal() - networkFile.completedBytes;
            ctx.writeAndFlush(new FileRequestMsg(
                    fileObjMsg.getFilePath(), fileObjMsg.getFileName(),
                    networkFile.completedBytes,
                    len > 8192 ? 8192 : len));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        fileDaemon = new Timer(1000, e -> networkFiles.forEach((path, networkFile) -> {
            if (networkFile.lastUpdateTime < System.currentTimeMillis() - 5000) {
                try {
                    networkFile.randomAccessFile.close();
                    logger.warn("文件 {} 下载超时, 已停止占用本地文件.", path);
                } catch (Exception ex) {
                    logger.error(ex);
                }
                networkFiles.remove(path);
            }
        }));
        fileDaemon.start();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        networkFiles.forEach((path, networkFile) -> {
            try {
                networkFile.randomAccessFile.close();
            } catch (Exception e) {
                logger.error(e);
            }
            networkFiles.remove(path);
        });

        fileDaemon.stop();
    }

    private static class NetworkFile {
        private long lastUpdateTime;
        private final RandomAccessFile randomAccessFile;
        private long completedBytes = 0;

        public NetworkFile(long lastUpdateTime, RandomAccessFile file) {
            this.lastUpdateTime = lastUpdateTime;
            this.randomAccessFile = file;
        }
    }
}
