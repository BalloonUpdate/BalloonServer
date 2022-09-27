package github.kasuminova.balloonserver.GUI.Panels;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatArcDarkContrastIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkContrastIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme;
import github.kasuminova.balloonserver.Configurations.BalloonServerConfig;
import github.kasuminova.balloonserver.Configurations.CloseOperations;
import github.kasuminova.balloonserver.Configurations.ConfigurationManager;
import github.kasuminova.balloonserver.GUI.LayoutManager.VFlowLayout;
import github.kasuminova.balloonserver.Utils.GUILogger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static github.kasuminova.balloonserver.BalloonServer.*;

/**
 * 设置面板类，一个主程序只能有一个设置面板。
 */
public class SettingsPanel {
    private static final JCheckBox autoStartDefaultServer = new JCheckBox("自动启动主服务端");
    private static final JCheckBox autoStartDefaultServerOnce = new JCheckBox("自动启动主服务端（单次）");
    private static final JCheckBox autoCheckUpdates = new JCheckBox("自动检查更新");
    private static final JCheckBox autoUpdate = new JCheckBox("自动更新");
    private static final Vector<CloseOperations> operations = new Vector<>();
    private static final JComboBox<CloseOperations> closeOperationComboBox = new JComboBox<>(operations);
    private static final JCheckBox enableDebugMode = new JCheckBox("启用 Debug 模式");
    public static JPanel getPanel() {
        //主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        //子主面板
        JPanel subMainPanel = new JPanel(new BorderLayout());

        //设置面板
        JPanel settingsPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP,VFlowLayout.LEFT,5,10,5,5,false,false));

        //自动启动主服务端
        JLabel autoStartDefaultServerDesc = new JLabel("此项选中后，BalloonServer 在启动时会自动启动主服务端的服务器，无需手动开启服务端.");

        //自动启动主服务端
        JLabel autoStartDefaultServerOnceDesc = new JLabel("此项选中后，BalloonServer 在启动时会自动启动主服务端的服务器，仅生效一次，生效后自动关闭.");

        //自动检查更新
        JLabel autoCheckUpdatesDesc = new JLabel("此项选中后，BalloonServer 在会在启动时检查最新更新.");

        //自动更新
        JLabel autoUpdateDesc = new JLabel("此项及“自动检查更新”项选中后，BalloonServer 在检查到更新后，会自动下载并自动重启应用更新，如果主服务端正在运行，则下次启动会自动启动服务器.");
        //如果程序非 exe 格式则设置为禁用
        if (!ARCHIVE_NAME.contains("e4j")) {
            autoUpdate.setEnabled(false);
            autoUpdate.setText("自动更新（不支持，目前仅支持 exe 格式服务端）");
        }

        //关闭选项
        operations.add(BalloonServerConfig.QUERY);
        operations.add(BalloonServerConfig.HIDE_ON_CLOSE);
        operations.add(BalloonServerConfig.EXIT_ON_CLOSE);

        Box closeOperationBox = Box.createHorizontalBox();
        closeOperationComboBox.setMaximumRowCount(3);
        closeOperationBox.add(new JLabel("窗口关闭选项："));
        closeOperationBox.add(closeOperationComboBox);
        JLabel closeOperationsDesc = new JLabel("此项决定点击 BalloonServer 窗口右上角关闭按钮后程序的操作.");

        //Debug Mode
        JLabel enableDebugModeDesc = new JLabel("此项仅为开发人员提供，普通用户请勿开启.");

        applyConfiguration();

        //组装
        settingsPanel.add(autoStartDefaultServer);
        settingsPanel.add(autoStartDefaultServerDesc);
        settingsPanel.add(autoStartDefaultServerOnce);
        settingsPanel.add(autoStartDefaultServerOnceDesc);
        settingsPanel.add(autoCheckUpdates);
        settingsPanel.add(autoCheckUpdatesDesc);
        settingsPanel.add(autoUpdate);
        settingsPanel.add(autoUpdateDesc);
        settingsPanel.add(closeOperationBox);
        settingsPanel.add(closeOperationsDesc);
        settingsPanel.add(enableDebugMode);
        settingsPanel.add(enableDebugModeDesc);

        JScrollPane settingsPanelScroll = new JScrollPane(
                settingsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        settingsPanelScroll.getVerticalScrollBar().setUnitIncrement(20);
        settingsPanelScroll.setBorder(new TitledBorder("主程序设置"));
        subMainPanel.add(settingsPanelScroll, BorderLayout.CENTER);

        JPanel saveConfigPanel = new JPanel(new VFlowLayout());
        //保存提示
        JLabel tipLabel = new JLabel("上方配置修改后，请点击保存配置按钮来应用配置.", SwingConstants.CENTER);
        tipLabel.setForeground(new Color(255,75,75));
        saveConfigPanel.add(tipLabel);

        //保存配置
        JButton saveConfig = new JButton("保存配置");
        saveConfig.addActionListener(e -> saveConfiguration());
        saveConfigPanel.add(saveConfig);

        subMainPanel.add(saveConfigPanel, BorderLayout.SOUTH);

        mainPanel.add(subMainPanel, BorderLayout.CENTER);
        mainPanel.add(loadThemeListPanel(), BorderLayout.WEST);

        return mainPanel;
    }

    /**
     * 将配置文件内的配置应用到 GUI 中
     */
    private static void applyConfiguration()
    {
        autoStartDefaultServer.setSelected(CONFIG.isAutoStartServer());
        autoStartDefaultServerOnce.setSelected(CONFIG.isAutoStartServerOnce());
        autoCheckUpdates.setSelected(CONFIG.isAutoCheckUpdates());
        autoUpdate.setSelected(CONFIG.isAutoUpdate());
        closeOperationComboBox.setSelectedIndex(CONFIG.getCloseOperation());
        enableDebugMode.setSelected(CONFIG.isDebugMode());
    }

    /**
     * 将 GUI 中的配置保存到配置文件中
     */
    private static void saveConfiguration()
    {
        CONFIG.setAutoStartServer(autoStartDefaultServer.isSelected());
        CONFIG.setAutoStartServerOnce(autoStartDefaultServerOnce.isSelected());
        CONFIG.setAutoCheckUpdates(autoCheckUpdates.isSelected());
        CONFIG.setAutoUpdate(autoUpdate.isSelected());
        CONFIG.setCloseOperation(closeOperationComboBox.getSelectedIndex());
        CONFIG.setDebugMode(enableDebugMode.isSelected());

        try {
            ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
            GLOBAL_LOGGER.info("成功保存主程序配置文件.");
        } catch (IOException e) {
            GLOBAL_LOGGER.warning("主程序配置文件保存失败！\n" + GUILogger.stackTraceToString(e));
        }
    }

    private static JPanel loadThemeListPanel() {
        //主题切换
        List<String> themes = new ArrayList<>();
        themes.add("FlatLaf Light");
        themes.add("FlatLaf Dark");
        themes.add("FlatLaf IntelIJ");
        themes.add("FlatLaf Dracula");
        themes.add("Atom One Dark");
        themes.add("Atom One Dark Contrast");
        themes.add("Arc Dark Contrast");
        themes.add("Material Design Dark");

        JPanel themeListPanel = new JPanel();
        themeListPanel.setBorder(new TitledBorder("选择界面主题"));

        JList<String> themeList = new JList<>();
        themeList.setListData(themes.toArray(new String[0]));
        themeList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        themeList.setSelectedIndex(5);
        themeList.addListSelectionListener(e -> {
            try {
                switch (themeList.getSelectedValue()) {
                    case "FlatLaf Light" -> UIManager.setLookAndFeel(new FlatLightLaf());
                    case "FlatLaf Dark" -> UIManager.setLookAndFeel(new FlatDarkLaf());
                    case "FlatLaf IntelIJ" -> UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    case "FlatLaf Dracula" -> UIManager.setLookAndFeel(new FlatDarculaLaf());
                    case "Atom One Dark" -> UIManager.setLookAndFeel(new FlatAtomOneDarkIJTheme());
                    case "Atom One Dark Contrast" -> UIManager.setLookAndFeel(new FlatAtomOneDarkContrastIJTheme());
                    case "Arc Dark Contrast" -> UIManager.setLookAndFeel(new FlatArcDarkContrastIJTheme());
                    case "Material Design Dark" -> UIManager.setLookAndFeel(new FlatMaterialDesignDarkIJTheme());
                }
                SwingUtilities.updateComponentTreeUI(MAIN_FRAME);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        themeListPanel.add(themeList);

        return themeListPanel;
    }
}
