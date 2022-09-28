package github.kasuminova.balloonserver.Utils;

import github.kasuminova.balloonserver.BalloonServer;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * 一个简易的 Logger，与 GUI 同步
 */
public class GUILogger {
    private final String name;
    private final JTextPane logPane;
    private final Logger logger;
    /**
     * logger 线程池
     * 用于保证 Log 顺序
     */
    private final ExecutorService loggerThreadPool = Executors.newSingleThreadExecutor();
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
//    private static final String INFO  = "INFO";
//    private static final String WARN  = "WARN";
//    private static final String ERROR = "ERROR";
//    private static final String DEBUG = "DEBUG";

    /**
     * 创建一个 Logger
     * @param name 前缀
     * @param logPane 要同步的 JTextPane
     */
    public GUILogger(String name, JTextPane logPane) {
        logger = Logger.getLogger(name);
        this.name = name;
        this.logPane = logPane;

        //设置颜色
        StyleConstants.setForeground(INFO_ATTRIBUTE, INFO_COLOR);
        StyleConstants.setForeground(WARN_ATTRIBUTE, WARN_COLOR);
        StyleConstants.setForeground(ERROR_ATTRIBUTE, ERROR_COLOR);
        StyleConstants.setForeground(DEBUG_ATTRIBUTE, DEBUG_COLOR);

        try {
            logPane.setFont(logPane.getFont().deriveFont(13f));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void info(String msg) {
        String threadName = Thread.currentThread().getName();
        loggerThreadPool.execute(() -> {
            logger.info(msg);
            insertStringToDocument(buildNormalLogMessage(threadName, msg, name), INFO_ATTRIBUTE);
        });
    }

    public void debug(String msg) {
        if (!BalloonServer.CONFIG.isDebugMode()) {
            return;
        }
        String threadName = Thread.currentThread().getName();
        loggerThreadPool.execute(() -> {
            logger.info(msg);
            insertStringToDocument(buildNormalLogMessage(threadName, msg, name), DEBUG_ATTRIBUTE);
        });
    }

    public void warn(String msg) {
        String threadName = Thread.currentThread().getName();
        loggerThreadPool.execute(() -> {
            logger.warning(msg);
            insertStringToDocument(buildNormalLogMessage(threadName, msg, name), WARN_ATTRIBUTE);
        });
    }

    public void error(String msg) {
        String threadName = Thread.currentThread().getName();
        loggerThreadPool.execute(() -> {
            logger.warning(msg);
            insertStringToDocument(buildNormalLogMessage(threadName, msg, name), ERROR_ATTRIBUTE);
        });
    }

    public void error(String msg, Exception e) {
        String threadName = Thread.currentThread().getName();
        loggerThreadPool.execute(() -> {
            logger.warning(msg);
            logger.warning(stackTraceToString(e));
            insertStringToDocument(buildNormalLogMessage(threadName,
                    String.format("%s\n%s", msg, stackTraceToString(e)), name), ERROR_ATTRIBUTE);
        });
    }

    public void error(Exception e) {
        String threadName = Thread.currentThread().getName();
        loggerThreadPool.execute(() -> {
            logger.warning(stackTraceToString(e));
            insertStringToDocument(buildNormalLogMessage(threadName, stackTraceToString(e), name), ERROR_ATTRIBUTE);
        });
    }

    /*
     * 向 logPane 输出信息
     */
    private void insertStringToDocument(String str, SimpleAttributeSet ATTRIBUTE) {
        try {
            Document document = logPane.getDocument();
            document.insertString(document.getLength(), str, ATTRIBUTE);
        } catch (BadLocationException e) {
            e.printStackTrace();
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

    private static String buildNormalLogMessage(String threadName, String msg, String name) {
        //占位符分别为 日期，线程名，名称，消息本体
        return String.format("[%s][%s][%s]: %s\n", DATE_FORMAT.format(System.currentTimeMillis()),threadName, name, msg);
    }
}
