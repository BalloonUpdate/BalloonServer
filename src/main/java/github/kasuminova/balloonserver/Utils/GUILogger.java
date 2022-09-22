package github.kasuminova.balloonserver.Utils;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
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
    private static final ExecutorService loggerThreadPool = Executors.newSingleThreadExecutor();
    //日期格式
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

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
        loggerThreadPool.execute(new Thread(() -> {
            logger.info(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, new Color(30,170,255));//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage("INFO", msg, name), attrSet);
                logPane.setCaretPosition(document.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public synchronized void debug(String msg) {
        loggerThreadPool.execute(new Thread(() -> {
            logger.info(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, new Color(180,70,255));//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage("DEBUG", msg, name), attrSet);
                logPane.setCaretPosition(document.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public synchronized void warn(String msg) {
        loggerThreadPool.execute(new Thread(() -> {
            logger.warning(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, new Color(255,215,0));//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage("WARN", msg, name), attrSet);
                logPane.setCaretPosition(document.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public synchronized void error(String msg) {
        loggerThreadPool.execute(new Thread(() -> {
            logger.warning(msg);
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, new Color(255,75,75));//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg, name), attrSet);
                logPane.setCaretPosition(document.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public synchronized void error(String msg, Exception e) {
        loggerThreadPool.execute(new Thread(() -> {
            logger.warning(msg);
            if (e.getCause() != null) {
                logger.warning(e.getCause().toString());
            }
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, new Color(255,64,64));//设置颜色
            try {
                if (e.getCause() != null) {
                    document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg + ": " + e.getCause(), name), attrSet);
                } else {
                    document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg, name), attrSet);
                }
                logPane.setCaretPosition(document.getLength());
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    document.insertString(document.getLength(), "        at " + stackTraceElement + "\n", attrSet);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));
    }

    public synchronized void error(Exception e) {
        loggerThreadPool.execute(new Thread(() -> {
            if (e.getCause() != null) {
                logger.warning(e.getCause().toString());
            }
            Document document = logPane.getDocument();
            StyleConstants.setForeground(attrSet, new Color(255,64,64));//设置颜色
            try {
                if (e.getCause() != null) {
                    document.insertString(document.getLength(), buildNormalLogMessage("ERROR", e.getCause().toString(), name), attrSet);
                }
                logPane.setCaretPosition(document.getLength());
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    document.insertString(document.getLength(), "        at " + stackTraceElement + "\n", attrSet);
                    logger.warning("        at " + stackTraceElement.toString());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));
    }

    public static String buildNormalLogMessage(String level, String msg, String name){
        //占位符分别为 日期，日志等级，线程名，名称，消息本体
        return String.format("[%s][%s][%s]%s: %s\n", simpleDateFormat.format(System.currentTimeMillis()), level, Thread.currentThread().getName(), name, msg);
    }
}
