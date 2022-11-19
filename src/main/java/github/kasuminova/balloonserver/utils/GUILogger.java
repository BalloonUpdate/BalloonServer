package github.kasuminova.balloonserver.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.dialect.console.ConsoleLog;
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
import java.util.concurrent.ConcurrentLinkedQueue;

import static github.kasuminova.balloonserver.BalloonServer.CONFIG;
import static github.kasuminova.balloonserver.utils.ModernColors.*;

/**
 * 一个简易的 super，与 GUI 同步
 */
public class GUILogger extends ConsoleLog {
    @Serial
    private static final long serialVersionUID = 6797586304881795733L;

    //日期格式
    public static final SimpleDateFormat NORMAL_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat FULL_DATE_FORMAT   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final SimpleAttributeSet INFO_ATTRIBUTE   = new SimpleAttributeSet();
    public static final SimpleAttributeSet WARN_ATTRIBUTE   = new SimpleAttributeSet();
    public static final SimpleAttributeSet ERROR_ATTRIBUTE  = new SimpleAttributeSet();
    public static final SimpleAttributeSet DEBUG_ATTRIBUTE  = new SimpleAttributeSet();
    public static final String INFO  = "INFO";
    public static final String WARN  = "WARN";
    public static final String ERROR = "ERROR";
    public static final String DEBUG = "DEBUG";
    private static final int MAX_LOGFILES = 50;
    private static final int LOG_PANE_MAX_LENGTH = 200000;
    //行距, 0.1 相当于 1.1 倍行距
    private static final float LINE_SPACE_SIZE = 0.1F;
    private JTextPane logPane;
    private final Writer logWriter;
    private final ConcurrentLinkedQueue<LogMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean isStopped = false;

    /**
     * 创建一个 super
     *
     * @param name    log 文件名
     * @param logPane 要同步的 JTextPane
     */
    public GUILogger(String name, JTextPane logPane) {
        this(name);

        setLogPane(logPane);
    }

    /**
     * 创建一个 super
     *
     * @param name log 文件名
     */
    public GUILogger(String name) {
        super(name);

        BalloonServer.GLOBAL_THREAD_POOL.execute(new LoggerThread());
        logPane = null;
        logWriter = createLogFile(name);
    }

    public void setLogPane(JTextPane logPane) {
        this.logPane = logPane;

        //行距设置
        SimpleAttributeSet lineSpaceAttribute = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(lineSpaceAttribute, LINE_SPACE_SIZE);
        logPane.setParagraphAttributes(lineSpaceAttribute, false);

        //设置颜色
        StyleConstants.setForeground(INFO_ATTRIBUTE, BLUE);
        StyleConstants.setForeground(WARN_ATTRIBUTE, YELLOW);
        StyleConstants.setForeground(ERROR_ATTRIBUTE, RED);
        StyleConstants.setForeground(DEBUG_ATTRIBUTE, PURPLE);
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
     * @return 一个对应文件名的 Writer, 如果出现问题, 则返回 null
     */
    public Writer createLogFile(String name) {
        File logFile = new File(String.format("./logs/%s.log", name));
        try {
            if (logFile.exists()) {
                int count = 1;
                while (count <= MAX_LOGFILES) {
                    File oldLogFile = new File(String.format("./logs/%s-%s.log", name, count));
                    if (oldLogFile.exists()) {
                        count++;
                    } else {
                        if (!logFile.renameTo(oldLogFile)) {
                            super.warn("无法正确转移旧日志文件！");
                        }
                        break;
                    }
                }
                return new OutputStreamWriter(Files.newOutputStream(new File(String.format("./logs/%s.log", name)).toPath()), StandardCharsets.UTF_8);
            }

            //检查父文件夹
            if (!logFile.getParentFile().exists()) {
                try {
                    FileUtil.touch(logFile);
                } catch (IORuntimeException e) {
                    super.warn("日志文件夹创建失败！{}", MiscUtils.stackTraceToString(e));
                    return null;
                }
            }

            return new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            super.warn(MiscUtils.stackTraceToString(e));
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

    public void log(String msg, SimpleAttributeSet attributeSet, String level, Object... params) {
        if (isStopped) throw new IllegalStateException("Logger has benn stopped!");

        String threadName = Thread.currentThread().getName();

        messageQueue.offer(new LogMessage(msg, threadName, attributeSet, level, params));
    }

    /**
     * 输出 INFO 日志
     *
     * @param msg                   消息
     * @param attributeSet          自定义颜色属性
     */
    public void info(String msg, SimpleAttributeSet attributeSet) {
        log(msg, attributeSet, INFO);
    }

    /**
     * 输出 INFO 日志
     *
     * @param msg                   消息
     * @param customForegroundColor 自定义颜色
     */
    public void info(String msg, Color customForegroundColor) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attributeSet, customForegroundColor);

        log(msg, attributeSet, INFO);
    }

    /**
     * 输出 INFO 日志
     *
     * @param msg 消息
     */
    public void info(String msg) {
        log(msg, INFO_ATTRIBUTE, INFO);
    }

    /**
     * 输出 INFO 日志
     *
     * @param format    消息
     * @param params    占位符
     */
    public void info(String format, Object... params) {
        log(format, INFO_ATTRIBUTE, INFO, params);
    }

    /**
     * 输出 INFO 日志
     *
     * @param format                消息
     * @param customForegroundColor 自定义颜色
     * @param params                占位符
     */
    public void info(String format, Color customForegroundColor, Object... params) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attributeSet, customForegroundColor);

        log(format, attributeSet, INFO, params);
    }

    /**
     * 输出 DEBUG 日志
     *
     * @param msg 消息
     */
    public void debug(String msg) {
        if (CONFIG.isDebugMode()) log(msg, DEBUG_ATTRIBUTE, DEBUG);
    }

    public void debug(String format, Object... params) {
        if (CONFIG.isDebugMode()) log(format, DEBUG_ATTRIBUTE, DEBUG, params);
    }

    /**
     * 输出 WARN 日志
     *
     * @param msg 消息
     */
    public void warn(String msg) {
        log(msg, WARN_ATTRIBUTE, WARN);
    }

    public void warn(String msg, Object... params) {
        log(msg, WARN_ATTRIBUTE, WARN, params);
    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     */
    public void error(String msg) {
        log(msg, ERROR_ATTRIBUTE, ERROR);
    }

    public void error(String format, Object... params) {
        log(format, ERROR_ATTRIBUTE, ERROR, params);
    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     * @param e   错误信息
     */
    public void error(String msg, Throwable e) {
        log(msg, ERROR_ATTRIBUTE, ERROR, MiscUtils.stackTraceToString(e));
    }

    /**
     * 输出 ERROR 日志
     *
     * @param e 错误信息
     */
    public void error(Throwable e) {
        log("", ERROR_ATTRIBUTE, ERROR, MiscUtils.stackTraceToString(e));
    }

    /**
     * 关闭 Writer, 解除 log 文件的占用
     */
    public void closeLogWriter() throws IOException {
        if (logWriter == null) return;
        logWriter.close();
    }

    public void stop() throws IOException {
        closeLogWriter();

        isStopped = true;
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
        } catch (IOException ignored) {}
    }

    /**
     * 向 logPane 输出信息
     */
    private void insertStringToDocument(String str, SimpleAttributeSet ATTRIBUTE) {
        if (logPane == null) return;
        try {
            Document document = logPane.getDocument();
            if (document.getLength() > LOG_PANE_MAX_LENGTH) {
                document.remove(0, logPane.getDocument().getLength());
                info("日志窗口过长, 已清空当前服务器实例日志窗口.");
            }
            document.insertString(document.getLength(), str, ATTRIBUTE);
            logPane.setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            super.warn(MiscUtils.stackTraceToString(e));
        }
    }

    private class LoggerThread implements Runnable {
        @Override
        public void run() {
            while (!isStopped) {
                while (!messageQueue.isEmpty()) {
                    LogMessage logMessage = messageQueue.poll();

                    String formatMsg = StrUtil.format(logMessage.message, logMessage.params);

                    switch (logMessage.level) {
                        case INFO -> GUILogger.super.info(formatMsg);
                        case WARN -> GUILogger.super.warn(formatMsg);
                        case ERROR -> GUILogger.super.error(formatMsg);
                        case DEBUG -> GUILogger.super.debug(formatMsg);
                    }

                    writeAndFlushMessage(buildFullLogMessage(logMessage.threadName, formatMsg, logMessage.level));
                    insertStringToDocument(buildNormalLogMessage(formatMsg, logMessage.level), logMessage.attributeSet);
                }

                ThreadUtil.sleep(100);
            }
        }
    }

    private record LogMessage(String message, String threadName, SimpleAttributeSet attributeSet, String level, Object[] params) {
    }
}
