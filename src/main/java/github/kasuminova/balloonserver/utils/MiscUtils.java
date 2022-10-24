package github.kasuminova.balloonserver.utils;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;

import static github.kasuminova.balloonserver.BalloonServer.MAIN_FRAME;
import static github.kasuminova.balloonserver.BalloonServer.TITLE;

public class MiscUtils {
    /**
     * 使用系统默认浏览器打开指定链接
     *
     * @param url 链接
     */
    public static void openLinkInBrowser(String url) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(URI.create(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(MAIN_FRAME,
                    "当前系统没有默认浏览器或不支持！", TITLE,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * <p>
     * 将错误打印成字符串。
     * </p>
     * <p>
     * 类似 printStackTrace()。
     * </p>
     *
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

    public static String formatTime(long time) {
        if (time < 1000 * 10) {
            return String.format("%.3fs", (double) time / 1000);
        } else if (time < 1000 * 100) {
            return String.format("%.2fs", (double) time / 1000);
        } else if (time < 1000 * 1000) {
            return String.format("%.1fs", (double) time / 1000);
        } else {
            return String.format("%ss", time / 1000);
        }
    }
}
