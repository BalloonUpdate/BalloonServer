package github.kasuminova.messages.filemessages;

public class FileRequestMsg extends FileMessage {
    private long offset;
    private long length;

    public FileRequestMsg(String filePath, String fileName, long offset, long length) {
        super(filePath, fileName);
        this.offset = offset;
        this.length = length;
    }

    public FileRequestMsg(String filePath, String fileName) {
        super(filePath, fileName);
        offset = 0;
        length = -1;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}
