package github.kasuminova.balloonserver.gui;

import cn.hutool.core.thread.ThreadUtil;

import javax.swing.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 平滑进度条
 */
public class SmoothProgressBar extends JProgressBar {
    public static final long TIME_MULTIPLIER = 3L;
    //单线程线程池，用于保证进度条的操作顺序
    private final ThreadPoolExecutor singleThreadExecutor;
    private final int flowTime;
    //每秒刷新频率
    private final int frequency;

    /**
     * 创建一个平滑进度条，基于 JProgressBar
     *
     * @param max      进度条最大值
     * @param flowTime 每次变动进度时消耗的时间，时间越长进度条越平滑，并除以 10 作为 frequency
     */
    public SmoothProgressBar(int max, int flowTime) {
        super(0, max);
        this.flowTime = flowTime;
        frequency = flowTime / 10;
        singleThreadExecutor = new ThreadPoolExecutor(1, 1,
                flowTime * 2L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    /**
     * <p>
     * 以平滑方式设置进度
     * </p>
     *
     * @param value 新进度
     */
    @Override
    public void setValue(int value) {
        singleThreadExecutor.execute(() -> {
            int currentValue = getValue();
            if (value < currentValue) {
                decrement(currentValue - value);
            } else {
                increment(value - currentValue);
            }
        });
    }

    @Override
    public void setVisible(boolean aFlag) {
        //保证在进度条在完成先前所有的 加/减 操作后，再进行 setVisible 操作
        singleThreadExecutor.execute(() -> super.setVisible(aFlag));
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
            int queueSize = singleThreadExecutor.getQueue().size();

            super.setValue(currentValue + ((value / finalFrequency) * i));

            //如果线程池中的任务过多则加快进度条速度（即降低 sleep 时间）
            if (queueSize >= 1) {
                ThreadUtil.safeSleep((flowTime / (finalFrequency + (i * TIME_MULTIPLIER))) * (1 / queueSize));
            } else {
                ThreadUtil.safeSleep(flowTime / (finalFrequency + (i * TIME_MULTIPLIER)));
            }
        }

        //如果最后进度条的值差异过大，则重新进行一次 increment
        int lastValue = (currentValue + value) - getValue();
        if (lastValue >= 3) {
            increment(lastValue);
        } else {
            //防止差异，设置为最终结果值
            super.setValue(currentValue + value);
        }
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
            int queueSize = singleThreadExecutor.getQueue().size();

            super.setValue(currentValue - ((value / finalFrequency) * i));

            //如果线程池中的任务过多则加快进度条速度（即降低 sleep 时间）
            if (queueSize >= 1) {
                ThreadUtil.safeSleep((flowTime / (finalFrequency + (i * TIME_MULTIPLIER))) * (1 / queueSize));
            } else {
                ThreadUtil.safeSleep(flowTime / (finalFrequency + (i * TIME_MULTIPLIER)));
            }
        }

        //如果最后进度条的值差异过大，则重新进行一次 increment
        int lastValue = (currentValue - value) - getValue();
        if (lastValue >= 3) {
            decrement(lastValue);
        } else {
            //防止差异，设置为最终结果值
            super.setValue(currentValue - value);
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        singleThreadExecutor.shutdownNow();
    }
}
