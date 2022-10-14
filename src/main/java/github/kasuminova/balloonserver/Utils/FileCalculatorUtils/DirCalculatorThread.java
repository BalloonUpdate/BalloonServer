package github.kasuminova.balloonserver.Utils.FileCalculatorUtils;

import cn.hutool.core.thread.ThreadUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

record DirCalculatorThread(File dir, AtomicLong totalSize, AtomicLong totalFiles)
        implements Runnable {

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
                    Thread thread = new Thread(new DirCalculatorThread(value, totalSize, totalFiles));
                    threadList.add(thread);
                    thread.start();
                }
            }
        }
        for (Thread thread : threadList) {
            ThreadUtil.waitForDie(thread);
        }
    }
}
