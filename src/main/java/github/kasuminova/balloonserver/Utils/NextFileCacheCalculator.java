package github.kasuminova.balloonserver.Utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

/**
 * 一个多线程计算文件缓存差异的工具类
 * Feature: 使用 AtomicReference 替代 synchronized
 * @author Kasumi_Nova
 */
public class NextFileCacheCalculator {
    private final GUILogger logger;
    public NextFileCacheCalculator(GUILogger logger) {
        this.logger = logger;
    }
    //文件夹计算线程的线程池
    private static final ExecutorService dirThreadPool = Executors.newCachedThreadPool();
    //文件计算线程的线程池
    private static final ThreadPoolExecutor fileThreadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 6,
            100,
            TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>());
    public AtomicInteger progress = new AtomicInteger(0);
    public JSONArray scanDir(JSONArray jsonArray, File dir) {
        AtomicReference<JSONArray> atomicJsonArray = new AtomicReference<>(jsonArray);
        //两个 ArrayList 存储启动的线程，用于读取变量
        ArrayList<FutureTask<SimpleFileObject>> fileThreadList = new ArrayList<>();
        ArrayList<FutureTask<SimpleDirectoryObject>> dirThreadList = new ArrayList<>();

        //遍历文件夹内文件，并与缓存对比是否有新 文件/文件夹
        File[] resDirFiles = dir.listFiles();
        if (resDirFiles != null) {
            for (File resDirFile : resDirFiles) {
                boolean isExist = false;
                for (int i = 0; i < atomicJsonArray.get().size(); i++) {
                    if (atomicJsonArray.get().getJSONObject(i).getString("name").equals(resDirFile.getName())) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    if (resDirFile.isFile()) {
                        FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(resDirFile));
                        fileThreadList.add(fileCounterThread);
                        fileThreadPool.execute(fileCounterThread);
                    } else {
                        FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirCounterThread(resDirFile));
                        dirThreadList.add(dirCounterThread);
                        dirThreadPool.execute(dirCounterThread);
                    }
                }
            }
        }

        //读取上方所创建的线程的返回值
        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : dirThreadList) {
            try {
                SimpleDirectoryObject directoryObject = simpleDirectoryObjectFutureTask.get();
                logger.info("检测到资源目录下有新文件夹：" + dir.getPath() + File.separator + directoryObject.getName() + ", 添加中...");

                atomicJsonArray.get().add(directoryObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileThreadList) {
            try {
                SimpleFileObject fileObject = simpleFileObjectFutureTask.get();
                logger.info("检测到资源目录下有新文件：" + dir.getPath() + File.separator + fileObject.getName() + ", 添加中...");

                atomicJsonArray.get().add(fileObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //重置线程列表
        dirThreadList = new ArrayList<>();
        fileThreadList = new ArrayList<>();

        //遍历当前文件夹，并于缓存对比是否有文件变动
        for (int i = 0; i < atomicJsonArray.get().size(); i++) {
            JSONObject obj = atomicJsonArray.get().getJSONObject(i);

            String childPath = dir.getPath() + File.separator + obj.get("name");

            File childFile = new File(childPath);

            //如果文件或文件夹不存在，直接删除 JSONObject
            if (childFile.exists()) {
                //获取 length，如果为 null，则表示 JSONObject 存储的是文件夹
                if (obj.getLong("length") == null) {
                    //如果资源文件夹现在是文件，则重新生成文件缓存，否则扫描文件夹内内容变化
                    if (childFile.isFile()) {
                        atomicJsonArray.get().remove(obj);

                        FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(childFile));
                        fileThreadList.add(fileCounterThread);
                        fileThreadPool.execute(fileCounterThread);
                    } else {
                        JSONArray children = obj.getJSONArray("children");
                        obj.put("children", scanDir(children, childFile));
                    }
                    //判断修改日期
                } else if (childFile.isFile()) {
                    //如果文件修改时间变动，则重新生成缓存
                    if (childFile.lastModified() / 1000 != obj.getLong("modified")) {
                        atomicJsonArray.get().remove(obj);

                        FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(childFile));
                        fileThreadList.add(fileCounterThread);
                        fileThreadPool.execute(fileCounterThread);
                    } else {
                        progress.getAndIncrement();
                    }
                    //如果资源文件现在是文件夹，则遍历文件夹内部文件并返回 SimpleDirectoryObject
                } else if (childFile.isDirectory()) {
                    atomicJsonArray.get().remove(obj);

                    FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirCounterThread(childFile));
                    dirThreadList.add(dirCounterThread);
                    dirThreadPool.execute(dirCounterThread);
                }
            } else {
                atomicJsonArray.get().remove(obj);
                i--;
                logger.info(childPath + " 不存在, 删除缓存数据.");
            }
        }

        //读取上方的线程的返回值
        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : dirThreadList) {
            try {
                SimpleDirectoryObject directoryObject = simpleDirectoryObjectFutureTask.get();
                logger.info("检测到文件夹变动：" + dir.getPath() + File.separator + directoryObject.getName() + ", 添加中...");

                progress.getAndAdd(directoryObject.getChildren().size());
                atomicJsonArray.get().add(directoryObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileThreadList) {
            try {
                SimpleFileObject fileObject = simpleFileObjectFutureTask.get();
                logger.info("检测到文件变动：" + dir.getPath() + File.separator + fileObject.getName() + ", 添加中...");

                atomicJsonArray.get().add(fileObject);
                progress.getAndIncrement();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



        return atomicJsonArray.get();
    }

    /**
     * 计算一个文件夹的 MD5 类/线程
     * 以 Callable 实现，返回对应的类型
     */
    public static class DirCounterThread implements Callable<SimpleDirectoryObject> {
        ArrayList<FutureTask<SimpleFileObject>> fileThreadList = new ArrayList<>();
        ArrayList<FutureTask<SimpleDirectoryObject>> dirThreadList = new ArrayList<>();
        File dir;
        public DirCounterThread(File dir) {
            this.dir = dir;
        }
        @Override
        public SimpleDirectoryObject call() {
            File[] fileList = dir.listFiles();
            if (fileList != null) {
                ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();
                for (File file : fileList) {
                    if (file.isFile()) {
                        FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(file));
                        fileThreadList.add(fileCounterThread);
                        fileThreadPool.execute(fileCounterThread);
                    } else if (file.isDirectory()) {
                        FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirCounterThread(file));
                        dirThreadPool.execute(dirCounterThread);
                        dirThreadList.add(dirCounterThread);
                    }
                }
                for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileThreadList) {
                    try {
                        fileObjList.add(simpleFileObjectFutureTask.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : dirThreadList) {
                    try {
                        fileObjList.add(simpleDirectoryObjectFutureTask.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return new SimpleDirectoryObject(dir.getName(), fileObjList);
            } else {
                return new SimpleDirectoryObject(dir.getName(), new ArrayList<>());
            }
        }
    }

    /**
     * 计算单个文件的 MD5 的 类/线程.
     * 传入 File 文件
     * 创建一个线程, 计算文件信息
     * 计算完毕后, 返回 SimpleFileObject
     */
    public static class FileCounterThread implements Callable<SimpleFileObject> {
        File file;
        public FileCounterThread(File file) {
            this.file = file;
        }
        @Override
        public SimpleFileObject call() {
            String hash = getSHA1(file);
            return new SimpleFileObject(file.getName(),file.length(),hash,file.lastModified() / 1000);
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