package github.kasuminova.balloonserver.updatechecker;

import java.io.Serial;
import java.io.Serializable;

public class ApplicationVersion implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public static final String BETA = "BETA";
    public static final String STABLE = "STABLE";
    public static final int BIG_VERSION_WEIGHTS = 100000;
    public static final int SUB_VERSION_WEIGHTS = 1000;
    private final int bigVersion;
    private final int subVersion;
    private final int minorVersion;
    private final String branch;

    /**
     * 程序版本对象
     *
     * @param version 如 1.1.1-BETA, 1.0.0-STABLE
     */
    public ApplicationVersion(String version) {
        String[] versionTmp = version.split("-", 2);
        String[] versions = versionTmp[0].split("\\.", 3);

        bigVersion = Integer.parseInt(versions[0]);
        subVersion = Integer.parseInt(versions[1]);
        minorVersion = Integer.parseInt(versions[2]);
        branch = versionTmp[1];
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getSubVersion() {
        return subVersion;
    }

    public String getBranch() {
        return branch;
    }

    public int getBigVersion() {
        return bigVersion;
    }

    public String toString() {
        return String.format("%s.%s.%s-%s", bigVersion, subVersion, minorVersion, branch);
    }

    public int toInt() {
        return (bigVersion * BIG_VERSION_WEIGHTS) + (subVersion * SUB_VERSION_WEIGHTS) + minorVersion;
    }
}
