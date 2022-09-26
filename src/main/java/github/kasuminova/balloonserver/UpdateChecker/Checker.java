package github.kasuminova.balloonserver.UpdateChecker;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.BalloonServer;

public class Checker {
    public static void main(String[] args) {
        String apiURL = "https://gitee.com/api/v5/repos/hikari_nova/BalloonServer/releases";
        String giteeAPIJson = HttpClient.getStringWithURL(apiURL);
        if (giteeAPIJson.startsWith("ERROR:")) {
            return;
        }
        JSONArray jsonArray = JSONArray.parseArray(giteeAPIJson);
        JSONObject newRelease = jsonArray.getJSONObject(0);
        ApplicationVersion newVersion = new ApplicationVersion(newRelease.getString("tag_name"));

        ApplicationVersion applicationVersion = BalloonServer.VERSION;
        //大版本更新检查
        if (applicationVersion.getBigVersion() < newVersion.getBigVersion()) {
            //业务代码...
            System.out.println("Has a BigVersion Update");

               //子版本更新检查
        } else if (applicationVersion.getSubVersion() < newVersion.getSubVersion()) {
            System.out.println("Has a SubVersion Update");
            //业务代码...

               //小版本更新检查
        } else if (applicationVersion.getMinorVersion() < newVersion.getMinorVersion()) {
            System.out.println("Has a MinorVersion Update");
            //业务代码...
        } else {
            System.out.println("No Update");
        }

        System.out.println(BalloonServer.VERSION);
        System.out.println(newVersion);
    }
}
