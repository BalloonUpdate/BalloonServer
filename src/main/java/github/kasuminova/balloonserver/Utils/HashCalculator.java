package github.kasuminova.balloonserver.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.zip.CRC32;

/**
 * 公用 Hash 计算类
 */
public class HashCalculator {
    /**
     * 获取文件 SHA1
     * BalloonUpdate 的默认方法
     * @param file 目标文件
     * @return String
     **/
    public static String getSHA1(File file) {
        //计算 MD5
        try {
            FileInputStream in = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] buffer = FileUtil.formatFileSizeByte(file.length());

            int len;
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                md.update(buffer, 0, len);
            }

            in.close();

            //转换并返回包含 20 个元素字节数组,返回数值范围为 -128 到 127
            byte[] sha1Bytes = md.digest();
            //1 代表绝对值
            BigInteger bigInt = new BigInteger(1, sha1Bytes);
            //转换为 16 进制
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * 获取文件 CRC32
     * 相比 MD5, CRC32更快, 但是可靠性要稍微低一些
     * @param file 目标文件
     * @return String
     **/
    public static String getCRC32(File file) {
        try {
            CRC32 crc32 = new CRC32();
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = FileUtil.formatFileSizeByte(file.length());

            int length;
            while ((length = in.read(buffer)) != -1) {
                crc32.update(buffer, 0, length);
            }

            in.close();
            return String.valueOf(crc32.getValue());
        } catch (Exception e){
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * 获取文件 MD5
     * @param file 目标文件
     * @return String
     **/
    public static String getMD5(File file) {
        //计算 MD5
        try {
            FileInputStream in = new FileInputStream(file);
            //拿到一个 MD5 转换器，此外还有 SHA-1, SHA-256
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = FileUtil.formatFileSizeByte(file.length());

            int length;
            while ((length = in.read(buffer, 0, 1024)) != -1) {
                md.update(buffer, 0, length);
            }

            in.close();
            //转换并返回包含 16 个元素字节数组,返回数值范围为 -128 到 127
            byte[] md5Bytes = md.digest();
            //1 代表绝对值
            BigInteger bigInt = new BigInteger(1, md5Bytes);
            //转换为 16 进制
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}