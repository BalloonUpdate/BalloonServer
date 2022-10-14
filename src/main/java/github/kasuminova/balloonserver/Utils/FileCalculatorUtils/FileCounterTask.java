package github.kasuminova.balloonserver.Utils.FileCalculatorUtils;

import github.kasuminova.balloonserver.Utils.FileObject.SimpleFileObject;
import github.kasuminova.balloonserver.Utils.HashCalculator;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static github.kasuminova.balloonserver.Utils.HashCalculator.getCRC32;
import static github.kasuminova.balloonserver.Utils.HashCalculator.getSHA1;

record FileCounterTask(File file, String hashAlgorithm, AtomicLong completedBytes, AtomicInteger completedFiles)
        implements Callable<SimpleFileObject> {
    @Override
    public SimpleFileObject call() throws IOException, NoSuchAlgorithmException {
        String hash;
        if (hashAlgorithm.equals(HashCalculator.SHA1)) {
            hash = getSHA1(file, completedBytes);
        } else {
            hash = getCRC32(file, completedBytes);
        }
        completedFiles.getAndIncrement();
        return new SimpleFileObject(
                file.getName(),
                file.length(),
                hash,
                file.lastModified() / 1000);
    }
}
