package github.kasuminova.balloonserver.Utils.FileObject;

public class SimpleFileObject extends AbstractSimpleFileObject {
    public SimpleFileObject(String name, long length, String hash, long modified) {
        this.name = name;
        this.length = length;
        this.hash = hash;
        this.modified = modified;
    }

    long length;
    String hash;
    long modified;

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }
}
