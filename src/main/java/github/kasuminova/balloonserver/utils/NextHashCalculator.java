package github.kasuminova.balloonserver.utils;

import cn.hutool.core.util.HexUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

/**
 * 公用 Hash 计算类
 */
public class NextHashCalculator {
    public static final String SHA1 = "sha1";
    public static final String CRC32 = "crc32";

    /**
     * 获取文件 SHA1
     * BalloonUpdate 的默认方法
     *
     * @param file 目标文件
     * @return SHA1 值
     **/
    public static String getSHA1(File file) throws IOException, NoSuchAlgorithmException {
        return getSHA1(file, null);
    }

    /**
     * 获取文件 CRC32
     *
     * @param file 目标文件
     * @return CRC32 值
     */
    public static String getCRC32(File file) throws IOException {
        return getCRC32(file, null);
    }

    /**
     * 获取文件 SHA1
     * BalloonUpdate 的默认方法
     *
     * @param file     目标文件
     * @param progress 进度变量
     * @return SHA1 值
     **/
    public static String getSHA1(File file, AtomicLong progress) throws IOException, NoSuchAlgorithmException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        byte[] data = new byte[FileUtil.formatFileSizeInt(randomAccessFile.length())];
        int len;
        MessageDigest md = MessageDigest.getInstance("SHA1");
        while ((len = randomAccessFile.read(data)) > 0) {
            md.update(data, 0, len);
            if (progress != null) progress.getAndAdd(len);
        }
        randomAccessFile.close();

        //转换为 16 进制
        return HexUtil.encodeHexStr(md.digest());
    }

    /**
     * 获取文件 CRC32
     *
     * @param file     目标文件
     * @param progress 进度变量
     * @return CRC32 值
     */
    public static String getCRC32(File file, AtomicLong progress) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        byte[] data = new byte[FileUtil.formatFileSizeInt(randomAccessFile.length())];
        int len;
        CRC32 crc32 = new CRC32();
        while ((len = randomAccessFile.read(data)) > 0) {
            crc32.update(data, 0, len);
            if (progress != null) progress.getAndAdd(len);
        }
        randomAccessFile.close();

        //转换为 16 进制
        return HexUtil.toHex(crc32.getValue());
    }
}