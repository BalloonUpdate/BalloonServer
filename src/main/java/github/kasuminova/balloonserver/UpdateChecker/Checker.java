package github.kasuminova.balloonserver.UpdateChecker;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.Utils.GUILogger;

import javax.swing.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static github.kasuminova.balloonserver.BalloonServer.*;
import static github.kasuminova.balloonserver.UpdateChecker.HttpClient.downloadFileWithURL;

public class Checker {
    /**
     * 从 Gitee 仓库获取最新 Release 信息
     */
    public static void checkUpdates() {
        String apiURL = "https://gitee.com/api/v5/repos/hikari_nova/BalloonServer/releases";
        String giteeAPIJson = HttpClient.getStringWithURL(apiURL);
        if (giteeAPIJson.startsWith("ERROR:")) {
            return;
        }
        JSONArray jsonArray = JSONArray.parseArray(giteeAPIJson);

        //遍历从 API 获取到的 Releases, 并寻找最新版本
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject latestRelease = jsonArray.getJSONObject(i);
            ApplicationVersion newVersion = new ApplicationVersion(latestRelease.getString("tag_name"));

            ApplicationVersion applicationVersion = BalloonServer.VERSION;

            //如果版本分支不一样则忽略此版本
            if (!applicationVersion.getBranch().equals(newVersion.getBranch())) continue;

            //如果当前版本高于仓库最新版本则忽略此版本
            if (applicationVersion.getBigVersion() > newVersion.getBigVersion() ||
                applicationVersion.getSubVersion() > newVersion.getSubVersion() ||
                applicationVersion.getMinorVersion() > newVersion.getMinorVersion())
            {
                continue;
            }

            //版本更新检查
            if (applicationVersion.getBigVersion() < newVersion.getBigVersion() ||
                applicationVersion.getSubVersion() < newVersion.getSubVersion() ||
                applicationVersion.getMinorVersion() < newVersion.getMinorVersion())
            {
                if (CONFIG.isAutoUpdate() && ARCHIVE_NAME.contains("e4j") && ARCHIVE_NAME.contains("Temp"))
                {
                    String fileName = downloadUpdate(latestRelease, false);
                    if (fileName != null) startProgram(fileName, true);
                    return;
                }
                int operation = JOptionPane.showConfirmDialog(MAIN_FRAME,
                        String.format("检测到有版本更新 (%s)，您要下载更新吗？", newVersion),
                        "更新提示",
                        JOptionPane.YES_NO_OPTION);

                if (operation != JOptionPane.YES_NO_OPTION) return;

                downloadUpdate(latestRelease, true);
            }
        }
    }

    /**
     * 启动指定名称的程序
     * @param fileName 程序名
     */
    public static void startProgram(String fileName, boolean exitThisProgram) {
        try {
            Runtime.getRuntime().exec(new String[]{String.format(".\\%s", fileName)});
            if (exitThisProgram) {
                //如果主服务端正在运行，则打开自动启动服务器（仅一次）选项并保存，下次启动服务端时自动启动服务器
                if (availableCustomServerInterfaces.get(0).isStarted().get()) {
                    CONFIG.setAutoStartServerOnce(true);
                }
                if (availableCustomServerInterfaces.get(1).isStarted().get()) {
                    CONFIG.setAutoStartLegacyServerOnce(true);
                }
                BalloonServer.saveConfig();
                //停止所有正在运行的服务器并保存配置
                BalloonServer.stopAllServers(false);
                System.exit(0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(MAIN_FRAME,
                    String.format("无法启动服务端。\n%s", GUILogger.stackTraceToString(e)), BalloonServer.TITLE,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 下载最新的程序版本，返回下载完成后的文件名
     * @param latestRelease Release API 的 JSON 信息
     * @param showCompleteDialog 完成下载后是否弹出完成对话框
     * @return 文件名, 如果无匹配服务端或下载失败返回 null
     */
    private static String downloadUpdate(JSONObject latestRelease, boolean showCompleteDialog) {
        JSONArray assets = latestRelease.getJSONArray("assets");
        assets.remove(assets.size() - 1);

        if (ARCHIVE_NAME.contains("e4j")) {
            for (int i = 0; i < assets.size(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String fileName = asset.getString("name");
                if (fileName.endsWith(".exe")) {
                    try {
                        //下载文件
                        downloadFileWithURL(
                                new URL(asset.getString("browser_download_url")),
                                new File(String.format("./%s", fileName)));
                        if (showCompleteDialog) {
                            JOptionPane.showMessageDialog(MAIN_FRAME,
                                    String.format("程序下载完成！已保存至当前程序路径(%s)。", fileName),
                                    BalloonServer.TITLE,
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                        return fileName;
                    } catch (IOException e) {
                        GLOBAL_LOGGER.error("下载更新失败.", e);
                    }
                }
            }
            return null;
        }

        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String fileName = asset.getString("name");
            if (fileName.endsWith(".jar")) {
                try {
                    //下载文件
                    downloadFileWithURL(
                            new URL(asset.getString("browser_download_url")),
                            new File(String.format("./%s", fileName)));
                    if (showCompleteDialog) {
                        JOptionPane.showMessageDialog(MAIN_FRAME,
                                String.format("程序下载完成！已保存至当前程序路径(%s)。", fileName),
                                BalloonServer.TITLE,
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    return fileName;
                } catch (IOException e) {
                    GLOBAL_LOGGER.error("下载更新失败，可能是因为用户取消了操作或无法连接至服务器.", e);
                }
            }
        }
        return null;
    }
}
