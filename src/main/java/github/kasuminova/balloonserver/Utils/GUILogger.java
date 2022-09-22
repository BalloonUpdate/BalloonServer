package github.kasuminova.balloonserver.Utils;

import javax.swing.*;
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
 * 使用 synchronized 是为了防止多个线程同时输出 log 导致 logPane 内容混乱
 */
public class GUILogger {
    private final String name;
    private final JTextPane logPane;
    private final Logger logger;
    private static final SimpleAttributeSet attrSet = new SimpleAttributeSet();
    //logger 线程池
    private static final ExecutorService LOGGER_THREAD_POOL = Executors.newSingleThreadExecutor();
    //日期格式
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
    private static final Color INFO_COLOR  = new Color(30,170,255);
    private static final Color WARN_COLOR  = new Color(255,215,0);
    private static final Color ERROR_COLOR = new Color(255,75,75);
    private static final Color DEBUG_COLOR = new Color(180,70,255);
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
        try {
            logPane.setFont(logPane.getFont().deriveFont(14f));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void info(String msg) {
        LOGGER_THREAD_POOL.execute(() -> {
            logger.info(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, INFO_COLOR);//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage(INFO, msg, name), attrSet);
                logPane.setCaretPosition(document.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public synchronized void debug(String msg) {
        LOGGER_THREAD_POOL.execute(() -> {
            logger.info(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, DEBUG_COLOR);//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage(DEBUG, msg, name), attrSet);
                logPane.setCaretPosition(document.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public synchronized void warn(String msg) {
        LOGGER_THREAD_POOL.execute(() -> {
            logger.warning(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, WARN_COLOR);//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage(WARN, msg, name), attrSet);
                logPane.setCaretPosition(document.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public synchronized void error(String msg) {
        LOGGER_THREAD_POOL.execute(() -> {
            logger.warning(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, ERROR_COLOR);//设置颜色
            try {
                document.insertString(document.getLength(), buildNormalLogMessage(ERROR, msg, name), attrSet);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public synchronized void error(String msg, Exception e) {
        LOGGER_THREAD_POOL.execute(() -> {
            logger.warning(msg);
            logger.warning(stackTraceToString(e));
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, ERROR_COLOR);//设置颜色
            try {
                document.insertString(document.getLength(),
                        buildNormalLogMessage(ERROR,
                                String.format("%s\n%s", msg, stackTraceToString(e)), name),
                        attrSet);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public synchronized void error(Exception e) {
        LOGGER_THREAD_POOL.execute(() -> {
            logger.warning(stackTraceToString(e));
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, ERROR_COLOR);//设置颜色
            try {
                document.insertString(document.getLength(), buildNormalLogMessage(ERROR, stackTraceToString(e), name), attrSet);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * 将错误打印成字符串
     * 输出类似 printStackTrace() 的内容
     * @param e Exception
     * @return 字符串
     */
    private static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private static String buildNormalLogMessage(String level, String msg, String name) {
        //占位符分别为 日期，日志等级，线程名，名称，消息本体
        return String.format("[%s][%s][%s]%s: %s\n", simpleDateFormat.format(System.currentTimeMillis()), level, Thread.currentThread().getName(), name, msg);
    }
}
