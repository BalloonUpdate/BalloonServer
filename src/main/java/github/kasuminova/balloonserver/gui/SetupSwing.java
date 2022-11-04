package github.kasuminova.balloonserver.gui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkContrastIJTheme;
import github.kasuminova.balloonserver.gui.panels.AboutPanel;
import github.kasuminova.balloonserver.utils.ModernColors;

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
    private static final SplashScreen SPLASH = SplashScreen.getSplashScreen();
    private static final Graphics2D SPLASH_GRAPHICS = SPLASH == null ? null : SPLASH.createGraphics();
    private static final Dimension SPLASH_SIZE = SPLASH == null ? null : SPLASH.getSize();
    private static final Image SPLASH_IMAGE = SPLASH == null ? null : new ImageIcon(SPLASH.getImageURL()).getImage();
    public static final float DEFAULT_FONT_SIZE = 13F;
    public static void init() {
        if (SPLASH_GRAPHICS != null) {
            SPLASH_GRAPHICS.setPaintMode();
            SPLASH_GRAPHICS.setFont(SPLASH_GRAPHICS.getFont().deriveFont(22F));
            SPLASH_GRAPHICS.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            SPLASH_GRAPHICS.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            updateSplashProgress(5, "PreLoad");
        }

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

                    if (SPLASH_GRAPHICS != null) SPLASH_GRAPHICS.setFont(font.deriveFont(22F));
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
        updateSplashProgress(25, "已载入 UI");
        ThreadUtil.waitForDie(themeThread);
        updateSplashProgress(35, "已载入主题");

        logger.info("Swing UI Load Completed, Used {}ms", System.currentTimeMillis() - start1);
    }

    /**
     * 更新闪屏进度条, 最大值为 100
     *
     * @param progress 进度
     * @param progressMsg 进度信息
     */
    public static void updateSplashProgress(int progress, String progressMsg) {
        if (SPLASH != null) {
            //绘制背景，覆盖掉上次绘制的内容
            SPLASH_GRAPHICS.drawImage(SPLASH_IMAGE, 0, 0, (img, infoFlags, x, y, width, height) -> false);

            //绘制文字阴影, 65% 透明度
            SPLASH_GRAPHICS.setColor(Color.BLACK);
            SPLASH_GRAPHICS.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,0.6F));
            SPLASH_GRAPHICS.drawString(StrUtil.format("{} - {}%", progressMsg, progress), 27, SPLASH_SIZE.height - 28);
            SPLASH_GRAPHICS.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,1F));
            //绘制文字
            SPLASH_GRAPHICS.setColor(ModernColors.BLUE);
            SPLASH_GRAPHICS.drawString(StrUtil.format("{} - {}%", progressMsg, progress), 25, SPLASH_SIZE.height - 30);
            //更新进度
            SPLASH_GRAPHICS.fillRect(0, SPLASH_SIZE.height - 20, (int) (SPLASH_SIZE.width * ((double) progress / 100)), 10);

            SPLASH.update();
        }
    }

    /**
     * 更新闪屏进度条, 最大值为 100
     *
     * @param progress 进度
     */
    public static void updateSplashProgress(int progress) {
        updateSplashProgress(progress, "载入中");
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
