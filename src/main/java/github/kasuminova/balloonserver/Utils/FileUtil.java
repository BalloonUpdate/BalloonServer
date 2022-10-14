package github.kasuminova.balloonserver.Utils;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.file.FileWriter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class FileUtil {
    /**
     * 生成.json格式文件
     */
    public static void createJsonFile(String jsonString, String filePath, String fileName) throws IORuntimeException {
        //拼接文件完整路径
        String fullPath = filePath + File.separator + fileName + ".json";
        File file = new File(fullPath);
        //保证创建一个新文件
        cn.hutool.core.io.FileUtil.touch(file);
        //写入文件
        FileWriter writer = new FileWriter(file);
        writer.write(jsonString);
    }

    /**
     * 根据传入大小返回合适的 int 大小
     *
     * @param size 文件大小
     * @return 根据大小适应的 int 大小
     */
    public static int formatFileSizeInt(long size) {
        if (size <= 1024) {
            return 1024;
        } else if (size <= 1024 * 1024) {
            return 1024 * 8;
        } else if (size <= 1024 * 1024 * 128) {
            return 1024 * 64;
        } else if (size <= 1024 * 1024 * 512) {
            return 1024 * 1024;
        } else {
            return 1024 * 1024 * 8;
        }
    }

    /**
     * 根据传入大小返回合适的格式化文本
     *
     * @param size 文件大小
     * @return Byte 或 KB 或 MB 或 GB
     */
    public static String formatFileSizeToStr(long size) {
        if (size <= 1024) {
            return size + " Byte";
        } else if (size <= 1024 * 1024) {
            return String.format("%.2f", (double) size / 1024) + " KB";
        } else if (size <= 1024 * 1024 * 1024) {
            return String.format("%.2f", (double) size / (1024 * 1024)) + " MB";
        } else {
            return String.format("%.2f", (double) size / (1024 * 1024 * 1024)) + " GB";
        }
    }

    public static class SimpleFileFilter extends FileFilter {
        private final String[] ext;
        private final String[] blackList;
        private final String des;

        /**
         * 一个简易的多文件扩展名过滤器
         * 文件扩展名过滤器
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
