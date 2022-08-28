package github.kasuminova.balloonserver.Utils;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

import static github.kasuminova.balloonserver.Servers.LittleServer.logger;
import static github.kasuminova.balloonserver.Servers.LittleServer.server;

public class FileListener extends FileAlterationListenerAdaptor {
    @Override
    public void onDirectoryCreate(File directory) {
        logger.info("新建：" + directory.getAbsolutePath());
        server.isFileChanged.set(true);
    }

    @Override
    public void onDirectoryChange(File directory) {
        logger.info("修改：" + directory.getAbsolutePath());
        server.isFileChanged.set(true);
    }

    @Override
    public void onDirectoryDelete(File directory) {
        logger.info("删除：" + directory.getAbsolutePath());
        server.isFileChanged.set(true);
    }

    @Override
    public void onFileCreate(File file) {
        logger.info("新建：" + file.getAbsolutePath());
        server.isFileChanged.set(true);
    }

    @Override
    public void onFileChange(File file) {
        String compressedPath = file.getAbsolutePath();
        logger.info("修改：" + compressedPath);
        server.isFileChanged.set(true);
    }

    @Override
    public void onFileDelete(File file) {
        logger.info("删除：" + file.getAbsolutePath());
        server.isFileChanged.set(true);
    }

    public static class FileMonitor {
        private FileAlterationMonitor monitor;

        public FileMonitor(long interval) {
            monitor = new FileAlterationMonitor(interval);
        }

        /**
         * 给文件添加监听
         *
         * @param path     文件路径
         * @param listener 文件监听器
         */
        public void monitor(String path, FileAlterationListener listener) {
            FileAlterationObserver observer = new FileAlterationObserver(new File(path));
            monitor.addObserver(observer);
            observer.addListener(listener);
        }

        public void stop() throws Exception {
            monitor.stop();
        }

        public void start() throws Exception {
            monitor.start();
        }
    }
}