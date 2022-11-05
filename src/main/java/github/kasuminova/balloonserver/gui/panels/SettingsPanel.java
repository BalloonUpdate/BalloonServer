package github.kasuminova.balloonserver.gui.panels;

import cn.hutool.core.io.IORuntimeException;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.*;
import github.kasuminova.balloonserver.configurations.BalloonServerConfig;
import github.kasuminova.balloonserver.configurations.CloseOperation;
import github.kasuminova.balloonserver.configurations.ConfigurationManager;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.utils.ModernColors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static github.kasuminova.balloonserver.BalloonServer.*;

/**
 * 设置面板类, 一个主程序只能有一个设置面板。
 * @author Kasumi_Nova
 */
public class SettingsPanel {
    private static final JCheckBox AUTO_START_DEFAULT_SERVER = new JCheckBox("自动启动主服务端");
    private static final JCheckBox AUTO_START_DEFAULT_SERVER_ONCE = new JCheckBox("自动启动主服务端 (单次)");
    private static final JCheckBox AUTO_CHECK_UPDATES = new JCheckBox("自动检查更新");
    private static final JCheckBox AUTO_UPDATE = new JCheckBox("自动更新");
    private static final CloseOperation[] OPERATIONS = {
            BalloonServerConfig.QUERY,
            BalloonServerConfig.HIDE_ON_CLOSE,
            BalloonServerConfig.EXIT_ON_CLOSE
    };
    private static final JComboBox<CloseOperation> CLOSE_OPERATION_COMBO_BOX = new JComboBox<>(OPERATIONS);
    private static final JSpinner FILE_THREAD_POOL_SIZE_SPINNER = new JSpinner();
    private static final JCheckBox LOW_IO_PERFORMANCE_MODE = new JCheckBox("低性能模式 (重启生效)");
    private static final JCheckBox ENABLE_DEBUG_MODE = new JCheckBox("启用 Debug 模式");
    private static final int MAXIMUM_FILE_THREAD_POOL_SIZE = 1024;
    public static JPanel createPanel() {
        //主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        //子主面板
        JPanel subMainPanel = new JPanel(new BorderLayout());

        //设置面板
        JPanel settingsPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP,VFlowLayout.LEFT,5,10,5,5,false,false));

        //自动启动主服务端
        JLabel autoStartDefaultServerDesc = new JLabel("此项选中后, BalloonServer 在启动时会自动启动主服务端的服务器, 无需手动开启服务端.");
        autoStartDefaultServerDesc.setForeground(ModernColors.BLUE);

        //自动启动主服务端（单次）
        JLabel autoStartDefaultServerOnceDesc = new JLabel("此项选中后, BalloonServer 在启动时会自动启动主服务端的服务器, 仅生效一次, 生效后自动关闭.");
        autoStartDefaultServerOnceDesc.setForeground(ModernColors.BLUE);

        //自动检查更新
        JLabel autoCheckUpdatesDesc = new JLabel("此项选中后, BalloonServer 在会在启动时检查最新更新.");
        autoCheckUpdatesDesc.setForeground(ModernColors.BLUE);

        //自动更新
        JLabel autoUpdateDesc = new JLabel("此项及 “自动检查更新” 项选中后, BalloonServer 在检查到更新后, 会自动下载并自动重启应用更新, 如果主服务端正在运行, 则下次启动会自动启动服务器.");
        autoUpdateDesc.setForeground(ModernColors.BLUE);
        //如果程序非 exe 格式则设置为禁用
        if (!ARCHIVE_NAME.contains("e4j")) {
            AUTO_UPDATE.setEnabled(false);
            AUTO_UPDATE.setText("自动更新（不支持, 目前仅支持 exe 格式服务端）");
        }

        Box closeOperationBox = Box.createHorizontalBox();
        CLOSE_OPERATION_COMBO_BOX.setMaximumRowCount(3);
        closeOperationBox.add(new JLabel("窗口关闭选项: "));
        closeOperationBox.add(CLOSE_OPERATION_COMBO_BOX);
        JLabel closeOperationsDesc = new JLabel("此项决定点击 BalloonServer 窗口右上角关闭按钮后程序的操作.");
        closeOperationsDesc.setForeground(ModernColors.BLUE);

        //低性能模式
        JLabel lowIOPerformanceModeDesc0 = new JLabel("此项选中后, 将会限制生成缓存的线程数至单线程, 对于机械盘等低 IO 性能的服务器可能会有性能提升.");
        lowIOPerformanceModeDesc0.setForeground(ModernColors.BLUE);
        JLabel lowIOPerformanceModeDesc1 = new JLabel("此项会覆盖 \"文件计算线程池大小\" 配置.");
        lowIOPerformanceModeDesc1.setForeground(ModernColors.YELLOW);

        //文件线程池大小
        Box fileThreadPoolSizeBox = Box.createHorizontalBox();
        SpinnerNumberModel fileThreadPoolSizeSpinnerModel = new SpinnerNumberModel(0, 0, MAXIMUM_FILE_THREAD_POOL_SIZE, 1);
        FILE_THREAD_POOL_SIZE_SPINNER.setModel(fileThreadPoolSizeSpinnerModel);
        JSpinner.NumberEditor portSpinnerEditor = new JSpinner.NumberEditor(FILE_THREAD_POOL_SIZE_SPINNER, "#");
        FILE_THREAD_POOL_SIZE_SPINNER.setEditor(portSpinnerEditor);
        fileThreadPoolSizeBox.add(new JLabel("文件计算线程池大小 (重启生效): "));
        fileThreadPoolSizeBox.add(FILE_THREAD_POOL_SIZE_SPINNER);
        JLabel fileThreadPoolSizeDesc0 = new JLabel("此项决定在生成资源缓存时同时计算文件校验码的线程数, 默认为 0, 即为逻辑处理器数量 * 2.");
        fileThreadPoolSizeDesc0.setForeground(ModernColors.BLUE);
        JLabel fileThreadPoolSizeDesc1 = new JLabel("如果您不理解此选项, 请勿修改本设置.");
        fileThreadPoolSizeDesc1.setForeground(ModernColors.YELLOW);

        //Debug Mode
        JLabel enableDebugModeDesc = new JLabel("此项仅为开发人员提供, 普通用户请勿开启.");
        enableDebugModeDesc.setForeground(ModernColors.BLUE);

        applyConfiguration();

        //组装
        settingsPanel.add(AUTO_START_DEFAULT_SERVER);
        settingsPanel.add(autoStartDefaultServerDesc);
        settingsPanel.add(AUTO_START_DEFAULT_SERVER_ONCE);
        settingsPanel.add(autoStartDefaultServerOnceDesc);
        settingsPanel.add(AUTO_CHECK_UPDATES);
        settingsPanel.add(autoCheckUpdatesDesc);
        settingsPanel.add(AUTO_UPDATE);
        settingsPanel.add(autoUpdateDesc);
        settingsPanel.add(closeOperationBox);
        settingsPanel.add(closeOperationsDesc);
        settingsPanel.add(LOW_IO_PERFORMANCE_MODE);
        settingsPanel.add(lowIOPerformanceModeDesc0);
        settingsPanel.add(lowIOPerformanceModeDesc1);
        settingsPanel.add(fileThreadPoolSizeBox);
        settingsPanel.add(fileThreadPoolSizeDesc0);
        settingsPanel.add(fileThreadPoolSizeDesc1);
        settingsPanel.add(ENABLE_DEBUG_MODE);
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
        JLabel tipLabel = new JLabel("上方配置修改后, 请点击保存配置按钮来应用配置.", SwingConstants.CENTER);
        tipLabel.setForeground(new Color(255, 75, 75));
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
    public static void applyConfiguration()
    {
        AUTO_START_DEFAULT_SERVER.setSelected(CONFIG.isAutoStartServer());
        AUTO_START_DEFAULT_SERVER_ONCE.setSelected(CONFIG.isAutoStartServerOnce());
        AUTO_CHECK_UPDATES.setSelected(CONFIG.isAutoCheckUpdates());
        AUTO_UPDATE.setSelected(CONFIG.isAutoUpdate());
        CLOSE_OPERATION_COMBO_BOX.setSelectedIndex(CONFIG.getCloseOperation());
        LOW_IO_PERFORMANCE_MODE.setSelected(CONFIG.isLowIOPerformanceMode());
        FILE_THREAD_POOL_SIZE_SPINNER.setValue(CONFIG.getFileThreadPoolSize());
        ENABLE_DEBUG_MODE.setSelected(CONFIG.isDebugMode());
    }

    /**
     * 将 GUI 中的配置保存到配置文件中
     */
    private static void saveConfiguration()
    {
        CONFIG.setAutoStartServer(AUTO_START_DEFAULT_SERVER.isSelected());
        CONFIG.setAutoStartServerOnce(AUTO_START_DEFAULT_SERVER_ONCE.isSelected());
        CONFIG.setAutoCheckUpdates(AUTO_CHECK_UPDATES.isSelected());
        CONFIG.setAutoUpdate(AUTO_UPDATE.isSelected());
        CONFIG.setCloseOperation(CLOSE_OPERATION_COMBO_BOX.getSelectedIndex());
        CONFIG.setLowIOPerformanceMode(LOW_IO_PERFORMANCE_MODE.isSelected());
        CONFIG.setFileThreadPoolSize((int) FILE_THREAD_POOL_SIZE_SPINNER.getValue());
        CONFIG.setDebugMode(ENABLE_DEBUG_MODE.isSelected());

        try {
            ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "balloonserver");
            GLOBAL_LOGGER.info("成功保存主程序配置文件.");
        } catch (IORuntimeException e) {
            GLOBAL_LOGGER.error("主程序配置文件保存失败！", e);
        }
    }

    private static JPanel loadThemeListPanel() {
        //主题切换
        String[] themes = {
                "Atom One Dark Contrast",
                "Moonlight Contrast",
                "Material Palenight Contrast"
        };

        JPanel themeListPanel = new JPanel();
        themeListPanel.setBorder(new TitledBorder("选择界面主题"));

        JList<String> themeList = new JList<>();
        themeList.setListData(themes);
        themeList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        themeList.setSelectedIndex(0);
        themeList.addListSelectionListener(e -> {
            try {
                switch (themeList.getSelectedValue()) {
                    case "Atom One Dark Contrast" -> UIManager.setLookAndFeel(new FlatAtomOneDarkContrastIJTheme());
                    case "Moonlight Contrast" -> UIManager.setLookAndFeel(new FlatMoonlightContrastIJTheme());
                    case "Material Palenight Contrast" -> UIManager.setLookAndFeel(new FlatMaterialPalenightContrastIJTheme());
                }
                SwingUtilities.updateComponentTreeUI(MAIN_FRAME);
            } catch (Exception ex) {
                GLOBAL_LOGGER.error(ex);
            }
        });
        themeListPanel.add(themeList);

        return themeListPanel;
    }
}
