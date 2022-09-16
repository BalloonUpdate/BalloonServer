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
 */
public class GUILogger {
    String name;
    JTextPane logPane;
    Logger logger;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
    SimpleAttributeSet attrSet = new SimpleAttributeSet();
    //logger 线程池
    ExecutorService loggerThreadPool = Executors.newSingleThreadExecutor();

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
            StyleConstants.setForeground(attrSet, new Color(30,144,255));//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage("INFO", msg), attrSet);
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
            StyleConstants.setForeground(attrSet, new Color(160,32,240));//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage("DEBUG", msg), attrSet);
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
                document.insertString(document.getLength(), buildNormalLogMessage("WARN", msg), attrSet);
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
            StyleConstants.setForeground(attrSet, new Color(255,64,64));//设置颜色

            try {
                document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg), attrSet);
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
                    document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg + ": " + e.getCause()), attrSet);
                } else {
                    document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg), attrSet);
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
                    document.insertString(document.getLength(), buildNormalLogMessage("ERROR", e.getCause().toString()), attrSet);
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

    public String buildNormalLogMessage(String level, String msg){
        return "[" + simpleDateFormat.format(System.currentTimeMillis())
                + "][" + level + "]["
                + Thread.currentThread().getName() + "]"
                + name + ": "
                + msg + "\n";
    }
}
