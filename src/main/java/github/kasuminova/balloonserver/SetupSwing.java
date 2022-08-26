package github.kasuminova.balloonserver;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;
import java.util.Objects;

public class SetupSwing {
    public static void init() {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        //设置字体
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(BalloonServer.class.getResourceAsStream("/font/sarasa-mono-sc-regular.ttf")));
            Font newFont = font.deriveFont(14f);
            initGlobalFont(newFont);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //更新 UI
        try {
            UIManager.setLookAndFeel(new FlatAtomOneDarkIJTheme());
        } catch( Exception ex ) {
            System.err.println("Failed to initialize LaF");
        }

        //设置圆角弧度
        UIManager.put("Button.arc", 7);
        UIManager.put("Component.arc", 7);
        UIManager.put("CheckBox.arc", 3);
        UIManager.put("ProgressBar.arc", 7);
        UIManager.put("TextComponent.arc", 5);

        //设置滚动条
        UIManager.put("ScrollBar.showButtons", true);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 14);
        UIManager.put("ScrollBar.trackInsets", new Insets(2,5,2,5));
        UIManager.put("ScrollBar.thumbInsets", new Insets(2,4,2,4));
        UIManager.put("ScrollBar.track", new Color(0xC1C1C1));

        //选项卡分隔线/背景
        UIManager.put("TabbedPane.showTabSeparators", true);
    }

    /**
     * 载入全局字体
     * @param font 字体
     */
    public static void initGlobalFont(Font font) {
        FontUIResource fontResource = new FontUIResource(font);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontResource);
            }
        }
    }
}
