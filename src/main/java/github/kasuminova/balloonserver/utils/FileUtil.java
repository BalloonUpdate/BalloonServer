package github.kasuminova.balloonserver.utils;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import com.alibaba.fastjson2.JSONArray;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class FileUtil {
    public static final int KB = 1024;
    public static final int MB = 1024 * 1024;
    public static final int GB = 1024 * 1024 * 1024;

    /**
     * 生成.json格式文件
     */
    public static void createJsonFile(String jsonString, String filePath, String fileName) throws IORuntimeException {
        createFile(jsonString, filePath, fileName + ".json");
    }

    /**
     * 生成文件
     * @param str 内容
     * @param filePath 路径
     * @param fileName 文件名
     */
    public static void createFile(String str, String filePath, String fileName) throws IORuntimeException {
        //拼接文件完整路径
        File target = new File(filePath + fileName);
        //保证创建一个新文件
        cn.hutool.core.io.FileUtil.touch(target);

        //写入文件
        FileWriter writer = new FileWriter(target);
        writer.write(str);
    }

    /**
     * 根据传入大小返回合适的 int 大小
     *
     * @param size 文件大小
     * @return 根据大小适应的 int 大小
     */
    public static int formatFileSizeInt(long size) {
        if (size <= KB) {
            return (int) size;
        } else if (size <= MB) {
            return KB * 8;
        } else if (size <= MB * 128) {
            return KB * 64;
        } else if (size <= MB * 512) {
            return MB;
        } else {
            return MB * 8;
        }
    }

    /**
     * 根据传入大小返回合适的 int 大小
     *
     * @param size 文件大小
     * @return 根据大小适应的 int 大小
     */
    public static int formatFileSizeSmallInt(long size) {
        if (size <= KB) {
            return (int) size;
        } else if (size <= MB) {
            return KB * 8;
        } else if (size <= MB * 128) {
            return KB * 16;
        } else {
            return KB * 32;
        }
    }

    /**
     * 根据传入大小返回合适的格式化文本
     *
     * @param size 文件大小
     * @return Byte 或 KB 或 MB 或 GB
     */
    public static String formatFileSizeToStr(long size) {
        if (size <= KB) {
            return size + " Byte";
        } else if (size <= MB) {
            return String.format("%.2f", (double) size / KB) + " KB";
        } else if (size <= GB) {
            return String.format("%.2f", (double) size / MB) + " MB";
        } else {
            return String.format("%.2f", (double) size / GB) + " GB";
        }
    }

    /**
     * 从指定名称 JSON 文件中读取 JSONArray
     *
     * @param fileName                 文件名
     * @param resJsonFileExtensionName 自定义扩展名
     * @param logger                   Logger
     * @return JSONArray, 如果文件不存在或出现问题, 则返回 null
     */
    public static JSONArray loadJsonArrayFromFile(String fileName, String resJsonFileExtensionName, GUILogger logger) {
        File jsonCache = new File(String.format("./%s.%s.json", fileName, resJsonFileExtensionName));

        JSONArray jsonArray = null;

        if (jsonCache.exists()) {
            try {
                String jsonString = new FileReader(jsonCache).readString();
                jsonArray = JSONArray.parseArray(jsonString);
            } catch (IORuntimeException e) {
                if (logger != null) {
                    logger.error("读取缓存文件的时候出现了一些问题...", e);
                    logger.warn("缓存文件读取失败, 重新生成缓存...");
                }
            }
        }

        return jsonArray;
    }

    public static class SimpleFileFilter extends FileFilter {
        private final String[] ext;
        private final String[] blackList;
        private final String des;

        /**
         * 一个简易的多文件扩展名过滤器
         *
         * @param ext       扩展名数组，如 png,jpg
         * @param blackList 黑名单
         * @param des       扩展描述
         */
        public SimpleFileFilter(String[] ext, String[] blackList, String des) {
            this.ext = ext;
            this.blackList = blackList;
            this.des = des;
        }

        public boolean accept(File file) {
            //如果是文件夹则显示文件夹
            if (file.isDirectory()) return true;
            String fileName = file.getName();
            //黑名单检查
            if (blackList != null) {
                for (String s : blackList) {
                    if (fileName.contains(s)) return false;
                }
            }
            for (String extension : ext) {
                if (file.getName().endsWith(String.format(".%s", extension))) return true;
            }
            return false;
        }

        public String getDescription() {
            return des;
        }
    }
}
