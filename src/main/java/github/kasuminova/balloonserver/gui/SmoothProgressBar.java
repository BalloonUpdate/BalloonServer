package github.kasuminova.balloonserver.gui;

import cn.hutool.core.thread.ThreadUtil;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 平滑进度条
 */
public class SmoothProgressBar extends JProgressBar {
    private final ExecutorService singleThreadExecutor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(2));
    private final int flowTime;
    private final int frequency;

    /**
     * 创建一个平滑进度条，基于 JProgressBar
     *
     * @param max      最大值
     * @param flowTime 每次变动进度时消耗的时间，时间越长进度条越平滑，并除以 10 作为刷新时间
     */
    public SmoothProgressBar(int max, int flowTime) {
        super(0, max);
        this.flowTime = flowTime;
        frequency = flowTime / 10;
    }

    /**
     * <p>
     * 以平滑方式设置进度
     * </p>
     * <p>
     * 注意：此方法会阻塞当前线程以设置进度，如果需要非阻塞，请自行创建一个线程或使用 setValueAsync(int value) 方法
     * </p>
     *
     * @param value 新进度
     * @see #setValueAsync(int value)
     */
    @Override
    public synchronized void setValue(int value) {
        int currentValue = getValue();
        if (value < currentValue) {
            decrement(currentValue - value);
        } else {
            increment(value - currentValue);
        }
    }

    /**
     * <p>
     * 以平滑方式设置进度
     * </p>
     * <p>
     * 此方法不会阻塞线程。
     * </p>
     *
     * @param value 新进度
     */
    public void setValueAsync(int value) {
        singleThreadExecutor.execute(() -> {
            int currentValue = getValue();
            if (value < currentValue) {
                decrement(currentValue - value);
            } else {
                increment(value - currentValue);
            }
        });
    }

    /**
     * 重置进度条进度至 0, 不使用平滑进度
     */
    public void reset() {
        super.setValue(0);
    }

    /**
     * 将进度条进度增长指定数值, 使用平滑方式
     *
     * @param value 增长的数值
     */
    public void increment(int value) {
        int currentValue = getValue();
        //如果变动的数值小于刷新速度，则使用变动数值作为刷新速度，否则使用默认刷新速度
        int finalFrequency = Math.min(frequency, value);
        for (int i = 1; i <= finalFrequency; i++) {
            if (((ThreadPoolExecutor) singleThreadExecutor).getQueue().size() > 0) break;
            super.setValue(currentValue + ((value / finalFrequency) * i));
            ThreadUtil.sleep(flowTime / (finalFrequency + i));
        }

        super.setValue(currentValue + value);
    }

    /**
     * 将进度条进度减少指定数值, 使用平滑方式
     *
     * @param value 减少的数值
     */
    public void decrement(int value) {
        int currentValue = getValue();
        //如果变动的数值小于刷新速度，则使用变动数值作为刷新速度，否则使用默认刷新速度
        int finalFrequency = Math.min(frequency, value);
        for (int i = 0; i < finalFrequency; i++) {
            if (((ThreadPoolExecutor) singleThreadExecutor).getQueue().size() > 0) break;
            super.setValue(currentValue - ((value / finalFrequency) * i));
            ThreadUtil.sleep(flowTime / (finalFrequency + i));
        }

        super.setValue(currentValue - value);
    }

    /**
     * <p>
     * 将进度条进度增长指定数值, 使用平滑方式
     * </p>
     *
     * <p>
     * 非同步模式
     * </p>
     *
     * @param value 增长的数值
     */
    public void incrementAsync(int value) {
        singleThreadExecutor.execute(() -> {
            int currentValue = getValue();
            //如果变动的数值小于刷新速度，则使用变动数值作为刷新速度，否则使用默认刷新速度
            int finalFrequency = Math.min(frequency, value);
            for (int i = 1; i <= finalFrequency; i++) {
                if (((ThreadPoolExecutor) singleThreadExecutor).getQueue().size() > 0) break;
                super.setValue(currentValue + ((value / finalFrequency) * i));
                ThreadUtil.sleep(flowTime / (finalFrequency + i));
            }

            super.setValue(currentValue + value);
        });
    }

    /**
     * <p>
     * 将进度条进度减少指定数值, 使用平滑方式
     * </p>
     *
     * <p>
     * 非同步模式
     * </p>
     *
     * @param value 减少的数值
     */
    public void decrementAsync(int value) {
        singleThreadExecutor.execute(() -> {
            int currentValue = getValue();
            //如果变动的数值小于刷新速度，则使用变动数值作为刷新速度，否则使用默认刷新速度
            int finalFrequency = Math.min(frequency, value);
            for (int i = 0; i < finalFrequency; i++) {
                if (((ThreadPoolExecutor) singleThreadExecutor).getQueue().size() > 0) break;
                super.setValue(currentValue - ((value / finalFrequency) * i));
                ThreadUtil.sleep(flowTime / (finalFrequency + i));
            }

            super.setValue(currentValue - value);
        });
    }
}