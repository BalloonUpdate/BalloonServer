package github.kasuminova.balloonserver.utils.filecacheutils;

import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.utils.fileobject.*;
import github.kasuminova.balloonserver.utils.FileUtil;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_THREAD_POOL;
import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_FILE_THREAD_POOL;

/**
 * 计算资源缓存的公用类
 */
public class FileCacheCalculator {
    private final AtomicLong completedBytes = new AtomicLong(0);
    private final AtomicInteger completedFiles = new AtomicInteger(0);
    private final String hashAlgorithm;
    public FileCacheCalculator(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    /**
     * 扫描目标文件夹内的文件与文件夹
     *
     * @param directory 目标文件夹
     * @return ArrayList<AbstractSimpleFileObject>, 如果文件夹内容为空则返回空 ArrayList
     */
    public ArrayList<AbstractSimpleFileObject> scanDir(File directory) {
        File[] fileList = directory.listFiles();
        if (fileList == null) {
            return new ArrayList<>(0);
        }
        ArrayList<FutureTask<SimpleFileObject>> fileCounterTaskList = new ArrayList<>(32);
        ArrayList<FutureTask<SimpleDirectoryObject>> direCounterTaskList = new ArrayList<>(8);
        ArrayList<AbstractSimpleFileObject> abstractSimpleFileObjectList = new ArrayList<>(32);

        for (File file : fileList) {
            if (file.isFile()) {
                FutureTask<SimpleFileObject> fileInfoTask = new FutureTask<>(new FileInfoTask(file, hashAlgorithm, completedBytes, completedFiles));
                fileCounterTaskList.add(fileInfoTask);
                GLOBAL_FILE_THREAD_POOL.execute(fileInfoTask);
            } else {
                FutureTask<SimpleDirectoryObject> dirCounterTask = new FutureTask<>(new DirInfoTask(file, hashAlgorithm, completedBytes, completedFiles));
                direCounterTaskList.add(dirCounterTask);
                GLOBAL_THREAD_POOL.execute(dirCounterTask);
            }
        }

        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : direCounterTaskList) {
            try {
                abstractSimpleFileObjectList.add(simpleDirectoryObjectFutureTask.get());
            } catch (Exception ignored) {}
        }

        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileCounterTaskList) {
            try {
                abstractSimpleFileObjectList.add(simpleFileObjectFutureTask.get());
            } catch (Exception ignored) {}
        }

        return abstractSimpleFileObjectList;
    }

    /**
     * 计算文件夹内容大小
     */
    private static class FileCounter {
        private final AtomicLong totalSize = new AtomicLong(0);
        private final AtomicLong totalFiles = new AtomicLong();

        private long[] getFiles(File dir, SmoothProgressBar statusProgressBar) {
            statusProgressBar.setString("扫描文件夹内容... (0 Byte, 0 文件)");
            Timer timer = new Timer(250, e -> statusProgressBar.setString(
                    String.format("扫描文件夹内容... (%s, %s 文件)",
                            FileUtil.formatFileSizeToStr(totalSize.get()),
                            totalFiles.get())));
            timer.start();

            statusProgressBar.setVisible(true);
            statusProgressBar.setIndeterminate(true);

            new DirSizeCalculatorThread(dir, totalSize, totalFiles).run();
            timer.stop();

            return new long[]{totalSize.get(), totalFiles.get()};
        }
    }

    /**
     * <p>
     * 统计目标文件夹内包含的 文件/文件夹 大小.
     * </p>
     *
     * <p>
     * 并将其大小整合在一起至一个变量, 用于轮询线程的查询.
     * </p>
     *
     * <p>
     * size[0] 为总大小
     * </p>
     *
     * <p>
     * size[1] 为总文件数量
     * </p>
     */
    public static long[] getDirSize(File dir, SmoothProgressBar statusProgressBar) {
        return new FileCounter().getFiles(dir, statusProgressBar);
    }

    public long getCompletedBytes() {
        return completedBytes.get();
    }

    public int getCompletedFiles() {
        return completedFiles.get();
    }
}
