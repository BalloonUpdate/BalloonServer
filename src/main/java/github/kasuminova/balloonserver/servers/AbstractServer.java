package github.kasuminova.balloonserver.servers;

import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.gui.ruleeditor.RuleEditorActionListener;
import github.kasuminova.balloonserver.httpserver.HttpServerInterface;
import github.kasuminova.balloonserver.servers.localserver.AddUpdateRule;
import github.kasuminova.balloonserver.servers.localserver.DeleteUpdateRule;
import github.kasuminova.balloonserver.utils.GUILogger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static github.kasuminova.balloonserver.BalloonServer.MAIN_FRAME;
import static github.kasuminova.balloonserver.utils.SvgIcons.*;

/**
 * 抽象服务端面板示例
 */
public abstract class AbstractServer {
    protected static final int CONTROL_PANEL_WIDTH = 350;
    protected final long start = System.currentTimeMillis();
    protected final JPanel controlPanel = new JPanel(new BorderLayout());
    protected final List<String> commonModeList = new ArrayList<>(0);
    protected final List<String> onceModeList = new ArrayList<>(0);
    //服务器启动状态
    protected final AtomicBoolean isStarted = new AtomicBoolean(false);
    protected final AtomicBoolean isStarting = new AtomicBoolean(false);
    //服务端是否在生成缓存，防止同一时间多个线程生成缓存导致程序混乱
    protected final AtomicBoolean isGenerating = new AtomicBoolean(false);
    //IP 输入框
    protected final JTextField IPTextField = new JTextField("0.0.0.0");
    //端口输入框
    protected final JSpinner portSpinner = new JSpinner();
    //Jks 证书密码
    protected final JPasswordField JksSslPassField = new JPasswordField();
    //资源文件夹输入框
    protected final JTextField mainDirTextField = new JTextField("/res");
    //实时文件监听
    protected final JCheckBox fileChangeListener = new JCheckBox("启用实时文件监听", true);
    //旧版兼容模式
    protected final JCheckBox compatibleMode = new JCheckBox("启用旧版兼容");
    //证书文件名（不可编辑）
    protected final JTextField JksSslTextField = new JTextField("请选择证书文件");
    //普通模式
    protected final JList<String> commonMode = new JList<>();
    //补全模式
    protected final JList<String> onceMode = new JList<>();
    protected final SmoothProgressBar statusProgressBar = new SmoothProgressBar(1000, 250);
    protected final GUILogger logger;
    protected final String serverName;
    protected final JTextPane logPane = new JTextPane();
    protected final JPanel logPanel = new JPanel(new BorderLayout());
    protected HttpServerInterface httpServerInterface;

    protected AbstractServer(String serverName) {
        this.serverName = serverName;

        //设置 Logger，主体为 logPanel
        logger = new GUILogger(serverName, logPane);
        loadLogPanel();
    }

    /**
     * 载入 Log 日志窗口面板
     */
    protected void loadLogPanel() {
        logPanel.setMinimumSize(new Dimension((int) (MAIN_FRAME.getWidth() * 0.5), 0));

        logPanel.setBorder(new TitledBorder("服务端实例日志"));
        logPane.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logPane);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        //日志窗口菜单
        JPopupMenu logPaneMenu = new JPopupMenu();
        JMenuItem cleanLogPane = new JMenuItem("清空日志", DELETE_ICON);
        cleanLogPane.addActionListener(e -> {
            try {
                logPane.getDocument().remove(0, logPane.getDocument().getLength());
                logger.info("已清空当前服务端实例日志窗口.");
            } catch (BadLocationException ignored) {}
        });
        logPaneMenu.add(cleanLogPane);
        logPane.addMouseListener(new LogPaneMouseAdapter(logPaneMenu, logPane));
    }

    protected Box loadIPPortBox() {
        //IP 配置
        Box IPPortBox = Box.createHorizontalBox();
        IPPortBox.add(new JLabel("监听 IP:"));
        IPPortBox.add(IPTextField);
        //端口配置
        SpinnerNumberModel portSpinnerModel = new SpinnerNumberModel(8080, 1, 65535, 1);
        portSpinner.setModel(portSpinnerModel);
        JSpinner.NumberEditor portSpinnerEditor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(portSpinnerEditor);
        IPPortBox.add(new JLabel(" 端口:"));
        IPPortBox.add(portSpinner);

        return IPPortBox;
    }

    protected Box loadMainDirBox() {
        //资源文件夹
        Box mainDirBox = Box.createHorizontalBox();
        JLabel mainDirLabel = new JLabel("资源文件夹:");
        mainDirTextField.putClientProperty("JTextField.showClearButton", true);
        mainDirTextField.setToolTipText("""
                仅支持程序当前目录下的文件夹或子文件夹，请勿输入其他文件夹。
                默认为 /res , 也可输入其他文件夹, 如 /resources、/content、/.minecraft 等.""");
        mainDirBox.add(mainDirLabel);
        mainDirBox.add(mainDirTextField);

        return mainDirBox;
    }

    protected abstract Box loadJksSslBox();

    protected Box loadJksSslPassBox() {
        Box JksSslPassBox = Box.createHorizontalBox();
        JksSslPassBox.add(new JLabel("JKS 证书密码:"));
        JksSslPassBox.add(JksSslPassField);
        return JksSslPassBox;
    }

    protected Component loadCommonModePanel() {
        //普通更新模式
        JPanel commonModePanel = new JPanel();
        commonModePanel.add(commonMode);

        JScrollPane commonModeScrollPane = new JScrollPane(
                commonModePanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        commonModeScrollPane.setBorder(new TitledBorder("普通更新模式"));
        commonModeScrollPane.setPreferredSize(new Dimension(0,200));

        //菜单
        JPopupMenu commonModeMenu = new JPopupMenu();
        JMenuItem openCommonModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        openCommonModeRuleEditor.setIcon(EDIT_ICON);
        //普通更新规则编辑器
        openCommonModeRuleEditor.addActionListener(new RuleEditorActionListener(commonMode, commonModeList, getServerInterface(), logger));
        commonModeMenu.add(openCommonModeRuleEditor);
        commonModeMenu.addSeparator();
        //添加更新规则
        JMenuItem addNewCommonRule = new JMenuItem("添加更新规则");
        addNewCommonRule.setIcon(PLUS_ICON);
        addNewCommonRule.addActionListener(new AddUpdateRule(commonMode, commonModeList, MAIN_FRAME));
        commonModeMenu.add(addNewCommonRule);
        //删除指定规则
        JMenuItem deleteCommonRule = new JMenuItem("删除选定的规则");
        deleteCommonRule.setIcon(REMOVE_ICON);
        deleteCommonRule.addActionListener(new DeleteUpdateRule(commonMode, commonModeList, MAIN_FRAME));
        commonModeMenu.add(deleteCommonRule);
        //鼠标监听
        RuleListMenuMouseAdapter commonRuleListMenuListener = new RuleListMenuMouseAdapter(commonModeMenu, commonMode);
        commonMode.addMouseListener(commonRuleListMenuListener);
        commonModePanel.addMouseListener(commonRuleListMenuListener);

        return commonModeScrollPane;
    }

    protected Component loadOnceModePanel() {
        //补全更新模式
        JPanel onceModePanel = new JPanel();
        onceModePanel.add(onceMode);

        JScrollPane onceModeScrollPane = new JScrollPane(
                onceModePanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        onceModeScrollPane.setBorder(new TitledBorder("补全更新模式"));
        onceModeScrollPane.setPreferredSize(new Dimension(0,200));

        //菜单
        JPopupMenu onceModeMenu = new JPopupMenu();
        JMenuItem openOnceModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        openOnceModeRuleEditor.setIcon(EDIT_ICON);
        //补全更新规则编辑器
        openOnceModeRuleEditor.addActionListener(new RuleEditorActionListener(onceMode, onceModeList, getServerInterface(), logger));
        onceModeMenu.add(openOnceModeRuleEditor);
        onceModeMenu.addSeparator();
        //添加更新规则
        JMenuItem addNewOnceRule = new JMenuItem("添加更新规则");
        addNewOnceRule.setIcon(PLUS_ICON);
        addNewOnceRule.addActionListener(new AddUpdateRule(onceMode, onceModeList, MAIN_FRAME));
        onceModeMenu.add(addNewOnceRule);
        //删除指定规则
        JMenuItem deleteOnceRule = new JMenuItem("删除选定的规则");
        deleteOnceRule.setIcon(REMOVE_ICON);
        deleteOnceRule.addActionListener(new DeleteUpdateRule(onceMode, onceModeList, MAIN_FRAME));
        onceModeMenu.add(deleteOnceRule);
        //鼠标监听
        RuleListMenuMouseAdapter onceRuleListMenuListener = new RuleListMenuMouseAdapter(onceModeMenu, onceMode);
        onceMode.addMouseListener(onceRuleListMenuListener);
        onceModePanel.addMouseListener(onceRuleListMenuListener);

        return onceModeScrollPane;
    }

    protected JPanel loadExtraFeaturesPanel() {
        JPanel extraFeaturesPanel = new JPanel(new BorderLayout());
        //实时文件监听
        fileChangeListener.setToolTipText("""
                开启后，启动服务器的同时会启动文件监听服务.
                文件监听服务会每隔 5 - 7 秒会监听资源文件夹的变化，如果资源一有变化会立即重新生成资源缓存.
                注意:不推荐在超大文件夹(10000 文件/文件夹 以上)上使用此功能，可能会造成 I/O 卡顿.""");

        compatibleMode.setToolTipText("""
                开启后，服务端将兼容 4.x.x 版本的所有类型客户端.
                但是同时也会造成一定的性能下降.""");

        extraFeaturesPanel.add(fileChangeListener, BorderLayout.WEST);
        extraFeaturesPanel.add(compatibleMode, BorderLayout.EAST);

        return extraFeaturesPanel;
    }

    /**
     * 重置主窗口状态栏进度条
     */
    protected void resetStatusProgressBar() {
        statusProgressBar.setVisible(false);
        statusProgressBar.setIndeterminate(false);
        statusProgressBar.reset();
    }

    /**
     * 返回当前服务器实例的 HTTP 服务器接口
     *
     * @return HttpServerInterface
     */
    public HttpServerInterface getHttpServerInterface() {
        return httpServerInterface;
    }

    protected abstract JPanel getPanel();

    protected abstract ServerInterface getServerInterface();

    protected abstract Component loadStatusBar();

    protected abstract Component loadControlPanel();

    protected abstract void loadConfigurationFromFile();

    protected abstract void updateGUIConfig(IntegratedServerConfig config);

    protected abstract void reloadConfigurationFromGUI();

    protected abstract void saveConfigurationToFile();

    private static class RuleListMenuMouseAdapter extends MouseAdapter {
        private final JPopupMenu commonModeMenu;
        private final JList<String> modeList;

        private RuleListMenuMouseAdapter(JPopupMenu modeMenu, JList<String> modeList) {
            this.commonModeMenu = modeMenu;
            this.modeList = modeList;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                commonModeMenu.show(modeList, e.getX(), e.getY());
            }
        }
    }
}
