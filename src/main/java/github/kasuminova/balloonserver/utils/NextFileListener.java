package github.kasuminova.balloonserver.utils;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.Watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件监听器
 */
public class NextFileListener {
    private final WatchMonitor monitor;

    public NextFileListener(String path, AtomicBoolean isFileChanged, GUILogger logger, int maxDepth) {
        monitor = WatchMonitor.createAll(path, new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                Object obj = event.context();
                logger.info(String.format("创建: %s -> %s", currentPath, obj));
                isFileChanged.set(true);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                Object obj = event.context();
                logger.info(String.format("修改: %s -> %s", currentPath, obj));
                isFileChanged.set(true);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                Object obj = event.context();
                logger.info(String.format("删除: %s -> %s", currentPath, obj));
                isFileChanged.set(true);
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                Object obj = event.context();
                logger.info(String.format("Overflow: %s -> %s", currentPath, obj));
                isFileChanged.set(true);
            }
        });
        monitor.setMaxDepth(maxDepth);
    }

    public void start() {
        monitor.start();
    }

    public void stop() {
        monitor.close();
        monitor.interrupt();
    }
}
