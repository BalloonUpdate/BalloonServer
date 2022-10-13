package github.kasuminova.balloonserver.Utils.FileCacheUtils;

import cn.hutool.core.thread.ThreadUtil;
import github.kasuminova.balloonserver.Utils.FileObject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.Utils.FileObject.SimpleFileObject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

/**
 * 计算一个文件夹的 SHA1/CRC32 类/线程
 * 以 Callable 实现，返回对应的类型
 */
record DirCounterThread(
        File dir,
        ExecutorService FILE_THREAD_POOL,
        String hashAlgorithm) implements Callable<SimpleDirectoryObject> {
    @Override
    public SimpleDirectoryObject call() {
        ArrayList<FutureTask<SimpleFileObject>> fileThreadList = new ArrayList<>();
        ArrayList<FutureTask<SimpleDirectoryObject>> dirThreadList = new ArrayList<>();

        File[] fileList = dir.listFiles();
        if (fileList != null) {
            ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();
            for (File file : fileList) {
                if (file.isFile()) {
                    FutureTask<SimpleFileObject> fileCounterThread = new FutureTask<>(new FileCounterThread(file, hashAlgorithm));
                    fileThreadList.add(fileCounterThread);
                    FILE_THREAD_POOL.submit(fileCounterThread);
                } else if (file.isDirectory()) {
                    FutureTask<SimpleDirectoryObject> dirCounterThread = new FutureTask<>(new DirCounterThread(file, FILE_THREAD_POOL, hashAlgorithm));
                    ThreadUtil.execAsync(dirCounterThread);
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
