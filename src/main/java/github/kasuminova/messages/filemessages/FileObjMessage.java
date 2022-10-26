package github.kasuminova.messages.filemessages;

public class FileObjMessage extends FileMessage {
    private long offset;
    private long length;
    private long total;
    private byte[] data;

    public FileObjMessage(String filePath, String fileName, long offset, long length, long total, byte[] data) {
        super(filePath, fileName);
        this.offset = offset;
        this.length = length;
        this.total = total;
        this.data = data;
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

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
