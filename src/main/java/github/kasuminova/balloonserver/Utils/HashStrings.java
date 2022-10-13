package github.kasuminova.balloonserver.Utils;

public class HashStrings {
    private final String CRC32;
    private final String SHA1;
    public HashStrings(String CRC32, String SHA1) {
        this.CRC32 = CRC32;
        this.SHA1 = SHA1;
    }

    public HashStrings(String CRC32) {
        this.CRC32 = CRC32;
        this.SHA1 = null;
    }

    public String getCRC32() {
        return CRC32;
    }

    public String getSHA1() {
        return SHA1;
    }
}
