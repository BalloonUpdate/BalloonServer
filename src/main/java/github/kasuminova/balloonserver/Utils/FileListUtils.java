package github.kasuminova.balloonserver.Utils;

import github.balloonupdate.littleserver.AbstractSimpleFileObject;
import github.balloonupdate.littleserver.SimpleDirectoryObject;
import github.balloonupdate.littleserver.SimpleFileObject;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.zip.CRC32;

public class FileListUtils {
    static final int maxThreads = Runtime.getRuntime().availableProcessors() * 4;
    static AtomicInteger runningThreads = new AtomicInteger(0);
    public static AtomicLong completedBytes = new AtomicLong(0);
    public static AtomicInteger completedFiles = new AtomicInteger(0);

    /**
     * 统计目标文件夹内包含的 文件/文件夹 大小,
     * 并将其大小整合在一起至一个变量, 用于轮询线程的查询
     * size[0] 为总大小
     * size[1] 为总文件数量
     */
    public static long[] getDirSize(File dir) {
        long[] size = new long[2];
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
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile()) {
                        while (true) {
                            if (runningThreads.get() <= maxThreads) {
                                Thread fileCounterThread = new Thread(new FileCounterThread(fileObjList, file));
                                threadList.add(fileCounterThread);
                                fileCounterThread.start();
                                break;
                            }
                            try {
                                Thread.sleep(25);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (file.isDirectory()) {
                        while (true) {
                            if (runningThreads.get() <= maxThreads) {
                                Thread dirCounterThread = new Thread(new DirCounterThread(fileObjList, file));
                                threadList.add(dirCounterThread);
                                dirCounterThread.start();
                                break;
                            }
                            try {
                                Thread.sleep(25);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                addFileObjToFileObjList(new SimpleDirectoryObject(dir.getName(), new ArrayList<>()), this.fileObjList);
                runningThreads.getAndDecrement();
                return;
            }
            runningThreads.getAndDecrement();
            for (Thread thread : threadList) {
                try {
                    thread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            addFileObjToFileObjList(new SimpleDirectoryObject(dir.getName(), fileObjList), this.fileObjList);
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
            runningThreads.getAndIncrement();
            SimpleFileObject fileObj = new SimpleFileObject(file.getName(),file.length(),getSHA1(file),file.lastModified() / 1000);
            addFileObjToFileObjList(fileObj,fileList);
            runningThreads.getAndDecrement();
            completedFiles.getAndIncrement();
        }
    }

    /**
     * 向指定数组添加一个对象,
     * 线程安全.
     * @param obj 对象
     * @param objList 对象列表
     */
    public static synchronized void addFileObjToFileObjList(AbstractSimpleFileObject obj, ArrayList<AbstractSimpleFileObject> objList) {
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
        //计算 MD5
        try {
            FileInputStream in = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] buffer = FileUtil.formatFileSizeByte(file.length());

            int len;
            while ((len = in.read(buffer)) != -1) {
                md.update(buffer, 0, len);
                completedBytes.getAndAdd(len);
            }

            in.close();

            //转换并返回包含 20 个元素字节数组,返回数值范围为 -128 到 127
            byte[] sha1Bytes = md.digest();
            //1 代表绝对值
            BigInteger bigInt = new BigInteger(1, sha1Bytes);
            //转换为 16 进制
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public static String getMD5(File file) {
        //计算 MD5
        try {
            FileInputStream in = new FileInputStream(file);
            //拿到一个 MD5 转换器，此外还有 SHA-1, SHA-256
            MessageDigest md = MessageDigest.getInstance("MD5");
            //分多次将一个文件读入，对于大型文件而言，比较推荐这种方式，占用内存比较少。
            byte[] buffer = FileUtil.formatFileSizeByte(file.length());

            int len;
            while ((len = in.read(buffer)) != -1) {
                md.update(buffer, 0, len);
                completedBytes.getAndAdd(len);
            }

            in.close();

            //转换并返回包含 16 个元素字节数组,返回数值范围为 -128 到 127
            byte[] md5Bytes = md.digest();
            //1 代表绝对值
            BigInteger bigInt = new BigInteger(1, md5Bytes);
            //转换为 16 进制
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public static String getCRC32(File file) {
        try {
            CRC32 crc32 = new CRC32();
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = FileUtil.formatFileSizeByte(file.length());

            int len;
            while ((len = in.read(buffer)) != -1) {
                crc32.update(buffer, 0, len);
                completedBytes.getAndAdd(len);
            }

            in.close();

            return String.valueOf(crc32.getValue());
        } catch (Exception e){
            e.printStackTrace();
            return "ERROR";
        }
    }

    public static String replaceString(String str) {
        return str.replaceFirst(Matcher.quoteReplacement("." + File.separator), "");
    }
}
