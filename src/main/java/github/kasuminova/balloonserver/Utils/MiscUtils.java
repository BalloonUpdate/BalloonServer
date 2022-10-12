package github.kasuminova.balloonserver.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.net.URI;

import static github.kasuminova.balloonserver.BalloonServer.MAIN_FRAME;
import static github.kasuminova.balloonserver.BalloonServer.TITLE;

public class MiscUtils {
    /**
     * 把指定文本复制到剪贴板
     */
    public static void setClipboardString(String text) {
        //获取系统剪贴板
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //封装文本内容
        Transferable trans = new StringSelection(text);
        //把文本内容设置到系统剪贴板
        clipboard.setContents(trans, null);
    }

    /**
     * 使用系统默认浏览器打开指定链接
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
}
