package github.kasuminova.balloonserver.utils.fileobject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_DIR_THREAD_POOL;
import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_FILE_THREAD_POOL;

/**
 * 获取文件夹内所有文件信息的线程
 * @param directory 目标文件夹
 * @param hashAlgorithm Hash 算法类型
 * @param completedBytes 进度（可空）
 * @param completedFiles 已完成的文件数量（可空）
 */
public record DirInfoTask(File directory, String hashAlgorithm, AtomicLong completedBytes, AtomicInteger completedFiles)
        implements Callable<SimpleDirectoryObject> {

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
                FutureTask<SimpleFileObject> fileCounterTask = new FutureTask<>(
                        new FileInfoTask(file, hashAlgorithm, completedBytes, completedFiles));
                fileCounterTaskList.add(fileCounterTask);
                GLOBAL_FILE_THREAD_POOL.execute(fileCounterTask);
            } else {
                FutureTask<SimpleDirectoryObject> dirCounterTask = new FutureTask<>(
                        new DirInfoTask(file, hashAlgorithm, completedBytes, completedFiles));
                direCounterTaskList.add(dirCounterTask);
                GLOBAL_DIR_THREAD_POOL.execute(dirCounterTask);
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

        return new SimpleDirectoryObject(directory.getName(), abstractSimpleFileObjectList);
    }
}
