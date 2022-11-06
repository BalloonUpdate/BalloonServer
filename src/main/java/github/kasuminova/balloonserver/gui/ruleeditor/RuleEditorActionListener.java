package github.kasuminova.balloonserver.gui.ruleeditor;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.file.FileReader;
import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.servers.ServerInterface;
import github.kasuminova.balloonserver.servers.localserver.IntegratedServerInterface;
import github.kasuminova.balloonserver.servers.remoteserver.RemoteClientInterface;
import github.kasuminova.balloonserver.utils.GUILogger;
import github.kasuminova.balloonserver.utils.HashCalculator;
import github.kasuminova.balloonserver.utils.filecacheutils.JsonCacheUtils;
import github.kasuminova.messages.RequestMessage;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_FILE_THREAD_POOL;
import static github.kasuminova.balloonserver.BalloonServer.MAIN_FRAME;

public class RuleEditorActionListener implements ActionListener {
    protected final JList<String> ruleList;
    protected final List<String> rules;
    protected final ServerInterface serverInterface;
    protected final GUILogger logger;

    public RuleEditorActionListener(JList<String> ruleList, List<String> rules, ServerInterface serverInterface, GUILogger logger) {
        this.ruleList = ruleList;
        this.rules = rules;
        this.serverInterface = serverInterface;
        this.logger = logger;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (serverInterface.isGenerating().get()) {
            JOptionPane.showMessageDialog(MAIN_FRAME,
                    "当前正在生成资源缓存，请稍后再试。",
                    BalloonServer.TITLE,
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (serverInterface instanceof IntegratedServerInterface) {
            File file = new File(String.format("./%s.%s.json", serverInterface.getServerName(), serverInterface.getResJsonFileExtensionName()));
            if (file.exists()) {
                int selection = JOptionPane.showConfirmDialog(MAIN_FRAME,
                        "检测到本地 JSON 缓存，是否以 JSON 缓存启动规则编辑器？",
                        BalloonServer.TITLE, JOptionPane.YES_NO_OPTION);
                if (!(selection == JOptionPane.YES_OPTION)) return;

                try {
                    String json = new FileReader(file).readString();
                    showRuleEditorDialog(JSONArray.parseArray(json), ruleList, rules, logger);
                } catch (IORuntimeException ex) {
                    logger.error("无法读取本地 JSON 缓存\n", ex);
                }
                return;
            }

            int selection = JOptionPane.showConfirmDialog(MAIN_FRAME,
                    "未检测到 JSON 缓存，是否立即生成 JSON 缓存并启动规则编辑器？",
                    BalloonServer.TITLE, JOptionPane.YES_NO_OPTION);
            if (!(selection == JOptionPane.YES_OPTION)) return;

            GLOBAL_FILE_THREAD_POOL.execute(() -> {
                new JsonCacheUtils((IntegratedServerInterface) serverInterface, null, null).updateDirCache(null, HashCalculator.CRC32);
                if (file.exists()) {
                    try {
                        String json = new FileReader(file).readString();
                        showRuleEditorDialog(JSONArray.parseArray(json), ruleList, rules, logger);
                    } catch (IORuntimeException ex) {
                        logger.error("无法读取本地 JSON 缓存\n", ex);
                    }
                }
            });
        } else if (serverInterface instanceof RemoteClientInterface remoteClientInterface) {
            remoteClientInterface.getChannel().writeAndFlush(new RequestMessage(
                    "GetJsonCache", List.of(new String[]{"RuleEditor"})));
        }
    }

    public static void showRuleEditorDialog(JSONArray jsonArray, JList<String> ruleList, List<String> rules, GUILogger logger) {
        GLOBAL_FILE_THREAD_POOL.execute(() -> {
            //锁定窗口，防止用户误操作
            MAIN_FRAME.setEnabled(false);
            RuleEditor editorDialog = new RuleEditor(jsonArray, rules, logger);
            editorDialog.setModal(true);

            MAIN_FRAME.setEnabled(true);
            editorDialog.setVisible(true);

            ruleList.setListData(rules.toArray(new String[0]));
        });
    }
}
