package github.kasuminova.balloonserver.utils;

import github.kasuminova.balloonserver.BalloonServer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Security {
    /**
     * 检查字符串是否存在非法字符
     *
     * @param c                        对话框父窗口
     * @param str                      要检查的对象
     * @param customUnavailableStrings 自定义非法字符列表
     * @return 未通过返回 true, 通过返回 false
     */
    public static boolean stringIsUnsafe(Container c, String str, String[] customUnavailableStrings) {
        //空字符检查
        if (str == null || str.isEmpty()) {
            return true;
        }

        //非法字符检查
        List<String> unavailableStrList = new ArrayList<>(List.of(
                ":", "*", "?", "<", ">", "|",
                "CON", "AUX",
                "COM1", "COM2", "COM3", "COM4",
                "LPT1", "LPT2", "LPT3",
                "PRN", "NUL"));

        //自定义非法字符
        if (customUnavailableStrings != null) unavailableStrList.addAll(List.of(customUnavailableStrings));

        //循环检查
        for (String s : unavailableStrList) {
            if (str.contains(s)) {
                JOptionPane.showMessageDialog(c, String.format("名称包含非法字符 “%s”.", s), BalloonServer.TITLE, JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
        return false;
    }
}
