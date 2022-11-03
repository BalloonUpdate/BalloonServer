package github.kasuminova.balloonserver.utils;

import java.io.IOException;

public class BatchUtils {
    /**
     * 执行一系列 bat 指令
     *
     * @param commands 将被分成每行一个指令
     */
    public static void runBatch(String[] commands) throws IOException {
        if (commands == null) {
            throw new NullPointerException("Commands is Null!");
        }
        if (commands.length == 0) {
            throw new IllegalArgumentException("Empty Commands!");
        }

        StringBuilder sb = new StringBuilder(32);
        for (String s : commands) {
            sb.append(s).append("\n");
        }

        String fileName = "batchTmp.bat";

        FileUtil.createFile(sb.toString(), "./", fileName);

        Runtime.getRuntime().exec(new String[]{String.format(".\\%s", fileName)});
    }
}
