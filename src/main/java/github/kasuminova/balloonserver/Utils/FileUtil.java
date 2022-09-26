package github.kasuminova.balloonserver.Utils;

import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtil {
    /**
     * 生成.json格式文件
     */
    public static void createJsonFile(String jsonString, String filePath, String fileName) throws IOException {
        // 拼接文件完整路径
        String fullPath = filePath + File.separator + fileName + ".json";

        // 生成json格式文件
        // 保证创建一个新文件
        File file = new File(fullPath);
        if (!file.getParentFile().exists()) {
            if (file.getParentFile().mkdirs()) {
                throw new IOException("父目录创建失败！");
            }
        }
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("旧文件删除失败！");
            }
        }
        if (!file.createNewFile()) {
            throw new IOException("创建文件失败！！");
        }


        if(jsonString.contains("'")){
            //将单引号转义一下，因为JSON串中的字符串类型可以单引号引起来的
            jsonString = jsonString.replaceAll("'", "\\'");
        }
        if(jsonString.contains("\"")){
            //将双引号转义一下，因为JSON串中的字符串类型可以单引号引起来的
            jsonString = jsonString.replaceAll("\"", "\\\"");
        }
        if(jsonString.contains("\r\n")){
            //将回车换行转换一下，因为JSON串中字符串不能出现显式的回车换行
            jsonString = jsonString.replaceAll("\r\n", "\\u000d\\u000a");
        }
        if(jsonString.contains("\n")){
            //将换行转换一下，因为JSON串中字符串不能出现显式的换行
            jsonString = jsonString.replaceAll("\n", "\\u000a");
        }

        // 将格式化后的字符串写入文件
        Writer write = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
        write.write(jsonString);
        write.flush();
        write.close();
    }

    /**
     * 根据传入大小返回合适的 byte[]
     * 适用于 FileInputStream 之类的环境
     * @param size 文件大小
     * @return 根据大小适应的 byte[] 大小
     */
    public static byte[] formatFileSizeByte(long size) {
        if (size <= 1024) {
            return new byte[1024];
        } else if (size <= 1024 * 1024) {
            return new byte[1024 * 64];
        } else if (size <= 1024 * 1024 * 128) {
            return new byte[1024 * 256];
        } else if (size <= 1024 * 1024 * 512) {
            return new byte[1024 * 1024];
        } else {
            return new byte[1024 * 1024 * 8];
        }
    }

    public static String readStringFromFile(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        fileReader.close();
        reader.close();

        return sb.toString();
    }

    /**
     * 根据传入大小返回合适的 int 大小
     * 适用于网络文件上传
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
        } else if (size <= 1024 * 1024 * 512){
            return 1024 * 1024;
        } else {
            return 1024 * 1024 * 8;
        }
    }

    /**
     * 根据传入大小返回合适的大小格式化文本
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
         * @param ext 扩展名数组，如 png,jpg
         * @param des 扩展描述
         */
        public SimpleFileFilter(String[] ext, String[] blackList,String des) {
            this.ext = ext;
            this.blackList = blackList;
            this.des = des;
        }

        public boolean accept(File file) {
            if (!file.isDirectory()) {
                String fileName = file.getName();
                //黑名单检查
                if (blackList != null) {
                    for (String s : blackList) {
                        if (fileName.contains(s)) return false;
                    }
                }
                int index = fileName.lastIndexOf('.');
                if (index > 0 && index < fileName.length() - 1) {
                    // 表示文件名称不为".xxx"现"xxx."之类型
                    String extension = fileName.substring(index + 1).toLowerCase();
                    for (String s : ext) {
                        if (extension.equals(s)) return true;
                    }
                }
            } else {
                //如果是文件夹，则显示文件夹
                return true;
            }
            return false;
        }
        public String getDescription() {
            return des;
        }
    }
}
