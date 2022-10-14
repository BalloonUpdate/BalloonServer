package github.kasuminova.balloonserver.Utils.FileObject;

import github.kasuminova.balloonserver.Utils.HashCalculator;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static github.kasuminova.balloonserver.Utils.HashCalculator.getCRC32;
import static github.kasuminova.balloonserver.Utils.HashCalculator.getSHA1;

public record FileInfoTask(File file, String hashAlgorithm, AtomicLong completedBytes, AtomicInteger completedFiles)
        implements Callable<SimpleFileObject> {
    @Override
    public SimpleFileObject call() throws IOException, NoSuchAlgorithmException {
        String hash;
        if (hashAlgorithm.equals(HashCalculator.SHA1)) {
            hash = completedBytes == null ? getSHA1(file) : getSHA1(file, completedBytes);
        } else {
            hash = completedBytes == null ? getCRC32(file) : getCRC32(file, completedBytes);
        }
        if (completedFiles != null) completedFiles.getAndIncrement();
        return new SimpleFileObject(
                file.getName(),
                file.length(),
                hash,
                file.lastModified() / 1000);
    }
}
