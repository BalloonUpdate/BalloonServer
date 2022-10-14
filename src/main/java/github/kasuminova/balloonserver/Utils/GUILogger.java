package github.kasuminova.balloonserver.Utils;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import github.kasuminova.balloonserver.BalloonServer;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static github.kasuminova.balloonserver.Utils.ModernColors.*;

/**
 * 一个简易的 Logger，与 GUI 同步
 */
public class GUILogger {
    //日期格式
    private static final SimpleDateFormat NORMAL_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat FULL_DATE_FORMAT   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleAttributeSet INFO_ATTRIBUTE   = new SimpleAttributeSet();
    private static final SimpleAttributeSet WARN_ATTRIBUTE   = new SimpleAttributeSet();
    private static final SimpleAttributeSet ERROR_ATTRIBUTE  = new SimpleAttributeSet();
    private static final SimpleAttributeSet DEBUG_ATTRIBUTE  = new SimpleAttributeSet();
    private static final String INFO  = "INFO";
    private static final String WARN  = "WARN";
    private static final String ERROR = "ERROR";
    private static final String DEBUG = "DEBUG";
    private final JTextPane logPane;
    private final Log logger;
    private final Writer logWriter;
    /**
     * logger 线程池
     * 用于保证 Log 顺序
     */
    private final ExecutorService loggerThreadPool = Executors.newSingleThreadExecutor();
    private final AtomicInteger prepared = new AtomicInteger(0);

    /**
     * 创建一个 Logger
     *
     * @param name    log 文件名
     * @param logPane 要同步的 JTextPane
     */
    public GUILogger(String name, JTextPane logPane) {
        logger = LogFactory.get(name);
        this.logPane = logPane;

        logWriter = createLogFile(name, logger);

        //设置颜色
        StyleConstants.setForeground(INFO_ATTRIBUTE, BLUE);
        StyleConstants.setForeground(WARN_ATTRIBUTE, YELLOW);
        StyleConstants.setForeground(ERROR_ATTRIBUTE, RED);
        StyleConstants.setForeground(DEBUG_ATTRIBUTE, PURPLE);
    }

    /**
     * 创建一个 Logger
     *
     * @param name log 文件名
     */
    public GUILogger(String name) {
        logger = LogFactory.get(name);
        logPane = null;

        logWriter = createLogFile(name, logger);
    }

    /**
     * <p>
     * 创建一个日志文件。
     * </p>
     * <p>
     * 如果日志文件已存在，则将其有序的重命名（最多保留 50 个）。
     * </p>
     *
     * @param name   文件名
     * @param logger 如果出现问题, 则使用此 logger 警告
     * @return 一个对应文件名的 Writer, 如果出现问题, 则返回 null
     */
    public static Writer createLogFile(String name, Log logger) {
        File logFile = new File(String.format("./logs/%s.log", name));
        try {
            if (logFile.exists()) {
                int count = 1;
                while (count <= 50) {
                    File oldLogFile = new File(String.format("./logs/%s-%s.log", name, count));
                    if (oldLogFile.exists()) {
                        count++;
                    } else {
                        if (!logFile.renameTo(oldLogFile)) {
                            logger.warn("无法正确转移旧日志文件！");
                        }
                        break;
                    }
                }
                return new OutputStreamWriter(Files.newOutputStream(new File(String.format("./logs/%s.log", name)).toPath()), StandardCharsets.UTF_8);
            }

            //检查父文件夹
            if (!logFile.getParentFile().exists()) {
                if (!logFile.mkdirs()) {
                    logger.warn("日志文件夹创建失败！");
                    return null;
                }
            }

            if (logFile.createNewFile()) {
                return new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
            } else {
                logger.warn("创建日志文件失败！");
            }
        } catch (Exception e) {
            logger.warn(MiscUtils.stackTraceToString(e));
        }
        return null;
    }

    private static String buildNormalLogMessage(String msg, String logLevel) {
        //占位符分别为 时间，消息等级，消息本体
        return String.format("[%s][%s]: %s\n", NORMAL_DATE_FORMAT.format(System.currentTimeMillis()), logLevel, msg);
    }

    private static String buildFullLogMessage(String threadName, String msg, String logLevel) {
        //占位符分别为 日期+时间，消息等级，线程名，消息本体
        return String.format("[%s][%s][%s]: %s\n", FULL_DATE_FORMAT.format(System.currentTimeMillis()), logLevel, threadName, msg);
    }

    /**
     * 输出 INFO 日志
     *
     * @param msg 消息
     */
    public void info(String msg) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.info(msg);

            writeAndFlushMessage(buildFullLogMessage(threadName, msg, INFO));
            insertStringToDocument(buildNormalLogMessage(msg, INFO), INFO_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    /**
     * 输出 INFO 日志
     *
     * @param msg                   消息
     * @param customForegroundColor 自定义颜色
     */
    public void info(String msg, Color customForegroundColor) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.info(msg);

            SimpleAttributeSet attributeSet = new SimpleAttributeSet();
            StyleConstants.setForeground(attributeSet, customForegroundColor);
            writeAndFlushMessage(buildFullLogMessage(threadName, msg, INFO));
            insertStringToDocument(buildNormalLogMessage(msg, INFO), attributeSet);
            prepared.getAndDecrement();
        });
    }

    /**
     * 输出 DEBUG 日志
     *
     * @param msg 消息
     */
    public void debug(String msg) {
        if (prepared.get() > 100) return;
        if (!BalloonServer.CONFIG.isDebugMode()) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.info(msg);

            writeAndFlushMessage(buildFullLogMessage(threadName, msg, DEBUG));
            insertStringToDocument(buildNormalLogMessage(msg, DEBUG), DEBUG_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    /**
     * 输出 WARN 日志
     *
     * @param msg 消息
     */
    public void warn(String msg) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.warn(msg);

            writeAndFlushMessage(buildFullLogMessage(threadName, msg, WARN));
            insertStringToDocument(buildNormalLogMessage(msg, WARN), WARN_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     */
    public void error(String msg) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        loggerThreadPool.execute(() -> {
            logger.warn(msg);

            writeAndFlushMessage(buildFullLogMessage(threadName, msg, ERROR));
            insertStringToDocument(buildNormalLogMessage(msg, ERROR), ERROR_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     * @param e   错误信息
     */
    public void error(String msg, Exception e) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.warn(String.format("%s \n %s", msg, MiscUtils.stackTraceToString(e)));

            writeAndFlushMessage(buildFullLogMessage(threadName,
                    String.format("%s\n%s", msg, MiscUtils.stackTraceToString(e)), ERROR));
            insertStringToDocument(buildNormalLogMessage(
                    String.format("%s\n%s", msg, MiscUtils.stackTraceToString(e)), ERROR), ERROR_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    /**
     * 输出 ERROR 日志
     *
     * @param e 错误信息
     */
    public void error(Exception e) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.warn(MiscUtils.stackTraceToString(e));

            writeAndFlushMessage(buildFullLogMessage(threadName, MiscUtils.stackTraceToString(e), ERROR));
            insertStringToDocument(buildNormalLogMessage(MiscUtils.stackTraceToString(e), ERROR), ERROR_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    /**
     * 关闭 Writer, 解除 log 文件的占用
     */
    public void closeLogWriter() throws IOException {
        loggerThreadPool.shutdown();
        if (logWriter == null) return;
        logWriter.close();
    }

    /**
     * 向 log 文件输出字符串
     *
     * @param logMessage 要输出的内容
     */
    private void writeAndFlushMessage(String logMessage) {
        if (logWriter == null) return;
        try {
            logWriter.write(logMessage);
            logWriter.flush();
        } catch (IOException ignored) {
        }
    }

    /**
     * 向 logPane 输出信息
     */
    private void insertStringToDocument(String str, SimpleAttributeSet ATTRIBUTE) {
        if (logPane == null) return;
        try {
            Document document = logPane.getDocument();
            if (document.getLength() > 200000) {
                document.remove(0, logPane.getDocument().getLength());
                info("日志窗口过长, 已清空当前服务器实例日志窗口.");
            }
            document.insertString(document.getLength(), str, ATTRIBUTE);
        } catch (BadLocationException e) {
            logger.warn(MiscUtils.stackTraceToString(e));
        }
    }
}
