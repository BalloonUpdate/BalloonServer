package github.kasuminova.balloonserver.Utils;

import github.kasuminova.balloonserver.Utils.FileObject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleFileObject;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

/**
 * 完全计算一次资源文件夹缓存的工具类
 * TODO:计划将 Runnable 类改成 Callable
 */
public class FileListUtils {
    public static AtomicLong completedBytes = new AtomicLong(0);
    public static AtomicInteger completedFiles = new AtomicInteger(0);
    //文件夹计算线程的线程池
    static ExecutorService dirThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    //文件计算线程的线程池
    static ExecutorService fileThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 6);
    /**
     * 统计目标文件夹内包含的 文件/文件夹 大小,
     * 并将其大小整合在一起至一个变量, 用于轮询线程的查询
     * size[0] 为总大小
     * size[1] 为总文件数量
     */
    public static long[] getDirSize(File dir) {
        long[] size = {0,0};
        List<File> files = new fileCounter().getFiles(dir.getPath());
        if (files != null) {
            size[1] = files.size();
            for (File file : files) {
                size[0] += file.length();
            }
        }
        return size;
    }

    /**
     * 计算一个文件夹的 MD5 类/线程
     * 创建一个线程, 并临时存储一个 SimpleDirectoryObject 变量
     * 并给予一个 ArrayList<AbstractSimpleFileObject>, 程序将在最后阶段
     * (即 thread.join 之后)
     * 将 SimpleDirectoryObject 存储进 ArrayList<AbstractSimpleFileObject> 内
     * <p>
     * 如果传入的 File 是文件，则直接启动新的文件计算线程，而不是继续执行下方指令
     * 程序遍历文件夹内文件, 如果为文件则创建一个 计算单个文件的 MD5 的 类/线程, 如果为文件夹则额外一个新的 计算一个文件夹的 MD5 类/线程
     * 并传入 SimpleDirectoryObject 的 children 变量 ArrayList<AbstractSimpleFileObject>
     * <p/>
     * <p>
     * 每个线程创建都需要将线程加入一个 ArrayList<Thread>, 用于 thread.join 方法
     * </p>
     * 主线程执行 thread.join 等待线程执行完毕, 期间此线程可以操作窗口进度
     */
    public static class DirCounterThread implements Runnable {
        ArrayList<Thread> threadList = new ArrayList<>();
        ArrayList<AbstractSimpleFileObject> fileObjList;
        File dir;
        public DirCounterThread(ArrayList<AbstractSimpleFileObject> fileObjList, File dir) {
            this.fileObjList = fileObjList;
            this.dir = dir;
        }
        @Override
        public void run() {
            ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();
            File[] fileList = dir.listFiles();
            if (dir.isFile()) {
                new FileCounterThread(fileObjList, dir).run();
                syncAddFileObjToFileObjList(fileObjList.get(0), this.fileObjList);
            } else if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile()) {
                        Thread fileCounterThread = new Thread(new FileCounterThread(fileObjList, file));
                        threadList.add(fileCounterThread);
                        fileThreadPool.execute(fileCounterThread);
                    } else if (file.isDirectory()) {
                        Thread dirCounterThread = new Thread(new DirCounterThread(fileObjList, file));
                        dirThreadPool.execute(dirCounterThread);
                        threadList.add(dirCounterThread);
                    }
                }
                for (Thread thread : threadList) {
                    try {
                        thread.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                syncAddFileObjToFileObjList(new SimpleDirectoryObject(dir.getName(), fileObjList), this.fileObjList);
            } else {
                syncAddFileObjToFileObjList(new SimpleDirectoryObject(dir.getName(), new ArrayList<>()), this.fileObjList);
            }
        }
    }

    /**
     * 计算单个文件的 MD5 的 类/线程.
     * 传入 ArrayList<AbstractSimpleFileObject> 文件列表, File 文件
     * 创建一个线程, 并临时存储一个 SimpleFileObject 变量,
     * 计算完毕后, 将给予的 ArrayList<AbstractSimpleFileObject> 中添加一个 SimpleFileObject
     */
    public static class FileCounterThread implements Runnable {
        ArrayList<AbstractSimpleFileObject> fileList;
        File file;
        public FileCounterThread(ArrayList<AbstractSimpleFileObject> fileList, File file) {
            this.fileList = fileList;
            this.file = file;
        }
        @Override
        public void run() {
            SimpleFileObject fileObj = new SimpleFileObject(file.getName(),file.length(),getSHA1(file),file.lastModified() / 1000);
            syncAddFileObjToFileObjList(fileObj,fileList);
            completedFiles.getAndIncrement();
        }
    }

    /**
     * 向指定数组添加一个对象,
     * 线程安全.
     * @param obj 对象
     * @param objList 对象列表
     */
    public static synchronized void syncAddFileObjToFileObjList(AbstractSimpleFileObject obj, ArrayList<AbstractSimpleFileObject> objList) {
        objList.add(obj);
    }

    /**
     * 计算文件夹内容大小
     */
    public static class fileCounter {
        List<File> files = new ArrayList<>();
        public List<File> getFiles(String path) {
            getFile(path);
            return files;
        }

        public void getFile(String filepath) {
            File file = new File(filepath);
            File[] listFile = file.listFiles();
            if (listFile != null) {
                for (File value : listFile) {
                    if (!value.isDirectory()) {
                        files.add(value);
                    } else {
    //                    files.add(value);
                        getFile(value.toString());
                    }
                }
            }
        }
    }

    public static String getSHA1(File file) {
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

    public static String getMD5(File file) {
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

    public static String getCRC32(File file) {
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
}
