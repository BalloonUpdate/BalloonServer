package github.kasuminova.balloonserver.utils;

import cn.hutool.core.util.StrUtil;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    private final String threadName;
    private final ThreadGroup group = Thread.currentThread().getThreadGroup();
    private final AtomicInteger threadCount = new AtomicInteger(1);

    public CustomThreadFactory(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(group, r, StrUtil.format(threadName, threadCount.getAndIncrement()));
    }

    /**
     * 新建一个自定义线程工厂, 此工厂的线程名可自定义
     * @param threadName 线程名, 如 "Thread-{}"; "CustomThread-{}", {} 为线程编号.
     */
    public static CustomThreadFactory create(String threadName) {
        return new CustomThreadFactory(threadName);
    }
}
