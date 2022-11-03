package github.kasuminova.balloonserver.utils.filecacheutils;

import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.balloonserver.utils.fileobject.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_DIR_THREAD_POOL;
import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_FILE_THREAD_POOL;

/**
 * <p>
 * 以 HashMap 实现的缓存差异算法线程。
 * </p>
 *
 * @param dir 本地文件夹
 * @param fileObjList 要检查的 ArrayList
 * @param hashAlgorithm 如果有差异, 则使用何种算法计算校验码
 * @param logger 日志输出器
 * @param completedFiles 进度
 */
public record JsonCacheCheckerTask(
        File dir, ArrayList<AbstractSimpleFileObject> fileObjList,
        String hashAlgorithm, GUILogger logger, AtomicInteger completedFiles)
        implements Callable<SimpleDirectoryObject> {

    @Override
    public SimpleDirectoryObject call() {
        File[] childFiles = dir.listFiles();

        if (childFiles == null || childFiles.length == 0) {
            return new SimpleDirectoryObject(dir.getName(), new ArrayList<>(0));
        }

        Map<String, AbstractSimpleFileObject> fileObjectMap = fileObjList.stream().collect(
                Collectors.toConcurrentMap(AbstractSimpleFileObject::getName, obj -> obj)
        );

        removeOldFiles(dir.getPath(), fileObjectMap, logger);

        ArrayList<FutureTask<SimpleFileObject>> fileTaskList = new ArrayList<>(0);
        ArrayList<FutureTask<SimpleDirectoryObject>> dirTaskList = new ArrayList<>(0);
        ArrayList<FutureTask<SimpleDirectoryObject>> checkTaskList = new ArrayList<>(0);
        ArrayList<AbstractSimpleFileObject> fileObjectList = new ArrayList<>(childFiles.length);
        ArrayList<AbstractSimpleFileObject> fileObjectListTmp = new ArrayList<>(childFiles.length);

        //遍历目标文件夹内所有文件并检查差异
        for (File childFile : childFiles) {
            String fileName = childFile.getName();

            AbstractSimpleFileObject obj = fileObjectMap.get(fileName);

            if (obj == null) {
                if (childFile.isFile()) {
                    FutureTask<SimpleFileObject> fileInfoTask = new FutureTask<>(new FileInfoTask(childFile, hashAlgorithm, null, completedFiles));
                    fileTaskList.add(fileInfoTask);
                    GLOBAL_FILE_THREAD_POOL.execute(fileInfoTask);

                    logger.info(String.format("%s 是新文件, 更新缓存数据.", childFile.getPath()));
                } else {
                    FutureTask<SimpleDirectoryObject> dirInfoTask = new FutureTask<>(new DirInfoTask(childFile, hashAlgorithm, null, completedFiles));
                    dirTaskList.add(dirInfoTask);
                    GLOBAL_DIR_THREAD_POOL.execute(dirInfoTask);

                    logger.info(String.format("%s 是新文件夹, 更新缓存数据.", childFile.getPath()));
                }
                continue;
            }

            //检查 JSONObject 是否为文件夹对象
            if (obj instanceof SimpleDirectoryObject) {
                //如果本地文件非文件夹，则新建一个 task 计算文件属性
                if (childFile.isFile()) {
                    FutureTask<SimpleFileObject> fileInfoTask = new FutureTask<>(new FileInfoTask(childFile, hashAlgorithm, null, completedFiles));
                    fileTaskList.add(fileInfoTask);
                    GLOBAL_FILE_THREAD_POOL.execute(fileInfoTask);

                    //删除已完成的 AbstractSimpleFileObject 以提高下一次计算的性能
                    fileObjectMap.remove(fileName);

                    continue;
                }

                //如果本地文件依然为文件夹，则递归检查
                FutureTask<SimpleDirectoryObject> checkTask = new FutureTask<>(new JsonCacheCheckerTask(
                        childFile, ((SimpleDirectoryObject) obj).getChildren() , hashAlgorithm, logger, completedFiles));
                checkTaskList.add(checkTask);
                GLOBAL_DIR_THREAD_POOL.execute(checkTask);

                //删除已完成的 AbstractSimpleFileObject 以提高下一次计算的性能
                fileObjectMap.remove(fileName);
            } else {
                //如果本地文件非文件夹，则新建一个 task 计算文件夹属性
                if (childFile.isDirectory()) {
                    FutureTask<SimpleDirectoryObject> dirInfoTask = new FutureTask<>(new DirInfoTask(childFile, hashAlgorithm, null, completedFiles));
                    dirTaskList.add(dirInfoTask);
                    GLOBAL_DIR_THREAD_POOL.execute(dirInfoTask);

                    //删除已完成的 AbstractSimpleFileObject 以提高下一次计算的性能
                    fileObjectMap.remove(fileName);

                    continue;
                }

                //如果本地文件依然为文件，则检查目标是否被修改
                if (fileObjIsNotChanged((SimpleFileObject) obj, childFile)) {
                    fileObjectListTmp.add(obj);
                    completedFiles.getAndIncrement();

                    //删除已完成的 AbstractSimpleFileObject 以提高下一次计算的性能
                    fileObjectMap.remove(fileName);
                }
            }
        }

        checkTaskList.forEach(task -> {
            try {
                fileObjectList.add(task.get());
            } catch (Exception ignored) {
            }
        });

        dirTaskList.forEach(task -> {
            try {
                SimpleDirectoryObject obj = task.get();
                fileObjectList.add(obj);
                logger.info(String.format("文件夹 %s 已完成缓存数据更新.", obj.getName()));
            } catch (Exception ignored) {
            }
        });

        fileTaskList.forEach(task -> {
            try {
                SimpleFileObject obj = task.get();
                fileObjectList.add(task.get());
                logger.info(String.format("文件 %s 已完成缓存数据更新.", obj.getName()));
            } catch (Exception ignored) {
            }
        });

        fileObjectList.addAll(fileObjectListTmp);

        return new SimpleDirectoryObject(dir.getName(), fileObjectList);
    }

    /**
     * 将 fileObjMap 与本地文件比较, 并删除旧文件
     * @param path 路径
     * @param fileObjMap 要比较的 map
     * @param logger 如果文件不存在用此 logger 输出内容
     */
    public static void removeOldFiles(String path, Map<String, AbstractSimpleFileObject> fileObjMap, GUILogger logger) {
        final StringBuilder sb = new StringBuilder(16);

        fileObjMap.forEach((key, value) -> {
            sb.setLength(0);
            String filePath = sb.append(path).append("/").append(key).toString();

            if (!new File(filePath).exists()) {
                logger.info(String.format("%s 不存在, 删除缓存数据.", filePath));
                fileObjMap.remove(key);
            }
        });
    }

    /**
     * 此文件对象是否与物理磁盘上文件相比是否未修改
     *
     * @param obj 要检查的 JSON 对象
     */
    static boolean fileObjIsNotChanged(SimpleFileObject obj, File file) {
        return file.lastModified() / 1000 == obj.getModified();
    }

}
