package github.kasuminova.balloonserver.remoteclient;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.FileUtil;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.messages.filemessages.*;
import io.netty.channel.ChannelHandlerContext;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteClientFileChannel extends AbstractRemoteClientChannel {
    public static final int BUFFER_SIZE = 8192;
    private final Map<String, NetworkFile> networkFiles = new ConcurrentHashMap<>(4);
    private Timer fileDaemon;

    public RemoteClientFileChannel(GUILogger logger, RemoteClientInterface serverInterface) {
        super(logger, serverInterface);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;

        startFileDaemon();

        //文件信息消息
        registerMessage(FileInfoMsg.class, message0 -> printFileInfo((FileInfoMsg) message0));
        //文件对象消息
        registerMessage(FileObjMessage.class, message0 -> receiveFile((FileObjMessage) message0));

        ctx.fireChannelActive();
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

        ctx.fireChannelInactive();
    }

    private void receiveFile(FileObjMessage fileObjMsg) {
        String path = StrUtil.format("{}/{}", fileObjMsg.getFilePath(), fileObjMsg.getFileName());
        File file = new File(path);
        NetworkFile networkFile = networkFiles.get(path);

        try {
            if (networkFile == null) networkFile = createNewNetworkFile(path, file);

            RandomAccessFile randomAccessFile = networkFile.randomAccessFile;

            randomAccessFile.seek(fileObjMsg.getOffset());
            randomAccessFile.write(fileObjMsg.getData());
            networkFile.lastUpdateTime = System.currentTimeMillis();
            networkFile.completedBytes.getAndAdd(fileObjMsg.getLength());

            if (networkFile.completedBytes.get() >= fileObjMsg.getTotal()) {
                networkFiles.get(path).randomAccessFile.close();
                networkFiles.remove(path);
                logger.info("Successfully To Received File {}", path);
            } else {
                long len = fileObjMsg.getTotal() - networkFile.completedBytes.get();
                ctx.writeAndFlush(new FileRequestMsg(
                        fileObjMsg.getFilePath(), fileObjMsg.getFileName(),
                        networkFile.completedBytes.get(),
                        len > BUFFER_SIZE ? BUFFER_SIZE : len));
            }
        } catch (IOException e) {
            logger.error("接收文件的时候出现了一些问题...", e);
        }
    }

    private NetworkFile createNewNetworkFile(String path, File file) throws IOException {
        NetworkFile networkFile;
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

        return networkFile;
    }

    private void printFileInfo(FileInfoMsg message) {
        logger.info("文件信息: {}/{}, 大小: {}",
                message.getFileName(), message.getFilePath(),
                FileUtil.formatFileSizeToStr(message.getSize()));
    }

    private void startFileDaemon() {
        fileDaemon = new Timer(1000, e -> networkFiles.forEach((path, networkFile) -> {
            if (networkFile.lastUpdateTime < System.currentTimeMillis() - TIMEOUT) {
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

    private static class NetworkFile {
        private long lastUpdateTime;
        private final RandomAccessFile randomAccessFile;
        private final AtomicLong completedBytes = new AtomicLong(0);

        private NetworkFile(long lastUpdateTime, RandomAccessFile file) {
            this.lastUpdateTime = lastUpdateTime;
            this.randomAccessFile = file;
        }
    }
}
