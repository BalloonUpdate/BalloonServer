package github.kasuminova.balloonserver.servers;

import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.gui.ruleeditor.RuleEditorActionListener;
import github.kasuminova.balloonserver.httpserver.HttpServerInterface;
import github.kasuminova.balloonserver.servers.localserver.AddUpdateRule;
import github.kasuminova.balloonserver.servers.localserver.DeleteUpdateRule;

import javax.swing.*;
import javax.swing.border.TitledBorder;
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
    protected final JPanel controlPanel = new JPanel(new BorderLayout());
    protected final List<String> commonModeList = new ArrayList<>();
    protected final List<String> onceModeList = new ArrayList<>();
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
    protected HttpServerInterface httpServerInterface;

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

    protected JPanel loadCommonModePanel() {
        //普通更新模式
        JPanel commonModePanel = new JPanel(new VFlowLayout());
        commonModePanel.setBorder(new TitledBorder("普通更新模式"));
        commonMode.setVisibleRowCount(8);
        JScrollPane common_ModeScrollPane = new JScrollPane(
                commonMode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        commonModePanel.add(common_ModeScrollPane);

        //菜单
        JPopupMenu commonModeMenu = new JPopupMenu();
        JMenuItem openCommonModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        openCommonModeRuleEditor.setIcon(EDIT_ICON);
        //普通更新规则编辑器
        openCommonModeRuleEditor.addActionListener(new RuleEditorActionListener(commonMode, commonModeList, getServerInterface()));
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
        commonMode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    commonModeMenu.show(commonMode, e.getX(), e.getY());
                }
            }
        });

        return commonModePanel;
    }

    protected JPanel loadOnceModePanel() {
        //补全更新模式
        JPanel onceModePanel = new JPanel(new VFlowLayout());
        onceModePanel.setBorder(new TitledBorder("补全更新模式"));
        onceMode.setVisibleRowCount(8);
        JScrollPane once_ModeScrollPane = new JScrollPane(
                onceMode,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        onceModePanel.add(once_ModeScrollPane);

        //菜单
        JPopupMenu onceModeMenu = new JPopupMenu();
        JMenuItem openOnceModeRuleEditor = new JMenuItem("打开更新规则编辑器");
        openOnceModeRuleEditor.setIcon(EDIT_ICON);
        //补全更新规则编辑器
        openOnceModeRuleEditor.addActionListener(new RuleEditorActionListener(commonMode, commonModeList, getServerInterface()));
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
        //鼠标监听
        onceMode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    onceModeMenu.show(onceMode, e.getX(), e.getY());
                }
            }
        });
        onceModeMenu.add(deleteOnceRule);

        return onceModePanel;
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

    protected abstract ServerInterface getServerInterface();

    protected abstract Component loadStatusBar();

    protected abstract Component loadControlPanel();
}
