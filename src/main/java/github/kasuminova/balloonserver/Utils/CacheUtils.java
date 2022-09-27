package github.kasuminova.balloonserver.Utils;

import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.HTTPServer.HttpServerInterface;
import github.kasuminova.balloonserver.Servers.LittleServerInterface;
import github.kasuminova.balloonserver.Configurations.LittleServerConfig;
import github.kasuminova.balloonserver.Utils.FileObject.AbstractSimpleFileObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static github.kasuminova.balloonserver.BalloonServer.resetStatusProgressBar;
import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_STATUS_PROGRESSBAR;

/**
 * 从 LittleServer 的 cacheUtils 的内部类独立出来的一个工具，用于计算文件缓存并输出 JSON
 */
public class CacheUtils {
    private final HttpServerInterface httpServerInterface;
    private final LittleServerInterface serverInterface;
    public CacheUtils(LittleServerInterface serverInterface, HttpServerInterface httpServerInterface, JButton startOrStop) {
        this.serverInterface = serverInterface;
        this.httpServerInterface = httpServerInterface;
        this.startOrStop = startOrStop;

        logger = serverInterface.getLogger();
        isGenerating = serverInterface.isGenerating();
        config = serverInterface.getConfig();
    }
    private final JButton startOrStop;
    private final GUILogger logger;
    private final AtomicBoolean isGenerating;
    private final LittleServerConfig config;
    //jsonArray 转化为资源文件夹缓存必要的变量
    private final JSONArray jsonArray = new JSONArray();
    private final ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();
    //公用定时器
    private Timer timer;
    //fileObjList，用于序列化 JSON
    private Thread counterThread;
    private NextFileListUtils fileListUtils;

    /**
     * 更新资源缓存结构
     * 传入的参数如果为 null，则完整生成一次缓存
     */
    public void updateDirCache(JSONArray jsonCache) {
        long start = System.currentTimeMillis();

        if (jsonCache != null) {
            if (!genDirCache(jsonCache)) {
                return;
            }
            try {
                //等待线程结束
                counterThread.join();

                timer.stop();
                logger.info(String.format("资源变化计算完毕, 正在向磁盘生成 JSON 缓存. (%sms)", System.currentTimeMillis() - start));

                //输出并向服务器设置 JSON
                generateJsonToDiskAndSetServerJson(jsonArray);

                System.gc();
                logger.info("内存已完成回收.");

                //隐藏状态栏进度条
                GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
                //重置变量
                isGenerating.set(false);
            } catch (InterruptedException e) {
                logger.error("计算资源缓存的时候出现了问题...", e);
            }
        } else if (genDirCache(null)) {
            try {
                //等待线程结束
                counterThread.join();

                timer.stop();
                logger.info(String.format("资源变化计算完毕, 正在向磁盘生成 JSON 缓存. (%sms)", System.currentTimeMillis() - start));

                //输出并向服务器设置 JSON
                jsonArray.clear();
                jsonArray.addAll(fileObjList);
                generateJsonToDiskAndSetServerJson(jsonArray);

                System.gc();
                logger.info("内存已完成回收.");

                //隐藏状态栏进度条
                GLOBAL_STATUS_PROGRESSBAR.setVisible(false);
                //重置变量
                isGenerating.set(false);
            } catch (InterruptedException e) {
                logger.error("计算资源缓存的时候出现了问题...", e);
            }
        }
    }

    /**
     * 更新资源缓存结构并启动服务器
     * 传入的参数如果为 null，则完整生成一次缓存
     */
    public void updateDirCacheAndStartServer(JSONArray jsonCache) {
        if (jsonCache != null) {
            if (!genDirCache(jsonCache)) {
                //启动服务器
                if (httpServerInterface.startServer()) {
                    serverInterface.isStarted().set(true);
                    startOrStop.setText("关闭服务器");
                }
                return;
            }
            try {
                //等待线程结束
                counterThread.join();
                System.gc();
                logger.info("内存已完成回收.");

                timer.stop();
                logger.info("资源变化计算完毕, 正在向磁盘生成 JSON 缓存.");

                //输出并向服务器设置 JSON
                generateJsonToDiskAndSetServerJson(jsonArray);

                //隐藏状态栏进度条
                GLOBAL_STATUS_PROGRESSBAR.setVisible(false);

                //启动服务器
                if (httpServerInterface.startServer()) {
                    startOrStop.setText("关闭服务器");
                    serverInterface.isStarted().set(true);
                }

                //重置变量
                isGenerating.set(false);
            } catch (InterruptedException e) {
                logger.error("计算资源缓存的时候出现了问题...", e);
            }
        } else if (genDirCache(null)) {
            try {
                //等待线程结束
                counterThread.join();
                System.gc();
                logger.info("内存已完成回收.");

                timer.stop();
                logger.info("资源目录缓存生成完毕, 正在向磁盘生成 JSON 缓存.");

                //输出并向服务器设置 JSON
                jsonArray.clear();
                jsonArray.addAll(fileObjList);
                generateJsonToDiskAndSetServerJson(jsonArray);

                //隐藏状态栏进度条
                GLOBAL_STATUS_PROGRESSBAR.setVisible(false);

                //启动服务器
                if (httpServerInterface.startServer()) {
                    startOrStop.setText("关闭服务器");
                    serverInterface.isStarted().set(true);
                }

                //重置变量
                isGenerating.set(false);
            } catch (InterruptedException e) {
                logger.error("计算资源缓存的时候出现了问题...", e);
            }
        } else {
            //启动服务器
            if (httpServerInterface.startServer()) {
                serverInterface.isStarted().set(true);
                startOrStop.setText("关闭服务器");
            }
        }
    }

    /**
     * 向磁盘生成 JSON 缓存，并设置服务端 JSON
     * @param jsonArray JSON 缓存
     */
    private void generateJsonToDiskAndSetServerJson(JSONArray jsonArray) {
        String resJSONStr = jsonArray.toJSONString();
        serverInterface.setResJson(resJSONStr);
        try {
            FileUtil.createJsonFile(resJSONStr, "./", String.format("%s.%s", serverInterface.getServerName(), serverInterface.getResJsonFileExtensionName()));
            logger.info("JSON 缓存生成完毕.");
        } catch (IOException ex) {
            logger.error("生成 JSON 缓存的时候出现了问题...", ex);
        }
    }

    /**
     * 以多线程方式生成资源缓存结构
     * <p>
     * 使用配置文件中的资源文件夹路径
     * </p>
     * @return 如果资源文件夹为空返回 false, 否则返回 true
     */
    private boolean genDirCache(JSONArray jsonCache) {
        File dir = new File("." + config.getMainDirPath());
        if (!dir.exists()) {
            logger.warn(String.format("设定中的资源目录：%s 不存在, 使用默认路径", dir.getPath()));
            dir = new File("./res");
            if (!dir.exists()) {
                logger.warn(String.format("默认资源目录不存在：%s, 正在创建文件夹", dir.getPath()));
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
        long[] dirSize = NextFileListUtils.getDirSize(dir);

        String totalSize = FileUtil.formatFileSizeToStr(dirSize[0]);

        FileCacheCalculator fileCacheCalculator = new FileCacheCalculator(logger, serverInterface.getHashAlgorithm());
        logger.info(String.format("文件夹大小：%s, 文件数量：%s", totalSize,dirSize[1]));
        if (jsonCache != null) {
            logger.info("检测到已缓存的 JSON, 正在检查变化...");

            //创建新线程实例并执行
            File finalDir = dir;
            counterThread = new Thread(() -> {
                jsonArray.clear();
                jsonArray.addAll(fileCacheCalculator.scanDir(jsonCache, finalDir));
            });
            counterThread.start();

            resetStatusProgressBar();
            GLOBAL_STATUS_PROGRESSBAR.setString(String.format("检查变化中：0 文件 / %s 文件", dirSize[1]));
            timer = new Timer(25, e -> {
                int completedFiles = fileCacheCalculator.completedFiles.get();
                GLOBAL_STATUS_PROGRESSBAR.setValue((int) ((double) completedFiles * 1000 / dirSize[1]));
                GLOBAL_STATUS_PROGRESSBAR.setString(String.format("检查变化中：%s 文件 / %s 文件", completedFiles, dirSize[1]));
            });
        } else {
            logger.info("正在生成资源目录缓存...");
            File finalDir = dir;
            //新建资源计算器实例
            fileListUtils = new NextFileListUtils(serverInterface.getHashAlgorithm());
            //创建新线程实例并执行
            counterThread = new Thread(() -> {
                fileObjList.clear();
                fileObjList.addAll(fileListUtils.scanDir(finalDir, logger));
            });
            counterThread.start();

            resetStatusProgressBar();
            //轮询线程, 读取进度
            timer = new Timer(25, e -> {
                long completedBytes = fileListUtils.getCompletedBytes();
                long completedFiles = fileListUtils.getCompletedFiles();
                String completedSize = FileUtil.formatFileSizeToStr(completedBytes);
                GLOBAL_STATUS_PROGRESSBAR.setValue((int) ((double) completedBytes * 1000 / dirSize[0]));
                GLOBAL_STATUS_PROGRESSBAR.setString(String.format("生成缓存中：%s / %s - %s 文件 / %s 文件",
                        completedSize,
                        totalSize,
                        completedFiles,
                        dirSize[1]));
            });
        }
        //启动轮询
        timer.start();
        GLOBAL_STATUS_PROGRESSBAR.setVisible(true);

        return true;
    }
}
