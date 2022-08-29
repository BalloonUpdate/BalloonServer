package github.kasuminova.balloonserver.Utils;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.text.SimpleDateFormat;
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

    public GUILogger(String name) {
        logger = Logger.getLogger(name);
        this.name = name;
    }

    public synchronized void info(String msg) {
        logger.info(msg);
        Document document = logPane.getDocument();
        StyleConstants.setForeground(attrSet, Color.GREEN);//设置颜色

        try {
            document.insertString(document.getLength(), buildNormalLogMessage("INFO", msg), attrSet);
            logPane.setCaretPosition(document.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void debug(String msg) {
        logger.info(msg);
        Document document = logPane.getDocument();
        StyleConstants.setForeground(attrSet, Color.MAGENTA);//设置颜色

        try {
            document.insertString(document.getLength(), buildNormalLogMessage("DEBUG", msg), attrSet);
            logPane.setCaretPosition(document.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void warn(String msg) {
        logger.warning(msg);
        Document document = logPane.getDocument();
        StyleConstants.setForeground(attrSet, Color.YELLOW);//设置颜色

        try {
            document.insertString(document.getLength(), buildNormalLogMessage("WARN", msg), attrSet);
            logPane.setCaretPosition(document.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void error(String msg) {
        logger.warning(msg);
        Document document = logPane.getDocument();
        StyleConstants.setForeground(attrSet, Color.RED);//设置颜色

        try {
            document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg), attrSet);
            logPane.setCaretPosition(document.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void error(String msg, Throwable e) {
        logger.warning(msg);
        if (e.getLocalizedMessage() != null) {
            logger.warning(e.getLocalizedMessage());
        }
        Document document = logPane.getDocument();
        StyleConstants.setForeground(attrSet, Color.RED);//设置颜色
        try {
            if (e.getLocalizedMessage() != null) {
                document.insertString(document.getLength(), buildNormalLogMessage("ERROR", msg + ": " + e.getLocalizedMessage()), attrSet);
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
    }

    public synchronized void error(Throwable e) {
        if (e.getLocalizedMessage() != null) {
            logger.warning(e.getLocalizedMessage());
        }
        Document document = logPane.getDocument();
        StyleConstants.setForeground(attrSet, Color.RED);//设置颜色
        try {
            if (e.getLocalizedMessage() != null) {
                document.insertString(document.getLength(), buildNormalLogMessage("ERROR", e.getLocalizedMessage()), attrSet);
            }
            logPane.setCaretPosition(document.getLength());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                document.insertString(document.getLength(), "        at " + stackTraceElement + "\n", attrSet);
                logger.warning("        at " + stackTraceElement.toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String buildNormalLogMessage(String level, String msg){
        return "[" + simpleDateFormat.format(System.currentTimeMillis())
                + "][" + level + "]["
                + Thread.currentThread().getName() + "]"
                + name + ": "
                + msg + "\n";
    }
}