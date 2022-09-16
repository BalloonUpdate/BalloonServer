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

import github.kasuminova.balloonserver.Utils.FileObject.*;


public class NextFileListUtils {
    public AtomicLong completedBytes = new AtomicLong(0);
    public AtomicInteger completedFiles = new AtomicInteger(0);
    //全局线程池
    ExecutorService dirThreadPool = Executors.newCachedThreadPool();
    ThreadPoolExecutor fileThreadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
        Runtime.getRuntime().availableProcessors() * 6,
            100,
            TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>());

    /**
     * 扫描目标文件夹内的文件与文件夹，如果文件夹内容为空则返回 null
     * @param directory 目标文件夹
     * @param logger 日志输出器
     * @return ArrayList<AbstractSimpleFileObject> 或 null
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
            } catch (Exception ignored) {
            }
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
        File directory;
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
                } catch (Exception ignored) {
                }
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
        File file;
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

    public String getMD5(File file) {
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

    public String getSHA1(File file) {
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

    public String getCRC32(File file) {
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
     * 统计目标文件夹内包含的 文件/文件夹 大小,
     * 并将其大小整合在一起至一个变量, 用于轮询线程的查询
     * size[0] 为总大小
     * size[1] 为总文件数量
     */
    public static long[] getDirSize(File dir) {
        return new FileCounter().getFiles(dir);
    }

    /**
     * 计算文件夹内容大小
     */
    public static class FileCounter {
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong totalFiles = new AtomicLong();
        public long[] getFiles(File dir) {
            Thread thread = new Thread(new DirCalculatorThread(dir));
            thread.start();
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
                return new long[]{0, 0};
            }
            return new long[]{totalSize.get(),totalFiles.get()};
        }

        private class DirCalculatorThread implements Runnable {
            File dir;
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
