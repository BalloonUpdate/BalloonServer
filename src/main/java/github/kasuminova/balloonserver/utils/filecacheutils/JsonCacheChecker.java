package github.kasuminova.balloonserver.utils.filecacheutils;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.utils.fileobject.DirInfoTask;
import github.kasuminova.balloonserver.utils.fileobject.FileInfoTask;
import github.kasuminova.balloonserver.utils.fileobject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleFileObject;
import github.kasuminova.balloonserver.utils.GUILogger;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一个多线程计算文件缓存差异的工具类
 *
 * @author Kasumi_Nova
 */
public class JsonCacheChecker {
    public final AtomicInteger completedFiles = new AtomicInteger(0);
    private final ExecutorService fileThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private final GUILogger logger;
    private final String hashAlgorithm;
    /**
     * 可复用变量（降低内存使用率）
     */
    private final StringBuilder childPath = new StringBuilder();
    private final JSONObject obj = new JSONObject();

    public JsonCacheChecker(GUILogger logger, String hashAlgorithm) {
        this.logger = logger;
        this.hashAlgorithm = hashAlgorithm;
    }

    /**
     * 此文件对象是否为文件夹
     *
     * @param obj 要检查的 JSON 对象
     */
    private static boolean fileObjIsDirectoryObj(JSONObject obj) {
        return obj.getLong("length") == null;
    }

    /**
     * 此文件对象是否与物理磁盘上文件相比是否未修改
     *
     * @param obj 要检查的 JSON 对象
     */
    private static boolean fileObjIsNotChanged(JSONObject obj, File file) {
        return file.lastModified() / 1000 == obj.getLong("modified");
    }

    /**
     * 此文件对象是否与文件相同
     *
     * @param obj  文件对象
     * @param file 文件
     */
    private static boolean fileObjIsSameWithFile(JSONObject obj, File file) {
        return obj.getString("name").equals(file.getName());
    }

    /**
     * 以同步方式向指定 JSONArray 插入指定 JSONObject
     *
     * @param jsonArray 目标 JSONArray
     * @param object    要插入的 JSONObject
     */
    private synchronized void removeObjectFromJsonArraySync(JSONArray jsonArray, JSONObject object) {
        jsonArray.remove(object);
    }

    /**
     * 以同步方式向指定 JSONArray 移除指定 JSONObject
     *
     * @param jsonArray 目标 JSONArray
     * @param object    要移除的 JSONObject
     */
    private synchronized void addObjectObjectToJsonArraySync(JSONArray jsonArray, AbstractSimpleFileObject object) {
        jsonArray.add(object);
    }

    private void checkNewFiles(File dir, JSONArray jsonArray, ArrayList<FutureTask<SimpleFileObject>> fileTaskList, ArrayList<FutureTask<SimpleDirectoryObject>> dirTaskList) {
        //遍历文件夹内文件, 并与缓存对比是否有新 文件/文件夹
        File[] resDirFiles = dir.listFiles();
        if (resDirFiles != null) {
            for (File resDirFile : resDirFiles) {
                boolean isExist = false;
                for (int i = 0; i < jsonArray.size(); i++) {
                    if (fileObjIsSameWithFile(jsonArray.getJSONObject(i), resDirFile)) {
                        isExist = true;
                        break;
                    }
                }

                //如果文件存在则跳过本次循环, 否则执行下方内容
                if (isExist) continue;

                if (resDirFile.isFile()) {
                    FutureTask<SimpleFileObject> fileInfoTask = new FutureTask<>(new FileInfoTask(resDirFile, hashAlgorithm, null, null));
                    fileTaskList.add(fileInfoTask);
                    ThreadUtil.execute(fileInfoTask);
                } else {
                    FutureTask<SimpleDirectoryObject> dirInfoTask = new FutureTask<>(new DirInfoTask(resDirFile, fileThreadPool, hashAlgorithm, null, null));
                    dirTaskList.add(dirInfoTask);
                    ThreadUtil.execute(dirInfoTask);
                }
            }
        }

        //可复用变量名
        SimpleDirectoryObject directoryObject;
        SimpleFileObject fileObject;

        //读取上方所创建的线程的返回值
        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectTask : dirTaskList) {
            try {
                directoryObject = simpleDirectoryObjectTask.get();
                logger.info("检测到资源目录下有新文件夹: " + dir.getPath() + File.separator + directoryObject.getName() + ", 添加中...");

                addObjectObjectToJsonArraySync(jsonArray, directoryObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (FutureTask<SimpleFileObject> simpleFileObjectTask : fileTaskList) {
            try {
                fileObject = simpleFileObjectTask.get();
                logger.info("检测到资源目录下有新文件: " + dir.getPath() + File.separator + fileObject.getName() + ", 添加中...");

                addObjectObjectToJsonArraySync(jsonArray, fileObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public JSONArray checkDirJsonCache(JSONArray jsonArray, File dir) {
        //两个 ArrayList 存储启动的线程, 用于读取线程返回的变量
        ArrayList<FutureTask<SimpleFileObject>> fileTaskList = new ArrayList<>();
        ArrayList<FutureTask<SimpleDirectoryObject>> dirTaskList = new ArrayList<>();

        checkNewFiles(dir, jsonArray, fileTaskList, dirTaskList);

        //重置线程列表
        dirTaskList.clear();
        fileTaskList.clear();

        //遍历当前文件夹, 并于缓存对比是否有文件变动
        for (int i = 0; i < jsonArray.size(); i++) {
            obj.clear();
            obj.putAll(jsonArray.getJSONObject(i));

            childPath.setLength(0);
            childPath.append(dir.getPath()).append(File.separator).append(obj.get("name"));

            File childFile = new File(childPath.toString());

            //如果文件或文件夹不存在, 直接删除 JSONObject, 否则继续下方操作
            if (!childFile.exists()) {
                removeObjectFromJsonArraySync(jsonArray, obj);
                i--;
                logger.info(childPath + " 不存在, 删除缓存数据.");
                continue;
            }
            
            //此 Json 对象是否为文件夹对象
            if (fileObjIsDirectoryObj(obj)) {
                //如果资源文件夹现在是文件, 则重新生成文件缓存, 否则扫描文件夹内内容变化
                if (childFile.isFile()) {
                    removeObjectFromJsonArraySync(jsonArray, obj);

                    FutureTask<SimpleFileObject> fileCounterTask = new FutureTask<>(new FileInfoTask(childFile, hashAlgorithm, null, null));
                    fileTaskList.add(fileCounterTask);
                    fileThreadPool.submit(fileCounterTask);
                } else {
                    JSONArray children = obj.getJSONArray("children");
                    obj.put("children", checkDirJsonCache(children, childFile));
                }
                //判断修改日期
            } else if (childFile.isFile()) {
                //此对象是否已被修改
                if (fileObjIsNotChanged(obj, childFile)) {
                    completedFiles.getAndIncrement();
                } else {
                    removeObjectFromJsonArraySync(jsonArray, obj);

                    FutureTask<SimpleFileObject> fileCounterTask = new FutureTask<>(new FileInfoTask(childFile, hashAlgorithm, null, null));
                    fileTaskList.add(fileCounterTask);
                    fileThreadPool.submit(fileCounterTask);
                }
                //如果资源文件现在是文件夹, 则遍历文件夹内部文件并返回 SimpleDirectoryObject
            } else if (childFile.isDirectory()) {
                removeObjectFromJsonArraySync(jsonArray, obj);

                FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirInfoTask(childFile, fileThreadPool, hashAlgorithm, null, null));
                dirTaskList.add(dirCounterThread);
                ThreadUtil.execute(dirCounterThread);
            }
        }

        //可复用变量名
        SimpleDirectoryObject directoryObject;
        SimpleFileObject fileObject;

        //读取上方的线程的返回值
        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : dirTaskList) {
            try {
                directoryObject = simpleDirectoryObjectFutureTask.get();
                logger.info("检测到文件夹变动: " + dir.getPath() + File.separator + directoryObject.getName() + ", 添加中...");

                completedFiles.getAndAdd(directoryObject.getChildren().size());
                addObjectObjectToJsonArraySync(jsonArray, directoryObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileTaskList) {
            try {
                fileObject = simpleFileObjectFutureTask.get();
                logger.info("检测到文件变动: " + dir.getPath() + File.separator + fileObject.getName() + ", 添加中...");

                addObjectObjectToJsonArraySync(jsonArray, fileObject);
                completedFiles.getAndIncrement();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return jsonArray;
    }

}