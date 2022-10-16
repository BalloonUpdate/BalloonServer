package github.kasuminova.messages.fileobject;

public final class LiteFileObject extends AbstractLiteFileObject {
    long modified;
    long length;
    public LiteFileObject(String name, long length, long modified) {
        this.name = name;
        this.length = length;
        this.modified = modified;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }
}
