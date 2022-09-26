package github.kasuminova.balloonserver.UpdateChecker;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

public class HttpClient {
    public static void main(String[] args) {
        String url = "https://gitee.com/hikari_nova/BalloonServer/releases/download/1.1.3-BETA/BalloonServer-GUI-1.1.3-BETA.jar";

        try {
            downloadFileWithURL(new URL(url), new File("./BalloonServer-GUI-1.1.3-BETA.jar"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从指定 URL 中获取 String 字符串
     * @param urlStr 链接
     * @return 返回的字符串
     */
    public static String getStringWithURL(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String SUBMIT_METHOD_GET = "GET";  // 一定要是大写，否则请求无效

            HttpURLConnection connection;
            InputStream is = null;
            BufferedReader br = null;
            String result = null;  // 返回结果字符串
            // 通过远程 url 连接对象打开一个连接，强转成 httpURLConnection类
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接方式：GET
            connection.setRequestMethod(SUBMIT_METHOD_GET);
            // 设置连接主机服务器的超时时间：15000毫秒
            connection.setConnectTimeout(5000);
            // 设置读取远程返回的数据时间：60000毫秒
            connection.setReadTimeout(10000);
            // 发送请求
            connection.connect();
            // 通过connection连接，请求成功后获取输入流
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                // 封装输入流is，并指定字符集
                br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                // 存放数据
                StringBuilder sb = new StringBuilder();
                String temp;
                while ((temp = br.readLine()) != null) {
                    sb.append(temp);
                }
                result = sb.toString();
            }
            // 释放资源
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            connection.disconnect();  // 关闭远程连接
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

    private static void downloadFileWithURL(URL url, File file) throws IOException{
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Could Not Create File " + file.getPath());
        }
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileChannel foc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

        long total = 0;
        int count;
        while((count = bis.read(byteBuffer.array(),0,4096)) != -1)
        {
            foc.write(byteBuffer, total);
            total += count;
            byteBuffer.flip();
            byteBuffer.clear();
        }

        bis.close();
        foc.close();
    }
}
