package github.kasuminova.balloonserver.Utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.Utils.FileObject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleFileObject;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_THREAD_POOL;
import static github.kasuminova.balloonserver.Utils.HashCalculator.getCRC32;
import static github.kasuminova.balloonserver.Utils.HashCalculator.getSHA1;

/**
 * 一个多线程计算文件缓存差异的工具类
 * @author Kasumi_Nova
 */
public class FileCacheCalculator {
    public FileCacheCalculator(GUILogger logger, String hashAlgorithm) {
        this.logger = logger;
        this.hashAlgorithm = hashAlgorithm;
    }
    private final ExecutorService FILE_THREAD_POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private final GUILogger logger;
    private final String hashAlgorithm;
    public final AtomicInteger completedFiles = new AtomicInteger(0);

    /**
     * 可复用变量（降低内存使用率）
     */
    private final StringBuilder childPath = new StringBuilder();
    private final JSONObject obj = new JSONObject();

    public JSONArray scanDir(JSONArray jsonArray, File dir) {
        //两个 ArrayList 存储启动的线程，用于读取变量
        ArrayList<FutureTask<SimpleFileObject>> fileThreadList = new ArrayList<>();
        ArrayList<FutureTask<SimpleDirectoryObject>> dirThreadList = new ArrayList<>();

        //遍历文件夹内文件，并与缓存对比是否有新 文件/文件夹
        File[] resDirFiles = dir.listFiles();
        if (resDirFiles != null) {
            for (File resDirFile : resDirFiles) {
                boolean isExist = false;
                for (int i = 0; i < jsonArray.size(); i++) {
                    if (jsonArray.getJSONObject(i).getString("name").equals(resDirFile.getName())) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    if (resDirFile.isFile()) {
                        FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(resDirFile));
                        fileThreadList.add(fileCounterThread);
                        FILE_THREAD_POOL.execute(fileCounterThread);
                    } else {
                        FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirCounterThread(resDirFile));
                        dirThreadList.add(dirCounterThread);
                        GLOBAL_THREAD_POOL.execute(dirCounterThread);
                    }
                }
            }
        }

        //变量复用
        SimpleDirectoryObject directoryObject;
        SimpleFileObject fileObject;

        //读取上方所创建的线程的返回值
        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : dirThreadList) {
            try {
                directoryObject = simpleDirectoryObjectFutureTask.get();
                logger.info("检测到资源目录下有新文件夹：" + dir.getPath() + File.separator + directoryObject.getName() + ", 添加中...");

                addObjectObjectToJsonArray(jsonArray,directoryObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileThreadList) {
            try {
                fileObject = simpleFileObjectFutureTask.get();
                logger.info("检测到资源目录下有新文件：" + dir.getPath() + File.separator + fileObject.getName() + ", 添加中...");

                addObjectObjectToJsonArray(jsonArray,fileObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //重置线程列表
        dirThreadList.clear();
        fileThreadList.clear();

        //遍历当前文件夹，并于缓存对比是否有文件变动
        for (int i = 0; i < jsonArray.size(); i++) {
            obj.clear();
            obj.putAll(jsonArray.getJSONObject(i));

            childPath.setLength(0);
            childPath.append(dir.getPath()).append(File.separator).append(obj.get("name"));

            File childFile = new File(childPath.toString());

            //如果文件或文件夹不存在，直接删除 JSONObject
            if (childFile.exists()) {
                //获取 length，如果为 null，则表示 JSONObject 存储的是文件夹
                if (obj.getLong("length") == null) {
                    //如果资源文件夹现在是文件，则重新生成文件缓存，否则扫描文件夹内内容变化
                    if (childFile.isFile()) {
                        removeObjectFromJsonArray(jsonArray,obj);

                        FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(childFile));
                        fileThreadList.add(fileCounterThread);
                        FILE_THREAD_POOL.execute(fileCounterThread);
                    } else {
                        JSONArray children = obj.getJSONArray("children");
                        obj.put("children", scanDir(children, childFile));
                    }
                    //判断修改日期
                } else if (childFile.isFile()) {
                    //如果文件修改时间变动，则重新生成缓存
                    if (childFile.lastModified() / 1000 != obj.getLong("modified")) {
                        removeObjectFromJsonArray(jsonArray,obj);

                        FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(childFile));
                        fileThreadList.add(fileCounterThread);
                        FILE_THREAD_POOL.execute(fileCounterThread);
                    } else {
                        completedFiles.getAndIncrement();
                    }
                    //如果资源文件现在是文件夹，则遍历文件夹内部文件并返回 SimpleDirectoryObject
                } else if (childFile.isDirectory()) {
                    removeObjectFromJsonArray(jsonArray,obj);

                    FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirCounterThread(childFile));
                    dirThreadList.add(dirCounterThread);
                    GLOBAL_THREAD_POOL.execute(dirCounterThread);
                }
            } else {
                removeObjectFromJsonArray(jsonArray,obj);
                i--;
                logger.info(childPath + " 不存在, 删除缓存数据.");
            }
        }

        //读取上方的线程的返回值
        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : dirThreadList) {
            try {
                directoryObject = simpleDirectoryObjectFutureTask.get();
                logger.info("检测到文件夹变动：" + dir.getPath() + File.separator + directoryObject.getName() + ", 添加中...");

                completedFiles.getAndAdd(directoryObject.getChildren().size());
                addObjectObjectToJsonArray(jsonArray,directoryObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileThreadList) {
            try {
                fileObject = simpleFileObjectFutureTask.get();
                logger.info("检测到文件变动：" + dir.getPath() + File.separator + fileObject.getName() + ", 添加中...");

                addObjectObjectToJsonArray(jsonArray,fileObject);
                completedFiles.getAndIncrement();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return jsonArray;
    }

    private synchronized static void removeObjectFromJsonArray(JSONArray jsonArray, JSONObject object) {
        jsonArray.remove(object);
    }

    private synchronized static void addObjectObjectToJsonArray(JSONArray jsonArray, AbstractSimpleFileObject object) {
        jsonArray.add(object);
    }

    /**
     * 计算一个文件夹的 MD5 类/线程
     * 以 Callable 实现，返回对应的类型
     */
    public class DirCounterThread implements Callable<SimpleDirectoryObject> {
        private final ArrayList<FutureTask<SimpleFileObject>> fileThreadList = new ArrayList<>();
        private final ArrayList<FutureTask<SimpleDirectoryObject>> dirThreadList = new ArrayList<>();
        private final File dir;
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
                        FILE_THREAD_POOL.execute(fileCounterThread);
                    } else if (file.isDirectory()) {
                        FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirCounterThread(file));
                        GLOBAL_THREAD_POOL.execute(dirCounterThread);
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
    public class FileCounterThread implements Callable<SimpleFileObject> {
        private final File file;
        public FileCounterThread(File file) {
            this.file = file;
        }
        @Override
        public SimpleFileObject call() throws IOException, NoSuchAlgorithmException {
            String hash;
            if (hashAlgorithm.equals(HashCalculator.SHA1)) {
                hash = getSHA1(file);
            } else {
                hash = getCRC32(file);
            }
            return new SimpleFileObject(file.getName(),file.length(),hash,file.lastModified() / 1000);
        }
    }
}