package github.kasuminova.balloonserver.updatechecker;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import github.kasuminova.balloonserver.BalloonServer;
import github.kasuminova.balloonserver.gui.SmoothProgressBar;
import github.kasuminova.balloonserver.gui.layoutmanager.VFlowLayout;
import github.kasuminova.balloonserver.utils.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

public class HttpClient {
    /**
     * 从指定 URL 中获取 String 字符串
     *
     * @param urlStr 链接
     * @return 返回的字符串
     */
    public static String getStringWithURL(String urlStr) {
        String result = HttpUtil.get(urlStr);

        return Objects.requireNonNullElse(result, "ERROR");
    }

    /**
     * <p>
     * 从指定链接下载文件并保存到指定文件中，并弹出一个对话框显示进度。
     * </p>
     *
     * <p>
     * 高度封装。
     * </p>
     *
     * @param url 链接
     * @param file 保存文件
     * @throws HttpException 如果无法连接到服务器或状态码错误
     * @throws ExecutionException 如果线程异常或被中断
     * @throws InterruptedException 如果线程被用户中断
     */
    public static void downloadFileWithURL(String url, File file) throws HttpException, ExecutionException, InterruptedException {
        FutureTask<Boolean> downloadTask = new FutureTask<>(() -> {
            if (file.exists()) {
                if (!file.delete()) {
                    throw new IOException("Could Not Delete File " + file.getPath());
                }
            }
            if (!file.createNewFile()) {
                throw new IOException("Could Not Create File " + file.getPath());
            }

            JFrame downloadingFrame = new JFrame(BalloonServer.TITLE);
            downloadingFrame.setIconImage(BalloonServer.ICON.getImage());
            downloadingFrame.setSize(new Dimension(450, 145));
            downloadingFrame.setResizable(false);

            JPanel mainPanel = new JPanel(new VFlowLayout(0, 10, 5, true, false));
            mainPanel.add(new JLabel("进度: "));

            SmoothProgressBar progressBar = new SmoothProgressBar(500, 500);
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
            progressBar.setString("连接中...");

            mainPanel.add(progressBar);
            mainPanel.add(new JLabel("在此期间你可以继续使用程序。"));
            JButton cancel = new JButton("取消下载");
            mainPanel.add(cancel);

            //[0] 为已完成进度, [1] 为速度缓存进度
            AtomicLong completedBytes = new AtomicLong(0);
            AtomicLong cachedCompletedBytes = new AtomicLong(0);
            AtomicLong fileSize = new AtomicLong(0);
            //定时更新查询器
            Timer timer = new Timer(500, e -> {
                progressBar.setString(String.format("进度: %s / %s - 速度: %s/s",
                        FileUtil.formatFileSizeToStr(completedBytes.get()),
                        FileUtil.formatFileSizeToStr(fileSize.get()),
                        FileUtil.formatFileSizeToStr((completedBytes.get() - cachedCompletedBytes.get()) * 2)
                ));
                progressBar.setValue((int) (completedBytes.get() * progressBar.getMaximum() / fileSize.get()));

                cachedCompletedBytes.set(completedBytes.get());
            });

            downloadingFrame.add(mainPanel);
            downloadingFrame.setLocationRelativeTo(null);
            downloadingFrame.setVisible(true);

            InputStream fis = null;
            FileChannel foc = null;
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            int responseCode = urlConnection.getResponseCode();
            if (!(responseCode >= 200 && responseCode < 300)) {
                //关闭窗口
                downloadingFrame.dispose();

                throw new HttpException("无法连接至下载服务器或连接异常！(Code: {})", responseCode);
            }

            try {
                fis = urlConnection.getInputStream();
                foc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);

                ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
                fileSize.set(urlConnection.getContentLength());
                progressBar.setIndeterminate(false);
                timer.start();
                Thread currentThread = Thread.currentThread();
                InputStream finalFis = fis;
                FileChannel finalFoc = foc;
                cancel.addActionListener(e -> {
                    timer.stop();
                    currentThread.interrupt();
                    try {
                        finalFis.close();
                        finalFoc.close();
                    } catch (IOException ignored) {}
                });

                downloadingFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        timer.stop();
                        currentThread.interrupt();
                        try {
                            finalFis.close();
                            finalFoc.close();
                        } catch (IOException ignored) {}
                    }
                });

                int count;
                while ((count = fis.read(byteBuffer.array(), 0, 4096)) != -1) {
                    foc.write(byteBuffer, completedBytes.get());
                    completedBytes.getAndAdd(count);
                    byteBuffer.clear();
                }
            } finally {
                if (fis != null) {
                    fis.close();
                }
                if (foc != null) {
                    foc.close();
                }
                //关闭定时器和窗口
                timer.stop();
                downloadingFrame.dispose();
            }

            return true;
        });

        new Thread(downloadTask).start();
        downloadTask.get();
    }
}
