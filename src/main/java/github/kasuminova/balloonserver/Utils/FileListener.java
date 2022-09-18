package github.kasuminova.balloonserver.Utils;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Kasumi_Nova
 */
public class FileListener extends FileAlterationListenerAdaptor {
    private final GUILogger logger;
    private final AtomicBoolean isFileChanged;
    public FileListener(GUILogger logger, AtomicBoolean isFileChanged) {
        this.logger = logger;
        this.isFileChanged = isFileChanged;
    }
    @Override
    public void onDirectoryCreate(File directory) {
        logger.info("新建：" + directory.getPath());
        isFileChanged.set(true);
    }

    @Override
    public void onDirectoryChange(File directory) {
        logger.info("修改：" + directory.getPath());
        isFileChanged.set(true);
    }

    @Override
    public void onDirectoryDelete(File directory) {
        logger.info("删除：" + directory.getPath());
        isFileChanged.set(true);
    }

    @Override
    public void onFileCreate(File file) {
        logger.info("新建：" + file.getPath());
        isFileChanged.set(true);
    }

    @Override
    public void onFileChange(File file) {
        logger.info("修改：" + file.getPath());
        isFileChanged.set(true);
    }

    @Override
    public void onFileDelete(File file) {
        logger.info("删除：" + file.getPath());
        isFileChanged.set(true);
    }

    public static class FileMonitor {
        private final FileAlterationMonitor monitor;

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