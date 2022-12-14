package github.kasuminova.balloonserver.utils.filecacheutils;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.httpserver.HttpServerInterface;
import github.kasuminova.balloonserver.servers.localserver.IntegratedServerInterface;
import github.kasuminova.balloonserver.utils.*;
import github.kasuminova.balloonserver.utils.fileobject.AbstractSimpleFileObject;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 从 LittleServer 的 cacheUtils 的内部类独立出来的一个工具，用于计算文件缓存并输出 JSON
 */
public class JsonCacheUtils {
    public static final int TIMER_DELAY = 500;
    private final HttpServerInterface httpServerInterface;
    private final IntegratedServerInterface serverInterface;
    private final JButton startOrStop;
    private final SmoothProgressBar statusProgressBar;
    private final GUILogger logger;
    private final AtomicBoolean isGenerating;
    private final IntegratedServerConfig config;
    //jsonArray 转化为资源文件夹缓存必要的变量
    private final JSONArray jsonArray = new JSONArray();
    //fileObjList，用于序列化 JSON
    private final ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>(8);
    //公用定时器
    private Timer timer;
    private Thread counterThread;
    private FileCacheCalculator fileCacheCalculator;
    public JsonCacheUtils(IntegratedServerInterface serverInterface, HttpServerInterface httpServerInterface, JButton startOrStop) {
        this.serverInterface = serverInterface;
        this.httpServerInterface = httpServerInterface;
        this.startOrStop = startOrStop;

        logger = serverInterface.getLogger();
        isGenerating = serverInterface.isGenerating();
        config = serverInterface.getIntegratedServerConfig();
        statusProgressBar = serverInterface.getStatusProgressBar();
    }

    /**
     * 更新资源缓存结构
     * 传入的参数如果为 null，则完整生成一次缓存
     */
    public void updateDirCache(JSONArray jsonCache, String hashAlgorithm) {
        logger.info(String.format("正在计算 %s 资源缓存结构中...", hashAlgorithm));
        if (jsonCache != null) {
            if (!genDirCache(jsonCache, hashAlgorithm)) {
                return;
            }
            long start = System.currentTimeMillis();

            //等待线程结束
            ThreadUtil.waitForDie(counterThread);

            timer.stop();
            logger.info(String.format("资源变化计算完毕, 正在向磁盘生成 JSON 缓存. (%sms)", System.currentTimeMillis() - start));

            //输出并向服务器设置 JSON
            generateJsonToDiskAndSetServerJson(jsonArray, hashAlgorithm);

            //隐藏状态栏进度条
            statusProgressBar.setVisible(false);
            //重置变量
            isGenerating.set(false);
        } else if (genDirCache(null, hashAlgorithm)) {
            long start = System.currentTimeMillis();

            //等待线程结束
            ThreadUtil.waitForDie(counterThread);

            timer.stop();
            logger.info(String.format("资源变化计算完毕, 正在向磁盘生成 JSON 缓存. (%sms)", System.currentTimeMillis() - start));

            //输出并向服务器设置 JSON
            jsonArray.clear();
            jsonArray.addAll(fileObjList);
            generateJsonToDiskAndSetServerJson(jsonArray, hashAlgorithm);

            //隐藏状态栏进度条
            statusProgressBar.setVisible(false);
            //重置变量
            isGenerating.set(false);
        }
    }

    /**
     * <p>
     * 更新资源缓存结构并启动服务器。
     * </p>
     * <p>
     * 传入的参数如果为 null，则完整生成一次缓存。
     * </p>
     */
    public void updateDirCacheAndStartServer(JSONArray jsonCache, String hashAlgorithm) {
        updateDirCache(jsonCache, hashAlgorithm);

        System.gc();
        logger.info("内存已完成回收.");

        serverInterface.setStatusLabelText("启动中", ModernColors.YELLOW);
        //启动服务器
        if (httpServerInterface.startServer()) {
            startOrStop.setText("关闭服务器");
            serverInterface.isStarted().set(true);
        }
    }

    /**
     * 向磁盘生成 JSON 缓存，并设置服务端 JSON
     *
     * @param jsonArray JSON 缓存
     */
    private void generateJsonToDiskAndSetServerJson(JSONArray jsonArray, String hashAlgorithm) {
        String resJSONStr = jsonArray.toJSONString();
        try {
            if (hashAlgorithm.equals(HashCalculator.SHA1)) {
                serverInterface.setLegacyResJson(resJSONStr);
                FileUtil.createJsonFile(resJSONStr, "./", String.format("%s.%s",
                        serverInterface.getServerName(),
                        serverInterface.getLegacyResJsonFileExtensionName()));
            } else {
                serverInterface.setResJson(resJSONStr);
                FileUtil.createJsonFile(resJSONStr, "./", String.format("%s.%s",
                        serverInterface.getServerName(),
                        serverInterface.getResJsonFileExtensionName()));
            }
            logger.info("JSON 缓存生成完毕.");
        } catch (IORuntimeException ex) {
            logger.error("生成 JSON 缓存的时候出现了问题...", ex);
        }
    }

    /**
     * 以多线程方式生成资源缓存结构
     * <p>
     * 使用配置文件中的资源文件夹路径
     * </p>
     *
     * @return 如果资源文件夹为空返回 false, 否则返回 true
     */
    private boolean genDirCache(JSONArray jsonCache, String hashAlgorithm) {
        long start = System.currentTimeMillis();

        File dir = new File("." + config.getMainDirPath());
        if (!dir.exists()) {
            logger.warn(String.format("设定中的资源目录: %s 不存在, 使用默认路径", dir.getPath()));
            dir = new File("./res");
            if (!dir.exists()) {
                logger.warn(String.format("默认资源目录不存在: %s, 正在创建文件夹", dir.getPath()));
                if (!dir.mkdir()) {
                    logger.error("默认资源目录创建失败！请检查你的资源目录文件夹是否被占用或非文件夹。");
                }
                logger.warn("资源目录为空, 跳过缓存生成.");

                return false;
            }
        }

        logger.info("正在计算资源目录大小...");
        File[] fileList = dir.listFiles();
        //检查文件夹是否为空
        if (fileList == null || fileList.length == 0) {
            logger.warn("资源目录为空, 跳过缓存生成.");

            return false;
        }

        isGenerating.set(true);
        //计算文件夹内的文件和总大小（文件夹不计入），用于进度条显示
        long[] dirSize = FileCacheCalculator.getDirSize(dir, statusProgressBar);

        String totalSize = FileUtil.formatFileSizeToStr(dirSize[0]);

        logger.info(StrUtil.format("文件夹大小: {}, 文件数量: {} ({}ms)", totalSize, dirSize[1], System.currentTimeMillis() - start));

        if (jsonCache != null) {
            logger.info("检测到已缓存的 JSON, 正在检查变化...");
            checkJsonCache(dir, jsonCache, dirSize[1], hashAlgorithm);
        } else {
            logger.info("正在生成资源目录缓存...");
            generateCache(dir, hashAlgorithm, totalSize, dirSize[0], dirSize[1]);
        }

        return true;
    }

    private void checkJsonCache(File dir, JSONArray jsonCache, long totalFiles, String hashAlgorithm) {
        //创建新线程实例并执行
        AtomicInteger completedFiles = new AtomicInteger(0);
        jsonArray.clear();

        counterThread = new Thread(() -> jsonArray.addAll(new JsonCacheCheckerTask(
                dir, AbstractSimpleFileObject.jsonArrToFileObjArr(jsonCache), hashAlgorithm, logger, completedFiles)
                .call().getChildren()));
        counterThread.start();

        serverInterface.resetStatusProgressBar();
        statusProgressBar.setString(String.format("检查变化中: 0 文件 / %s 文件 - ETA: 0s", totalFiles));

        AtomicInteger lastCompletedFiles = new AtomicInteger(0);

        timer = new Timer(TIMER_DELAY, e -> {
            statusProgressBar.setValue((int) ((double) completedFiles.get() * statusProgressBar.getMaximum() / totalFiles));
            statusProgressBar.setString(StrUtil.format("检查变化中: {} 文件 / {} 文件 - ETA: {}s",
                    completedFiles.get(),
                    totalFiles,
                    (totalFiles - completedFiles.get()) / ((completedFiles.get() - lastCompletedFiles.get()) * 2L)));

            lastCompletedFiles.set(completedFiles.get());
        });
        //启动轮询
        timer.start();
        statusProgressBar.setVisible(true);
    }

    private void generateCache(File dir, String hashAlgorithm, String totalSize, long totalFileSize, long totalFiles) {
        //新建资源计算器实例
        fileCacheCalculator = new FileCacheCalculator(hashAlgorithm);
        //创建新线程实例并执行
        counterThread = new Thread(() -> {
            fileObjList.clear();
            fileObjList.addAll(fileCacheCalculator.scanDir(dir));
        });
        counterThread.start();

        serverInterface.resetStatusProgressBar();

        AtomicLong lastCompletedBytes = new AtomicLong(0);
        //轮询线程, 读取进度
        timer = new Timer(TIMER_DELAY, e -> {
            long completedBytes = fileCacheCalculator.getCompletedBytes();
            long completedFiles = fileCacheCalculator.getCompletedFiles();
            String completedSize = FileUtil.formatFileSizeToStr(completedBytes);
            statusProgressBar.setValue((int) ((double) completedBytes * statusProgressBar.getMaximum() / totalFileSize));
            statusProgressBar.setString(StrUtil.format("生成缓存中: {} / {} - {} 文件 / {} 文件 - ETA: {}s",
                    completedSize,
                    totalSize,
                    completedFiles,
                    totalFiles,
                    (totalFileSize - completedBytes) / ((completedBytes - lastCompletedBytes.get()) * 2)));

            lastCompletedBytes.set(completedBytes);
        });
        //启动轮询
        timer.start();
        statusProgressBar.setVisible(true);
    }
}
