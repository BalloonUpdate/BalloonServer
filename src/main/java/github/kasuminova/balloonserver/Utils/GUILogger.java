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
    private final String name;
    private final JTextPane logPane;
    private final Logger logger;
    private Writer logWriter = null;
    /**
     * logger 线程池
     * 用于保证 Log 顺序
     */
    private final ExecutorService loggerThreadPool = Executors.newSingleThreadExecutor();
    private final AtomicInteger prepared = new AtomicInteger(0);
    //日期格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
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
     * @param name 前缀
     * @param logPane 要同步的 JTextPane
     */
    public GUILogger(String name, JTextPane logPane) {
        logger = Logger.getLogger(name);
        this.name = name;
        this.logPane = logPane;

        File logFile = new File(String.format("./%s.log", name));

        try {
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    this.logWriter = new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
                }
            } else {
                this.logWriter = new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.warning(stackTraceToString(e));
        }

        //设置颜色
        StyleConstants.setForeground(INFO_ATTRIBUTE, INFO_COLOR);
        StyleConstants.setForeground(WARN_ATTRIBUTE, WARN_COLOR);
        StyleConstants.setForeground(ERROR_ATTRIBUTE, ERROR_COLOR);
        StyleConstants.setForeground(DEBUG_ATTRIBUTE, DEBUG_COLOR);
    }

    public GUILogger(String name) {
        logger = Logger.getLogger(name);
        logPane = null;
        this.name = name;

        File logFile = new File(String.format("./%s.log", name));

        try {
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    this.logWriter = new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
                }
            } else {
                this.logWriter = new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.warning(stackTraceToString(e));
        }
    }

    public void info(String msg) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            logger.info(msg);
            String logMessage = buildNormalLogMessage(threadName, msg, INFO, name);
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
            String logMessage = buildNormalLogMessage(threadName, msg, DEBUG, name);
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
            String logMessage = buildNormalLogMessage(threadName, msg, WARN, name);
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
            String logMessage = buildNormalLogMessage(threadName, msg, ERROR, name);
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
                    String.format("%s\n%s", msg, stackTraceToString(e)), ERROR, name);
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
            String logMessage = buildNormalLogMessage(threadName, stackTraceToString(e), ERROR, name);
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
            if (document.getLength() > 100000) {
                document.remove(0,logPane.getDocument().getLength());
                info("日志窗口过长，已清空当前服务器实例日志.");
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

    private static String buildNormalLogMessage(String threadName, String msg, String logLevel ,String name) {
        //占位符分别为 日期，线程名，名称，消息本体
//        return String.format("[%s][%s][%s][%s]: %s\n", DATE_FORMAT.format(System.currentTimeMillis()), logLevel, name, threadName, msg);
        return String.format("[%s][%s][%s]: %s\n", DATE_FORMAT.format(System.currentTimeMillis()), logLevel, name, msg);
    }
}
