package github.kasuminova.balloonserver.Utils;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.Utils.FileObject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleFileObject;

import javax.swing.*;

/**
 * 计算资源缓存的公用类
 */
public class NextFileListUtils {
    private final AtomicLong completedBytes = new AtomicLong(0);
    private final AtomicInteger completedFiles = new AtomicInteger(0);
    private final ExecutorService dirThreadPool = Executors.newCachedThreadPool();
    private final ExecutorService fileThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public long getCompletedBytes() {
        return completedBytes.get();
    }
    public int getCompletedFiles() {
        return completedFiles.get();
    }

    /**
     * 统计目标文件夹内包含的 文件/文件夹 大小,
     * 并将其大小整合在一起至一个变量, 用于轮询线程的查询
     * size[0] 为总大小
     * size[1] 为总文件数量
     */
    public static long[] getDirSize(File dir) {
        return new FileCounter().getFiles(dir);
    }

    /**
     * 扫描目标文件夹内的文件与文件夹
     * @param directory 目标文件夹
     * @param logger 日志输出器
     * @return ArrayList<AbstractSimpleFileObject>, 如果文件夹内容为空则返回空 ArrayList
     */
    public ArrayList<AbstractSimpleFileObject> scanDir(File directory, GUILogger logger) {
        File[] fileList = directory.listFiles();
        if (fileList == null) {
            return new ArrayList<>();
        }
        ArrayList<FutureTask<SimpleFileObject>> fileCounterTaskList = new ArrayList<>();
        ArrayList<FutureTask<SimpleDirectoryObject>> direCounterTaskList = new ArrayList<>();
        ArrayList<AbstractSimpleFileObject> abstractSimpleFileObjectList = new ArrayList<>();

        for (File file : fileList) {
            if (file.isFile()) {
                FutureTask<SimpleFileObject> fileCounterTask = new FutureTask<>(new FileCounterTask(file));
                fileCounterTaskList.add(fileCounterTask);
                fileThreadPool.execute(fileCounterTask);
            } else {
                FutureTask<SimpleDirectoryObject> dirCounterTask = new FutureTask<>(new DirCounterTask(file));
                direCounterTaskList.add(dirCounterTask);
                dirThreadPool.execute(dirCounterTask);
            }
        }

        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileCounterTaskList) {
            try {
                abstractSimpleFileObjectList.add(simpleFileObjectFutureTask.get());
            } catch (Exception ignored) {}
        }

        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : direCounterTaskList) {
            try {
                abstractSimpleFileObjectList.add(simpleDirectoryObjectFutureTask.get());
            } catch (Exception ignored) {}
        }

        //回收线程池
        new Thread(() -> {
            dirThreadPool.shutdown();
            fileThreadPool.shutdown();
            logger.info("已回收所有闲置线程.");
        }).start();

        return abstractSimpleFileObjectList;
    }

    private class DirCounterTask implements Callable<SimpleDirectoryObject> {
        private final File directory;
        public DirCounterTask(File directory) {
            this.directory = directory;
        }

        @Override
        public SimpleDirectoryObject call() {
            File[] fileList = directory.listFiles();
            if (fileList == null) {
                return new SimpleDirectoryObject(directory.getName(), new ArrayList<>());
            }
            ArrayList<FutureTask<SimpleFileObject>> fileCounterTaskList = new ArrayList<>();
            ArrayList<FutureTask<SimpleDirectoryObject>> direCounterTaskList = new ArrayList<>();
            ArrayList<AbstractSimpleFileObject> abstractSimpleFileObjectList = new ArrayList<>();

            for (File file : fileList) {
                if (file.isFile()) {
                    FutureTask<SimpleFileObject> fileCounterTask = new FutureTask<>(new FileCounterTask(file));
                    fileCounterTaskList.add(fileCounterTask);
                    fileThreadPool.execute(fileCounterTask);
                } else {
                    FutureTask<SimpleDirectoryObject> dirCounterTask = new FutureTask<>(new DirCounterTask(file));
                    direCounterTaskList.add(dirCounterTask);
                    dirThreadPool.execute(dirCounterTask);
                }
            }

            for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileCounterTaskList) {
                try {
                    abstractSimpleFileObjectList.add(simpleFileObjectFutureTask.get());
                } catch (Exception ignored) {}
            }

            for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : direCounterTaskList) {
                try {
                    abstractSimpleFileObjectList.add(simpleDirectoryObjectFutureTask.get());
                } catch (Exception ignored) {}
            }

            return new SimpleDirectoryObject(directory.getName(), abstractSimpleFileObjectList);
        }
    }

    private class FileCounterTask implements Callable<SimpleFileObject> {
        private final File file;
        public FileCounterTask(File file) {
            this.file = file;
        }
        @Override
        public SimpleFileObject call() {
            String sha1 = getSHA1(file);
            completedFiles.getAndIncrement();
            return new SimpleFileObject(
                    file.getName(),
                    file.length(),
                    sha1,
                    file.lastModified() / 1000);
        }
    }

    private String getMD5(File file) {
        try {
            FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
            ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
            int len;
            MessageDigest md = MessageDigest.getInstance("MD5");
            while ((len = fc.read(byteBuffer)) > 0) {
                md.update(byteBuffer.array(), 0, len);
                completedBytes.getAndAdd(len);
                byteBuffer.flip();
                byteBuffer.clear();
            }
            fc.close();
            //转换并返回包含 16 个元素字节数组,返回数值范围为 -128 到 127
            byte[] md5Bytes = md.digest();
            //1 代表绝对值
            BigInteger bigInt = new BigInteger(1, md5Bytes);
            //转换为 16 进制
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    private String getSHA1(File file) {
        try {
            FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
            ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
            int len;
            MessageDigest md = MessageDigest.getInstance("SHA1");
            while ((len = fc.read(byteBuffer)) > 0) {
                md.update(byteBuffer.array(), 0, len);
                completedBytes.getAndAdd(len);
                byteBuffer.flip();
                byteBuffer.clear();
            }
            fc.close();
            //转换并返回包含 16 个元素字节数组,返回数值范围为 -128 到 127
            byte[] md5Bytes = md.digest();
            //1 代表绝对值
            BigInteger bigInt = new BigInteger(1, md5Bytes);
            //转换为 16 进制
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    private String getCRC32(File file) {
        try {
            FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
            ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
            int len;
            CRC32 crc32 = new CRC32();
            while ((len = fc.read(byteBuffer)) > 0) {
                crc32.update(byteBuffer.array(), 0, len);
                completedBytes.getAndAdd(len);
                byteBuffer.flip();
                byteBuffer.clear();
            }
            fc.close();
            return String.valueOf(crc32.getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    /**
     * 计算文件夹内容大小
     */
    private static class FileCounter {
        private final AtomicLong totalSize = new AtomicLong(0);
        private final AtomicLong totalFiles = new AtomicLong();
        private long[] getFiles(File dir) {
            Thread thread = new Thread(new DirCalculatorThread(dir));
            thread.start();
            JProgressBar statusProgressBar = BalloonServer.addNewStatusProgressBar();

            statusProgressBar.setString("扫描文件夹内容... (0 Byte, 0 文件)");
            Timer timer = new Timer(250, e -> {
                statusProgressBar.setString(
                        String.format("扫描文件夹内容... (%s, %s文件)",
                                FileUtil.formatFileSizeToStr(totalSize.get()),
                                totalFiles.get()));
            });

            statusProgressBar.setVisible(true);
            statusProgressBar.setIndeterminate(true);
            timer.start();

            try {
                thread.join();
                timer.stop();
            } catch (Exception e) {
                e.printStackTrace();
                return new long[]{0, 0};
            }
            return new long[]{totalSize.get(),totalFiles.get()};
        }

        private class DirCalculatorThread implements Runnable {
            private final File dir;
            public DirCalculatorThread(File dir) {
                this.dir = dir;
            }
            @Override
            public void run() {
                File[] fileList = dir.listFiles();
                ArrayList<Thread> threadList = new ArrayList<>();
                if (fileList != null) {
                    for (File value : fileList) {
                        if (!value.isDirectory()) {
                            //计算大小
                            totalSize.getAndAdd(value.length());
                            //计算文件
                            totalFiles.getAndIncrement();
                        } else {
                            Thread thread = new Thread(new DirCalculatorThread(value));
                            threadList.add(thread);
                            thread.start();
                        }
                    }
                }
                for (Thread thread : threadList) {
                    try {
                        thread.join();
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
