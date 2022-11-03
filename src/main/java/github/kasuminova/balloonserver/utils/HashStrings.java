package github.kasuminova.balloonserver.utils;

public class HashStrings {
    private final String crc32;
    private final String sha1;

    public HashStrings(String CRC32, String SHA1) {
        this.crc32 = CRC32;
        this.sha1 = SHA1;
    }

    public HashStrings(String CRC32) {
        this.crc32 = CRC32;
        this.sha1 = null;
    }

    public String getCrc32() {
        return crc32;
    }

    public String getSha1() {
        return sha1;
    }
}
