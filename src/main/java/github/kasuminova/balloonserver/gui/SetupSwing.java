package github.kasuminova.balloonserver.gui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkContrastIJTheme;
import github.kasuminova.balloonserver.gui.panels.AboutPanel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.InputStream;

/**
 * @author Kasumi_Nova
 * 一个工具类，用于初始化 Swing（即美化）
 */
public class SetupSwing {
    public static final float DEFAULT_FONT_SIZE = 13F;
    public static void init() {
        Log logger = LogFactory.get("SwingThemeLoader");

        long start1 = System.currentTimeMillis();

        //UI 配置线程
        Thread uiThread = new Thread(() -> {
            LineBorder lineBorder = new LineBorder(new Color(55, 60, 70), 1, true);

            long start = System.currentTimeMillis();
            //设置圆角弧度
            UIManager.put("Button.arc", 7);
            UIManager.put("Component.arc", 7);
            UIManager.put("ProgressBar.arc", 7);
            UIManager.put("TextComponent.arc", 5);
            UIManager.put("CheckBox.arc", 3);
            //设置滚动条
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.thumbArc", 7);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));
            //选项卡分隔线
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
            //菜单
            UIManager.put("MenuItem.selectionType", "underline");
            UIManager.put("MenuItem.underlineSelectionHeight", 3);
            UIManager.put("MenuItem.margin", new Insets(5, 8, 3, 5));
            UIManager.put("MenuBar.underlineSelectionHeight", 3);
            //窗口标题居中
            UIManager.put("TitlePane.centerTitle", true);
            //进度条
            UIManager.put("ProgressBar.repaintInterval", 15);
            UIManager.put("ProgressBar.cycleTime", 7500);
            //提示框
            UIManager.put("ToolTip.border", lineBorder);

            logger.info("UI Load Completed, Used {}ms", System.currentTimeMillis() - start);
        });
        uiThread.start();

        //字体更换线程
        Thread fontThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            //抗锯齿字体
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
            System.setProperty("swing.aatext", "true");
            //设置字体
            try {
                InputStream HMOSSansAndJBMono = SetupSwing.class.getResourceAsStream("/font/HarmonyOS_Sans_SC+JetBrains_Mono.ttf");
                InputStream HMOSSansAndSaira = AboutPanel.class.getResourceAsStream("/font/HarmonyOS_Sans_SC+Saira.ttf");
                Font font;
                //日志窗口字体
                if (HMOSSansAndJBMono != null) {
                    font = Font.createFont(Font.TRUETYPE_FONT, HMOSSansAndJBMono).deriveFont(DEFAULT_FONT_SIZE);
                    UIManager.put("TextPane.font", font);
                }
                //全局字体 + 标题字体
                if (HMOSSansAndSaira != null) {
                    font = Font.createFont(Font.TRUETYPE_FONT, HMOSSansAndSaira).deriveFont(DEFAULT_FONT_SIZE);
                    initGlobalFont(font);
                    UIManager.put("TitlePane.font", font.deriveFont(DEFAULT_FONT_SIZE + 2.5F));
                }
            } catch (Exception e) {
                logger.error(e);
            }
            logger.info("Font Load Completed, Used {}ms", System.currentTimeMillis() - start);
        });
        fontThread.start();

        Thread themeThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            //更新 UI
            try {
                UIManager.setLookAndFeel(new FlatAtomOneDarkContrastIJTheme());
            } catch (Exception e) {
                logger.error("Failed to initialize LaF", e);
            }
            logger.info("Theme Load Completed, Used {}ms", System.currentTimeMillis() - start);
        });
        themeThread.start();

        ThreadUtil.waitForDie(uiThread);
        ThreadUtil.waitForDie(fontThread);
        ThreadUtil.waitForDie(themeThread);

        logger.info("Swing UI Load Completed, Used {}ms", System.currentTimeMillis() - start1);
    }

    /**
     * 载入全局字体
     *
     * @param font 字体
     */
    private static void initGlobalFont(Font font) {
        FontUIResource fontResource = new FontUIResource(font);
        UIManager.getDefaults().keySet().forEach((key) -> {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, fontResource);
            }
        });
    }
}
