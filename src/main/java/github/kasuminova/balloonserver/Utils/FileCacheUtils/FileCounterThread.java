package github.kasuminova.balloonserver.Utils.FileCacheUtils;

import github.kasuminova.balloonserver.Utils.FileObject.SimpleFileObject;
import github.kasuminova.balloonserver.Utils.HashCalculator;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import static github.kasuminova.balloonserver.Utils.HashCalculator.getCRC32;
import static github.kasuminova.balloonserver.Utils.HashCalculator.getSHA1;

/**
 * 计算单个文件的 SHA1/CRC32 的 类/线程.
 * 传入 File 文件
 * 创建一个线程, 计算文件信息
 * 计算完毕后, 返回 SimpleFileObject
 */
record FileCounterThread(File file, String hashAlgorithm) implements Callable<SimpleFileObject> {
    @Override
    public SimpleFileObject call() throws IOException, NoSuchAlgorithmException {
        String hash;
        if (hashAlgorithm.equals(HashCalculator.SHA1)) {
            hash = getSHA1(file);
        } else {
            hash = getCRC32(file);
        }
        return new SimpleFileObject(file.getName(), file.length(), hash, file.lastModified() / 1000);
    }
}
