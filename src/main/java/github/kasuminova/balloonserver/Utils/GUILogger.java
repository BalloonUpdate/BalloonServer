package github.kasuminova.balloonserver.Utils;

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
import java.util.logging.Logger;

/**
 * 一个简易的 Logger，与 GUI 同步
 */
public class GUILogger {
    private final JTextPane logPane;
    private final Logger logger;
    private final Writer logWriter;
    /**
     * logger 线程池
     * 用于保证 Log 顺序
     */
    private final ExecutorService loggerThreadPool = Executors.newSingleThreadExecutor();
    private final AtomicInteger prepared = new AtomicInteger(0);
    //日期格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Color INFO_COLOR  = new Color(30,170,255);
    private static final Color WARN_COLOR  = new Color(255,215,0);
    private static final Color ERROR_COLOR = new Color(255,75,75);
    private static final Color DEBUG_COLOR = new Color(180,70,255);
    private static final SimpleAttributeSet INFO_ATTRIBUTE  = new SimpleAttributeSet();
    private static final SimpleAttributeSet WARN_ATTRIBUTE  = new SimpleAttributeSet();
    private static final SimpleAttributeSet ERROR_ATTRIBUTE = new SimpleAttributeSet();
    private static final SimpleAttributeSet DEBUG_ATTRIBUTE = new SimpleAttributeSet();
    private static final String INFO  = "INFO";
    private static final String WARN  = "WARN";
    private static final String ERROR = "ERROR";
    private static final String DEBUG = "DEBUG";

    /**
     * 创建一个 Logger
     * @param name log 文件名
     * @param logPane 要同步的 JTextPane
     */
    public GUILogger(String name, JTextPane logPane) {
        logger = Logger.getLogger(name);
        this.logPane = logPane;

        logWriter = createLogFile(name, logger);

        //设置颜色
        StyleConstants.setForeground(INFO_ATTRIBUTE, INFO_COLOR);
        StyleConstants.setForeground(WARN_ATTRIBUTE, WARN_COLOR);
        StyleConstants.setForeground(ERROR_ATTRIBUTE, ERROR_COLOR);
        StyleConstants.setForeground(DEBUG_ATTRIBUTE, DEBUG_COLOR);
    }

    /**
     * 创建一个 Logger
     * @param name log 文件名
     */
    public GUILogger(String name) {
        logger = Logger.getLogger(name);
        logPane = null;

        logWriter = createLogFile(name, logger);
    }

    public void info(String msg) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.info(msg);
            String logMessage = buildNormalLogMessage(threadName, msg, INFO);
            writeAndFlushMessage(logMessage);
            insertStringToDocument(logMessage, INFO_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    public void debug(String msg) {
        if (prepared.get() > 100) return;
        if (!BalloonServer.CONFIG.isDebugMode()) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.info(msg);
            String logMessage = buildNormalLogMessage(threadName, msg, DEBUG);
            writeAndFlushMessage(logMessage);
            insertStringToDocument(logMessage, DEBUG_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    public void warn(String msg) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.warning(msg);
            String logMessage = buildNormalLogMessage(threadName, msg, WARN);
            writeAndFlushMessage(logMessage);
            insertStringToDocument(logMessage, WARN_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    public void error(String msg) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        loggerThreadPool.execute(() -> {
            logger.warning(msg);
            String logMessage = buildNormalLogMessage(threadName, msg, ERROR);
            writeAndFlushMessage(logMessage);
            insertStringToDocument(logMessage, ERROR_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    public void error(String msg, Exception e) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.warning(msg);
            logger.warning(stackTraceToString(e));
            String logMessage = buildNormalLogMessage(threadName,
                    String.format("%s\n%s", msg, stackTraceToString(e)), ERROR);
            writeAndFlushMessage(logMessage);
            insertStringToDocument(logMessage, ERROR_ATTRIBUTE);
            prepared.getAndDecrement();
        });
    }

    public void error(Exception e) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.warning(stackTraceToString(e));
            String logMessage = buildNormalLogMessage(threadName, stackTraceToString(e), ERROR);
            writeAndFlushMessage(logMessage);
            insertStringToDocument(logMessage, ERROR_ATTRIBUTE);
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
     * @param logMessage 要输出的内容
     */
    private void writeAndFlushMessage(String logMessage) {
        if (logWriter == null) return;
        try {
            logWriter.write(logMessage);
            logWriter.flush();
        } catch (IOException ignored) {}
    }

    /*
     * 向 logPane 输出信息
     */
    private void insertStringToDocument(String str, SimpleAttributeSet ATTRIBUTE) {
        if (logPane == null) return;
        try {
            Document document = logPane.getDocument();
            if (document.getLength() > 200000) {
                document.remove(0,logPane.getDocument().getLength());
                info("日志窗口过长，已清空当前服务器实例日志窗口.");
            }
            document.insertString(document.getLength(), str, ATTRIBUTE);
        } catch (BadLocationException e) {
            logger.warning(stackTraceToString(e));
        }
    }

    /**
     * 将错误打印成字符串
     * 输出类似 printStackTrace() 的内容
     * @param e Exception
     * @return 字符串
     */
    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    /**
     * <p>
     *     创建一个日志文件。
     * </p>
     * <p>
     *     如果日志文件已存在，则将其有序的重命名（最多保留 50 个）。
     * </p>
     * @param name 文件名
     * @param logger 如果出现问题, 则使用此 logger 警告
     * @return 一个对应文件名的 Writer, 如果出现问题, 则返回 null
     */
    public static Writer createLogFile(String name, Logger logger) {
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
                            logger.warning("无法正确转移旧日志文件！");
                        }
                        break;
                    }
                }
                return new OutputStreamWriter(Files.newOutputStream(new File(String.format("./logs/%s.log", name)).toPath()), StandardCharsets.UTF_8);
            }

            //检查父文件夹
            if (!logFile.getParentFile().exists()) {
                if (!logFile.mkdirs()) {
                    logger.warning("日志文件夹创建失败！");
                    return null;
                }
            }

            if (logFile.createNewFile()) {
                return new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
            } else {
                logger.warning("创建日志文件失败！");
            }
        } catch (Exception e) {
            logger.warning(stackTraceToString(e));
        }
        return null;
    }

    private static String buildNormalLogMessage(String threadName, String msg, String logLevel) {
        //占位符分别为 日期，线程名，名称，消息本体
        return String.format("[%s][%s][%s]: %s\n", DATE_FORMAT.format(System.currentTimeMillis()), logLevel, threadName, msg);
    }
}
