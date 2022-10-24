package github.kasuminova.balloonserver.utils.fileobject;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.messages.AbstractMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public abstract class AbstractSimpleFileObject implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 将 JSONObject 转为 AbstractSimpleFileObject
     *
     * @param obj 要转换的 JSONObject
     * @return SimpleFileObject 或 SimpleDirectoryObject
     */
    public static AbstractSimpleFileObject jsonObjectToFileObject(JSONObject obj) {
        if (fileObjIsDirectoryObj(obj)) {
            return new SimpleDirectoryObject(obj.getString("name"), jsonArrToFileObjArr(obj.getJSONArray("children")));
        } else {
            return new SimpleFileObject(
                    obj.getString("name"),
                    obj.getLong("length"),
                    obj.getString("hash"),
                    obj.getLong("modified"));
        }
    }

    /**
     * 将 JSONArray 转为 ArrayList<AbstractSimpleFileObject>
     *
     * @param arr 要转换的 JSONArray
     * @return SimpleFileObject 或 SimpleDirectoryObject
     */
    public static ArrayList<AbstractSimpleFileObject> jsonArrToFileObjArr(JSONArray arr) {
        ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();
        arr.toList(JSONObject.class).forEach(jsonObject -> {
            if (fileObjIsDirectoryObj(jsonObject)) {
                fileObjList.add(jsonObjectToFileObject(jsonObject));
            } else {
                fileObjList.add(new SimpleFileObject(
                        jsonObject.getString("name"),
                        jsonObject.getLong("length"),
                        jsonObject.getString("hash"),
                        jsonObject.getLong("modified")));
            }
        });

        return fileObjList;
    }

    /**
     * 此文件对象是否为文件夹
     *
     * @param obj 要检查的 JSON 对象
     */
    private static boolean fileObjIsDirectoryObj(JSONObject obj) {
        return obj.getLong("length") == null || obj.getLong("modified") == null || obj.getString("hash") == null;
    }
}
