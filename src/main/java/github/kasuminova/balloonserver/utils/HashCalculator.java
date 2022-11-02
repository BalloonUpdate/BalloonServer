package github.kasuminova.balloonserver.utils;

import cn.hutool.core.util.HexUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

/**
 * 公用 Hash 计算类
 */
public class HashCalculator {
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
        FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
        int len;
        MessageDigest md = MessageDigest.getInstance("SHA1");
        while ((len = fc.read(byteBuffer)) > 0) {
            md.update(byteBuffer.array(), 0, len);
            byteBuffer.clear();
        }
        fc.close();

        //转换为 16 进制
        return HexUtil.encodeHexStr(md.digest());
    }

    /**
     * 获取文件 CRC32
     *
     * @param file 目标文件
     * @return CRC32 值
     */
    public static String getCRC32(File file) throws IOException {
        FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
        int len;
        CRC32 crc32 = new CRC32();
        while ((len = fc.read(byteBuffer)) > 0) {
            crc32.update(byteBuffer.array(), 0, len);
            byteBuffer.clear();
        }
        fc.close();

        //转换为 16 进制
        return HexUtil.toHex(crc32.getValue());
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
        FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
        int len;
        MessageDigest md = MessageDigest.getInstance("SHA1");
        while ((len = fc.read(byteBuffer)) > 0) {
            md.update(byteBuffer.array(), 0, len);
            byteBuffer.clear();
            progress.getAndAdd(len);
        }
        fc.close();

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
        FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
        int len;
        CRC32 crc32 = new CRC32();
        while ((len = fc.read(byteBuffer)) > 0) {
            crc32.update(byteBuffer.array(), 0, len);
            byteBuffer.clear();
            progress.getAndAdd(len);
        }
        fc.close();

        //转换为 16 进制
        return HexUtil.toHex(crc32.getValue());
    }

    /**
     * 获取文件 CRC32 和 SHA1
     *
     * @param file     目标文件
     * @param progress 进度变量
     * @return CRC32 和 SHA1 值
     */
    public static HashStrings getCRC32AndSHA1(File file, AtomicLong progress) throws IOException, NoSuchAlgorithmException {
        FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
        int len;

        CRC32 crc32 = new CRC32();
        MessageDigest md = MessageDigest.getInstance("SHA1");

        while ((len = fc.read(byteBuffer)) > 0) {
            crc32.update(byteBuffer.array(), 0, len);
            md.update(byteBuffer.array(), 0, len);
            byteBuffer.clear();
            progress.getAndAdd(len);
        }
        fc.close();

        //转为 16 进制
        String CRC32String = HexUtil.toHex(crc32.getValue());
        String SHA1String = HexUtil.encodeHexStr(md.digest());

        return new HashStrings(CRC32String, SHA1String);
    }

    /**
     * 获取文件 CRC32 和 SHA1
     *
     * @param file 目标文件
     * @return CRC32 和 SHA1 值
     */
    public static HashStrings getCRC32AndSHA1(File file) throws IOException, NoSuchAlgorithmException {
        FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(FileUtil.formatFileSizeInt(file.length()));
        int len;

        CRC32 crc32 = new CRC32();
        MessageDigest md = MessageDigest.getInstance("SHA1");

        while ((len = fc.read(byteBuffer)) > 0) {
            crc32.update(byteBuffer.array(), 0, len);
            md.update(byteBuffer.array(), 0, len);
            byteBuffer.clear();
        }
        fc.close();

        //转为 16 进制
        String CRC32String = HexUtil.toHex(crc32.getValue());
        String SHA1String = HexUtil.encodeHexStr(md.digest());

        return new HashStrings(CRC32String, SHA1String);
    }
}